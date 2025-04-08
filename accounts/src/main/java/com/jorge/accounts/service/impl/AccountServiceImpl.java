package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.service.AccountService;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.model.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final CustomerClient customerClient;
    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;
    private final TransactionClient transactionClient;

    @Override
    public Flux<AccountResponse> getAllAccounts() {
        log.info("Fetching all accounts");
        return accountRepository.findAll()
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Mono<AccountResponse> getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Mono<Void> deleteAccountByAccountNumber(String accountNumber) {
        log.info("Deleting account with account number: {}", accountNumber);
        return accountRepository.deleteByAccountNumber(accountNumber);
    }

    @Override
    public Mono<BalanceResponse> getBalanceByAccountNumber(String accountNumber) {
        log.info("Fetching balance for account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .map(account -> {
                    BalanceResponse balanceResponse = new BalanceResponse();
                    balanceResponse.setAccountNumber(account.getAccountNumber());
                    balanceResponse.setBalance(account.getBalance());
                    balanceResponse.setAccountType(BalanceResponse.AccountTypeEnum.valueOf(account.getAccountType().name()));
                    return balanceResponse;
                });
    }

    @Override
    public Mono<BalanceResponse> increaseBalanceByAccountNumber(String accountNumber, BigDecimal balance) {
        log.info("Increasing balance by {} for account number: {}", balance, accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .flatMap(account -> {
                    account.setBalance(account.getBalance().add(balance));
                    return accountRepository.save(account);
                })
                .map(this::mapToBalanceResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")));
    }

    @Override
    public Mono<BalanceResponse> decreaseBalanceByAccountNumber(String accountNumber, BigDecimal balance) {
        log.info("Decreasing balance by {} for account number: {}", balance, accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .flatMap(account -> {
                    BigDecimal amount = account.getBalance().subtract(balance);
                    if(account.getBalance().compareTo(amount) < 0) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insufficient balance for the decrease account"));
                    account.setBalance(amount);
                    return accountRepository.save(account);
                })
                .map(this::mapToBalanceResponse).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")));
    }

    @Override
    public Mono<AccountResponse> depositByAccountNumber(String accountNumber, DepositRequest depositRequest) {
        log.info("Depositing {} to account number: {}", depositRequest.getAmount(), accountNumber);
        // Process Movements limit and Update fee status if applicable
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(this::processAccountMovement)  // Check movement limits and fee
                .flatMap(account -> {
                    BigDecimal depositAmount = depositRequest.getAmount();

                    // If there is a balance, it means there was a deposit already. This means no more deposits are allowed
                    if(account.getAccountType() == Account.AccountType.FIXED_TERM && account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "One deposit has already been performed for this Fixed Term account"));
                    }

                    // Apply commission fee if active
                    if (account.getIsCommissionFeeActive()) {
                        depositAmount = depositAmount.subtract(account.getMovementCommissionFee());
                        if(depositAmount.compareTo(BigDecimal.ZERO) < 0){
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Commission fee is higher than deposit amount"));
                        }
                    }
                    // Update balance
                    account.setBalance(account.getBalance().add(depositAmount));

                    return accountRepository.save(account)
                            .flatMap(savedAccount -> {
                                TransactionRequest transactionRequest = new TransactionRequest();
                                transactionRequest.setAccountNumber(accountNumber);
                                transactionRequest.setAmount(depositRequest.getAmount());
                                transactionRequest.setTransactionType(TransactionRequest.TransactionType.DEPOSIT);
                                transactionRequest.setDescription("Deposit to account " + accountNumber);
                                transactionRequest.setFee(savedAccount.getIsCommissionFeeActive()
                                        ? savedAccount.getMovementCommissionFee() : BigDecimal.ZERO);

                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedAccount);
                            });
                })
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Mono<AccountResponse> withdrawByAccountNumber(String accountNumber, WithdrawalRequest withdrawalRequest) {
        log.info("Withdrawing {} from account number: {}", withdrawalRequest.getAmount(), accountNumber);
        // Process Movements limit and Update fee status if applicable
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(this::processAccountMovement) // Check movement limits and fee
                .flatMap(account -> {
                    BigDecimal withdrawalAmount = withdrawalRequest.getAmount();

                    // Apply commission fee if active
                    if (account.getIsCommissionFeeActive()) {
                        withdrawalAmount = withdrawalAmount.add(account.getMovementCommissionFee()); // Add to withdrawal, since it's taken from balance
                    }

                    // Check if balance is less than the withdrawal
                    if(account.getBalance().compareTo(withdrawalAmount) < 0){
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Insufficient balance for the withdrawal"));
                    }

                    // If the withdrawal is made on a FIXED_TERM account
                    if (account.getAccountType() == Account.AccountType.FIXED_TERM) {
                        FixedTermAccount fixedTermAccount = (FixedTermAccount) account;
                        if (fixedTermAccount.getAllowedWithdrawal().isAfter(LocalDate.now())) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Withdrawal not allowed until " + fixedTermAccount.getAllowedWithdrawal()));
                        }
                    }
                    // Update balance
                    account.setBalance(account.getBalance().subtract(withdrawalAmount));

                    return accountRepository.save(account)
                            .flatMap(savedAccount -> {
                                TransactionRequest transactionRequest = new TransactionRequest();
                                transactionRequest.setAccountNumber(accountNumber);
                                transactionRequest.setAmount(withdrawalRequest.getAmount());
                                transactionRequest.setTransactionType(TransactionRequest.TransactionType.WITHDRAWAL);
                                transactionRequest.setDescription("Withdrawal from account " + accountNumber);
                                if(savedAccount.getIsCommissionFeeActive()) transactionRequest.setFee(savedAccount.getMovementCommissionFee());
                                else transactionRequest.setFee(BigDecimal.ZERO);

                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedAccount);
                            });
                })
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        log.info("Fetching transactions for account number {}", accountNumber);
        return transactionClient.getTransactionsByAccountNumber(accountNumber);
    }

    @Override
    public Mono<AverageMonthlyDailyBalanceResponse> calculateAverageMonthlyDailyBalance(String accountNumber, AverageMonthlyDailyBalanceRequest averageMonthlyDailyBalanceRequest) {
        YearMonth yearMonth = YearMonth.of(averageMonthlyDailyBalanceRequest.getYear(), averageMonthlyDailyBalanceRequest.getMonth());
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
        LocalDateTime endOfMonth = lastDayOfMonth.atTime(23, 59, 59);

        // Obtener las transacciones del mes y calcular el saldo promedio
        return transactionClient.getTransactionsByAccountNumberAndDateRange(accountNumber, startOfMonth, endOfMonth)
                .collectList()
                .flatMap(transactions -> calculateAverageBalance(accountNumber, transactions, firstDayOfMonth, lastDayOfMonth))
                .map(averageBalance -> { // Construir el objeto de respuesta
                    AverageMonthlyDailyBalanceResponse response = new AverageMonthlyDailyBalanceResponse();
                    response.setAccountNumber(accountNumber);
                    response.setYear(yearMonth.getYear());
                    response.setMonth(yearMonth.getMonthValue());
                    response.setAverageDailyBalance(averageBalance);
                    return response;
                });
    }


    private Mono<BigDecimal> calculateAverageBalance(String accountNumber, List<TransactionResponse> transactions, LocalDate firstDayOfMonth, LocalDate lastDayOfMonth) {
        return getInitialBalance(accountNumber, firstDayOfMonth)
                .flatMap(saldoInicial -> {
                    BigDecimal saldoDiarioTotal = BigDecimal.ZERO;
                    LocalDate fechaActual = firstDayOfMonth;
                    BigDecimal saldoActual = saldoInicial;

                    while (!fechaActual.isAfter(lastDayOfMonth)) {
                        BigDecimal saldoFinalDelDia = saldoActual;
                        for (TransactionResponse transaction : transactions) {
                            LocalDateTime transactionCreatedAt = transaction.getCreatedAt();
                            if (transactionCreatedAt != null && transactionCreatedAt.toLocalDate().isEqual(fechaActual)) {
                                saldoFinalDelDia = calculateBalanceAfterTransaction(saldoFinalDelDia, transaction); // To know if we sum or subtract
                            }
                        }
                        saldoDiarioTotal = saldoDiarioTotal.add(saldoFinalDelDia);
                        saldoActual = saldoFinalDelDia;
                        fechaActual = fechaActual.plusDays(1);
                    }

                    int numeroDeDiasEnElMes = lastDayOfMonth.getDayOfMonth();
                    return Mono.just(saldoDiarioTotal.divide(BigDecimal.valueOf(numeroDeDiasEnElMes), 2, RoundingMode.HALF_UP));
                });
    }

    private BigDecimal calculateBalanceAfterTransaction(BigDecimal saldoActual, TransactionResponse transaction) {
        assert transaction.getTransactionType() != null;

        return switch (transaction.getTransactionType()) {
            case DEBIT, WITHDRAWAL, MAINTENANCE_FEE, CREDIT_PAYMENT, CREDIT_CARD_PAYMENT -> {
                assert transaction.getAmount() != null;
                yield saldoActual.subtract(transaction.getAmount().add(transaction.getFee()));
            }
            case CREDIT, DEPOSIT, CREDIT_DEPOSIT -> {
                assert transaction.getAmount() != null;
                yield saldoActual.add(transaction.getAmount().subtract(transaction.getFee()));
            }
        };
    }

    // Obtener el saldo inicial de la cuenta al inicio del mes.  Si no hay historial, devuelve 0.
    private Mono<BigDecimal> getInitialBalance(String accountNumber, LocalDate firstDayOfMonth) {
        LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
        return transactionClient.getTransactionsByAccountNumberAndDateRange(accountNumber,
                        LocalDateTime.of(1970, 1, 1, 0, 0, 0), startOfMonth)
                .collectList()
                .map(transactions -> {
                    BigDecimal saldoInicial = BigDecimal.ZERO;
                    for (TransactionResponse transaction : transactions) {
                        saldoInicial = calculateBalanceAfterTransaction(saldoInicial, transaction);
                    }
                    return saldoInicial;
                });
    }

    @Override
    public Mono<TransactionResponse> transfer(String accountNumber, TransferRequest transferRequest){
        String receiverAccountNumber = transferRequest.getReceiverAccountNumber();
        BigDecimal transferAmount = transferRequest.getAmount();

        // Fetch and Validate Sender ---
        Mono<Account> validatedSender = accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found: " + accountNumber)))
                .flatMap(account -> {
                    // Sender Validations
                    if (account instanceof SavingsAccount savingsAccount) {
                        if (savingsAccount.getMovementsThisMonth() >= savingsAccount.getMonthlyMovementsLimit()) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Savings account " + accountNumber + " has reached max movements limit ("
                                            + savingsAccount.getMonthlyMovementsLimit() + ") this month."));
                        }
                    } else if (account instanceof FixedTermAccount) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Fixed Term accounts cannot initiate transfers."));
                    }
                    // Basic validation passed
                    return Mono.just(account);
                });
        Mono<Account> validatedReceiver = accountRepository.findByAccountNumber(receiverAccountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver account not found: " + receiverAccountNumber)))
                .flatMap(account -> {
                    // Receiver Type Validation
                    if (account.getAccountType() == Account.AccountType.FIXED_TERM) { // Assuming getAccountType() exists
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cannot transfer to a Fixed Term account."));
                    }
                    return Mono.just(account);
                });
        return Mono.zip(validatedSender, validatedReceiver)
                .flatMap(accountsTuple -> {
                    Account senderAccount = accountsTuple.getT1();
                    Account receiverAccount = accountsTuple.getT2();

                    // Calculate potential fee
                    BigDecimal fee;
                    // Check if Commission Fee is applicable
                    if (senderAccount.getIsCommissionFeeActive()) {
                        fee = senderAccount.getMovementCommissionFee();
                    } else {
                        fee = BigDecimal.ZERO;
                    }
                    BigDecimal totalDeduction = transferAmount.add(fee);

                    if (senderAccount.getBalance().compareTo(totalDeduction) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Insufficient funds in sender account " + accountNumber + " (Required: " + totalDeduction + ")"));
                    }

                    //  Update Accounts Balances
                    senderAccount.setBalance(senderAccount.getBalance().subtract(totalDeduction));
                    receiverAccount.setBalance(receiverAccount.getBalance().add(transferAmount));

                    // Update movements for Savings Account
                    if (senderAccount instanceof SavingsAccount savingsAccount) {
                        savingsAccount.setMovementsThisMonth(savingsAccount.getMovementsThisMonth() + 1);
                    }

                    return accountRepository.save(senderAccount)
                            .flatMap(savedSender -> accountRepository.save(receiverAccount)
                                            .map(savedReceiver -> Tuples.of(savedSender, fee))
                            );
                })
                .flatMap(savedSenderAndFeeTuple -> {
                    BigDecimal fee = savedSenderAndFeeTuple.getT2(); // Retrieve the fee calculated earlier

                    // Create Sender Transaction
                    TransactionRequest debitRequest = new TransactionRequest();
                    debitRequest.setAccountNumber(accountNumber);
                    debitRequest.setAmount(transferAmount); // Amount being transferred
                    debitRequest.setTransactionType(TransactionRequest.TransactionType.DEBIT);
                    debitRequest.setDescription("Transfer to account " + receiverAccountNumber);
                    debitRequest.setFee(fee); // Include the calculated fee

                    // Create Receiver Transaction
                    TransactionRequest creditRequest = new TransactionRequest();
                    creditRequest.setAccountNumber(receiverAccountNumber);
                    creditRequest.setAmount(transferAmount);
                    creditRequest.setTransactionType(TransactionRequest.TransactionType.CREDIT);
                    creditRequest.setDescription("Transfer from account " + accountNumber);
                    creditRequest.setFee(BigDecimal.ZERO); // Receiver does not get a fee

                    // --- Call Transaction Client for Both ---
                    Mono<TransactionResponse> debitTransactionMono = transactionClient.createTransaction(debitRequest);
                    Mono<TransactionResponse> creditTransactionMono = transactionClient.createTransaction(creditRequest);

                    // Returning the response from the DEBIT transaction
                    return Mono.zip(debitTransactionMono, creditTransactionMono)
                            .map(Tuple2::getT1);
                });
    }

    /*@Override
    public Mono<TransactionResponse> transfer(String accountNumber, TransferRequest transferRequest) {
        String receiverAccountNumber = transferRequest.getReceiverAccountNumber();
        BigDecimal transferAmount = transferRequest.getAmount();
        return accountRepository.findByAccountNumber(accountNumber)
                .flatMap(account -> {
                    if(account instanceof SavingsAccount savingsAccount) {
                        if(savingsAccount.getMovementsThisMonth() > savingsAccount.getMonthlyMovementsLimit()) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Savings account with number: " + accountNumber + " has reached max movements this month"));
                        }
                    } else if (account instanceof FixedTermAccount fixedAccount) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Fixed Term accounts can't make transfers"));
                    }
                    return Mono.just(account);
                })
                .flatMap(account -> accountRepository.findByAccountNumber(receiverAccountNumber).flatMap(recieverAccount -> {
                    if(recieverAccount.getAccountType() == Account.AccountType.FIXED_TERM){
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Can't transfer to a Fixed Term account"));
                    }
                    return Mono.just(account);
                }))
                .map()
    }*/

    @Override
    public Flux<FeeReportResponse> generateFeeReportBetweenDate(String accountNumber,
                                                                LocalDateTime startDate,
                                                                LocalDateTime endDate) {
        return transactionClient.getTransactionsFeesByAccountNumberAndDateRange(accountNumber, startDate, endDate);
    }

    public BalanceResponse mapToBalanceResponse(Account account){
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setAccountNumber(account.getAccountNumber());
        balanceResponse.setAccountType(BalanceResponse.AccountTypeEnum.valueOf(account.getAccountType().name()));
        balanceResponse.setBalance(account.getBalance());
        return balanceResponse;
    }

    private Mono<Account> processAccountMovement(Account account) {
        log.info("Processing account movement for account number: {}", account.getAccountNumber());
        // Specific validations based on account type
        if (account.getAccountType() == Account.AccountType.SAVINGS) {
            SavingsAccount savingsAccount = (SavingsAccount) account;
            if (savingsAccount.getMovementsThisMonth() > savingsAccount.getMonthlyMovementsLimit()) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Monthly movement limit reached for savings account"));
            }
        }
        // Increase movement count
        account.setMovementsThisMonth(account.getMovementsThisMonth() + 1);

        // Check if commission fee should be activated
        if (!account.getIsCommissionFeeActive() && account.getMovementsThisMonth().equals(account.getMaxMovementsFeeFreeThisMonth())) {
            account.setIsCommissionFeeActive(true);
        }
        return Mono.just(account);
    }

    /*
    @Override
    public Mono<AccountResponse> getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Mono<AccountResponse> createAccount(AccountRequest accountRequest) {
        log.info("Creating a new account for customer DNI: {}", accountRequest.getCustomerDni());
        Mono<CustomerResponse> customerResponse = customerClient.getCustomerByDni(accountRequest.getCustomerDni());
        return customerResponse.flatMap(customer ->
                switch (customer.getCustomerType()) {
                    case PERSONAL -> personalCustomerValidation(customer, accountRequest);
                    case BUSINESS -> businessCustomerValidation(accountRequest);
                    default -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported customer type"));
                }).map(accountMapper::mapToAccountResponse);
    }

    private Mono<Account> personalCustomerValidation(CustomerResponse customer, AccountRequest accountRequest) {
        log.info("Validating personal customer account creation for DNI: {}, Account Type: {}", customer.getDni(), accountRequest.getAccountType());
        return accountRepository.findByCustomerDniAndAccountType(customer.getDni(),
                        Account.AccountType.valueOf(accountRequest.getAccountType().name()))
                .flatMap(account -> {
                    log.warn("Conflict: Customer with DNI {} already has a {} account", customer.getDni(), accountRequest.getAccountType());
                    return Mono.<Account>error(new ResponseStatusException(HttpStatus.CONFLICT,
                        "Customer with dni: " + customer.getDni() + " already has a " + accountRequest.getAccountType().name() + " account"));
                    })
                .switchIfEmpty(Mono.defer(() -> {
                    Account newAccount = accountCreationSetUp(accountRequest);
                    log.info("Saving new personal account");
                    return accountRepository.save(newAccount);
                }));
    }

    private Mono<Account> businessCustomerValidation(AccountRequest accountRequest) {
        log.info("Validating business customer account creation. Account Type: {}", accountRequest.getAccountType());
        if (accountRequest.getAccountType() != AccountRequest.AccountTypeEnum.CHECKING) {
            log.warn("Business customer attempted to create a non-CHECKING account: {}", accountRequest.getAccountType());
            return Mono.error(
                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Business customer can't create a " + accountRequest.getAccountType().name() + " account"));
        }
        log.info("Saving new business account");
        return accountRepository.save(accountCreationSetUp(accountRequest));
    }

    @Override
    public Mono<Void> deleteAccountByAccountNumber(String accountNumber) {
        log.info("Deleting account with account number: {}", accountNumber);
        return accountRepository.deleteByAccountNumber(accountNumber);
    }

    @Override
    public Mono<AccountResponse> updateAccountByAccountNumber(String accountNumber, AccountRequest accountRequest) {
        log.info("Updating account with account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(existingAccount -> {
                    Account account = accountMapper.mapToAccount(accountRequest);
                    account.setId(existingAccount.getId());
                    account.setAccountNumber(existingAccount.getAccountNumber());
                    account.setCreatedAt(existingAccount.getCreatedAt());
                    log.info("Saving updated account with account number: {}", accountNumber);
                    return accountRepository.save(account);
                    })
                .map(accountMapper::mapToAccountResponse);
    }

    public Account accountCreationSetUp(AccountRequest accountRequest) {
        Account account = accountMapper.mapToAccount(accountRequest);
        account.setAccountNumber(accountUtils.generateAccountNumber());
        account.setCreatedAt(LocalDateTime.now());
        log.info("Setting up new account with generated account number: {}", account.getAccountNumber());
        return account;
    }*/
}
