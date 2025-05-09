package com.jorge.accounts.service.impl;

import com.jorge.accounts.listener.dto.BootCoinPurchaseKafkaMessage;
import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.service.AccountService;
import com.jorge.accounts.service.strategy.business.AccountMovementProcessStrategy;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.request.TransactionRequest;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;
    private final TransactionClient transactionClient;
    private final Map<Account.AccountType, AccountMovementProcessStrategy> movementProcessStrategies;

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
    public Flux<AccountResponse> getAccountsByCustomerId(String customerId) {
        log.info("Fetching accounts for customer id: {}", customerId);
        return accountRepository.findByCustomerId(customerId)
                .map(accountMapper::mapToAccountResponse);
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
                .doOnSuccess(balanceResponse -> log.info("Successfully increased balance for account number: {}", accountNumber))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")));
    }

    @Override
    public Mono<BalanceResponse> decreaseBalanceByAccountNumber(String accountNumber, BigDecimal balance) {
        log.info("Decreasing balance by {} for account number: {}", balance, accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(account -> {
                    BigDecimal amount = account.getBalance().subtract(balance);
                    if(account.getBalance().compareTo(amount) < 0) {
                        log.warn("Insufficient balance for account number: {}", accountNumber);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Insufficient balance for the decrease account"));
                    }
                    account.setBalance(amount);
                    return accountRepository.save(account);
                })
                .map(this::mapToBalanceResponse)
                .doOnSuccess(balanceResponse -> log.info("Successfully decreased balance for account number: {}", accountNumber));
    }

    @Override
    public Mono<AccountResponse> depositByAccountNumber(String accountNumber, DepositRequest depositRequest) {
        log.info("Depositing {} to account number: {}", depositRequest.getAmount(), accountNumber);
        // Process Movements limit and Update fee status if applicable
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(this::processAccountMovement)  // Check movement limits and fee
                .flatMap(this::validateFixedAccountDeposit)
                .flatMap(account -> {
                    BigDecimal depositAmount = depositRequest.getAmount();

                    // Apply commission fee if active
                    if (account.getIsCommissionFeeActive()) {
                        log.info("Applying commission fee of {} for account number: {}", account.getMovementCommissionFee(), accountNumber);
                        depositAmount = depositAmount.subtract(account.getMovementCommissionFee());
                        if (depositAmount.compareTo(BigDecimal.ZERO) < 0) {
                            log.warn("Commission fee is higher than deposit amount for account number: {}", accountNumber);
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Commission fee is higher than deposit amount"));
                        }
                    }
                    // Update balance
                    account.setBalance(account.getBalance().add(depositAmount));
                    // Saving changes
                    return accountRepository.save(account)
                            .flatMap(savedAccount -> {
                                TransactionRequest transactionRequest = createTransactionRequest(savedAccount,
                                        depositRequest.getAmount(),
                                        TransactionRequest.TransactionType.DEPOSIT,
                                        "Deposit to Account " + accountNumber);
                                // Creating Transaction
                                return transactionClient.createTransaction(transactionRequest)
                                        .doOnSuccess(transactionResponse ->
                                                log.info("Deposit Transaction created for account number: {}, transaction ID: {}",
                                                        accountNumber, transactionResponse.getId()))
                                        .doOnError(e ->
                                                log.error("Error creating transaction for " +
                                                        "deposit to account number: {}", accountNumber, e))
                                        .thenReturn(savedAccount);
                            })
                            .doOnSuccess(savedAccount -> log.info("Successfully updated balance for account number: {}", accountNumber));
                })
                .map(accountMapper::mapToAccountResponse)
                .doOnError(e -> log.error("Error depositing to account number: {}", accountNumber, e));

    }

    @Override
    public Mono<AccountResponse> withdrawByAccountNumber(String accountNumber, WithdrawalRequest withdrawalRequest) {
        log.info("Withdrawing {} from account number: {}", withdrawalRequest.getAmount(), accountNumber);
        // Process Movements limit and Update fee status if applicable
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(this::processAccountMovement) // Check movement limits and fee
                .flatMap(this::validateFixedAccountWithdraw)
                .flatMap(account -> {
                    BigDecimal withdrawalAmount = withdrawalRequest.getAmount();

                    // Apply commission fee if active
                    if (account.getIsCommissionFeeActive()) {
                        // Add to withdrawal, since it's taken from balance
                        withdrawalAmount = withdrawalAmount.add(account.getMovementCommissionFee());
                        log.info("Applying commission fee of {} for account number: {}", account.getMovementCommissionFee(), accountNumber);
                    }
                    // Check if balance is less than the withdrawal
                    if(account.getBalance().compareTo(withdrawalAmount) < 0){
                        log.warn("Insufficient balance for withdrawal from account number: {}", accountNumber);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Insufficient balance for withdrawal"));
                    }
                    // Update balance
                    account.setBalance(account.getBalance().subtract(withdrawalAmount));

                    return accountRepository.save(account)
                            .flatMap(savedAccount -> {
                                TransactionRequest transactionRequest = createTransactionRequest(savedAccount,
                                        withdrawalRequest.getAmount(),
                                        TransactionRequest.TransactionType.WITHDRAWAL,
                                        "Withdrawal from Account " + accountNumber);
                                return transactionClient.createTransaction(transactionRequest)
                                        .doOnSuccess(transactionResponse ->
                                                log.info("Withdrawl Transaction created for account number: {}, transaction ID: {}",
                                                        accountNumber, transactionResponse.getId()))
                                        .doOnError(e -> log.error("Error creating transaction for " +
                                                "withdrawal from account number: {}", accountNumber, e))
                                        .thenReturn(savedAccount);
                            })
                            .doOnSuccess(savedAccount -> log.info("Successfully updated balance for account number: {}", accountNumber));
                })
                .map(accountMapper::mapToAccountResponse)
                .doOnError(e -> log.error("Error withdrawing from account number: {}", accountNumber, e));
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        log.info("Fetching transactions for account number {}", accountNumber);
        return transactionClient.getTransactionsByAccountNumber(accountNumber);
    }

    @Override
    public Mono<AverageMonthlyDailyBalanceResponse> calculateAverageMonthlyDailyBalance(String accountNumber,
                                                                                        AverageMonthlyDailyBalanceRequest averageMonthlyDailyBalanceRequest) {
        YearMonth yearMonth = YearMonth.of(averageMonthlyDailyBalanceRequest.getYear(), averageMonthlyDailyBalanceRequest.getMonth());
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
        LocalDateTime endOfMonth = lastDayOfMonth.atTime(23, 59, 59);

        log.info("Calculating average monthly daily balance for account number: {}, year: {}, month: {}",
                accountNumber, yearMonth.getYear(), yearMonth.getMonthValue());
        // Obtener las transacciones del mes y calcular el saldo promedio
        return transactionClient.getTransactionsByAccountNumberAndDateRange(accountNumber, startOfMonth, endOfMonth)
                .collectList()
                .flatMap(transactions -> calculateAverageBalance(accountNumber,
                        transactions, firstDayOfMonth, lastDayOfMonth))
                .map(averageBalance -> { // Construir el objeto de respuesta
                    AverageMonthlyDailyBalanceResponse response = new AverageMonthlyDailyBalanceResponse();
                    response.setAccountNumber(accountNumber);
                    response.setYear(yearMonth.getYear());
                    response.setMonth(yearMonth.getMonthValue());
                    response.setAverageDailyBalance(averageBalance);
                    return response;
                })
                .doOnSuccess(response ->
                        log.info("Successfully calculated average monthly daily balance for account number: {}, average balance: {}", accountNumber, response.getAverageDailyBalance()))
                .doOnError(e ->
                        log.error("Error calculating average monthly daily balance for account number: {}", accountNumber, e));
    }


    private Mono<BigDecimal> calculateAverageBalance(String accountNumber, List<TransactionResponse> transactions,
                                                     LocalDate firstDayOfMonth, LocalDate lastDayOfMonth) {
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

    public Mono<AccountResponse> purchaseBootCoin(BootCoinPurchaseKafkaMessage bootCoinPurchaseKafkaMessage) {
        log.info("Processing BootCoin purchase for wallet ID: {}", bootCoinPurchaseKafkaMessage.getBootCoinWalletId());
        return accountRepository.findByAccountNumber(bootCoinPurchaseKafkaMessage.getPaymentMethodId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + bootCoinPurchaseKafkaMessage.getPaymentMethodId() + " not found")))
                .flatMap(this::processAccountMovement) // Validate account movement
                .flatMap(account -> {
                    BigDecimal paymentAmount = bootCoinPurchaseKafkaMessage.getPaymentAmount();

                    // Check if balance is enough
                    if (account.getBalance().compareTo(paymentAmount) < 0) {
                        log.warn("Insufficient balance for BootCoin purchase for account number: {}", account.getAccountNumber());
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Insufficient balance for BootCoin purchase"));
                    }

                    // Decrease balance
                    account.setBalance(account.getBalance().subtract(paymentAmount));

                    // Save account and create transaction
                    return accountRepository.save(account)
                            .flatMap(savedAccount -> {
                                TransactionRequest transactionRequest = createTransactionRequest(
                                        savedAccount,
                                        paymentAmount,
                                        TransactionRequest.TransactionType.DEBIT,
                                        "BootCoin purchase for wallet ID: " + bootCoinPurchaseKafkaMessage.getBootCoinWalletId()
                                );

                                return transactionClient.createTransaction(transactionRequest)
                                        .doOnSuccess(transactionResponse ->
                                                log.info("BootCoin purchase transaction created for account number: {}, transaction ID: {}",
                                                        savedAccount.getAccountNumber(), transactionResponse.getId()))
                                        .doOnError(e ->
                                                log.error("Error creating transaction for BootCoin purchase for account number: {}", savedAccount.getAccountNumber(), e))
                                        .thenReturn(savedAccount);
                            })
                            .doOnSuccess(savedAccount -> log.info("Successfully processed BootCoin purchase for account number: {}", savedAccount.getAccountNumber()));
                })
                .map(accountMapper::mapToAccountResponse)
                .doOnError(e -> log.error("Error processing BootCoin purchase: {}", e.getMessage()));
    }

    @Override
    public Mono<TransactionResponse> transfer(String accountNumber, TransferRequest transferRequest){
        String receiverAccountNumber = transferRequest.getReceiverAccountNumber();
        BigDecimal transferAmount = transferRequest.getAmount();

        log.info("Initiating transfer of {} from account {} to account {}", transferAmount, accountNumber, receiverAccountNumber);

        // Fetch and Validate Sender ---
        Mono<Account> validatedSender = accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found: " + accountNumber)))
                .flatMap(account -> {
                    // Sender Validations
                    if (account instanceof SavingsAccount savingsAccount) {
                        if (savingsAccount.getMovementsThisMonth() >= savingsAccount.getMonthlyMovementsLimit()) {
                            log.warn("Savings account {} has reached max movements limit", accountNumber);
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Savings account " + accountNumber + " has reached max movements limit ("
                                            + savingsAccount.getMonthlyMovementsLimit() + ") this month."));
                        }
                    } else if (account instanceof FixedTermAccount) {
                        log.warn("Fixed Term accounts cannot initiate transfers for account {}", accountNumber);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Fixed Term accounts cannot initiate transfers."));
                    }
                    // Basic validation passed
                    return Mono.just(account);
                })
                .doOnSuccess(account -> log.debug("Sender account {} validation passed", accountNumber))
                .doOnError(e -> log.error("Sender account {} validation failed: {}", accountNumber, e.getMessage()));

        // Fetch and Validate Receiver ---
        Mono<Account> validatedReceiver = accountRepository.findByAccountNumber(receiverAccountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver account not found: " +
                        receiverAccountNumber)))
                .flatMap(account -> {
                    // Receiver Type Validation
                    if (account.getAccountType() == Account.AccountType.FIXED_TERM) {
                        log.warn("Cannot transfer to Fixed Term account {}", receiverAccountNumber);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cannot transfer to a Fixed Term account."));
                    }
                    return Mono.just(account);
                })
                .doOnSuccess(account -> log.debug("Receiver account {} validation passed", receiverAccountNumber))
                .doOnError(e -> log.error("Receiver account {} validation failed: {}", receiverAccountNumber, e.getMessage()));

        return Mono.zip(validatedSender, validatedReceiver)
                .flatMap(accountsTuple ->
                        processTransferBetweenAccounts(accountsTuple, transferAmount))
                .flatMap(savedSenderAndFeeTuple -> {
                    Account senderAccount = savedSenderAndFeeTuple.getT1();
                    BigDecimal fee = savedSenderAndFeeTuple.getT2(); // Retrieve the fee calculated earlier

                    // Create Sender Transaction
                    log.info("Creating transaction for sender account {}", senderAccount.getAccountNumber());
                    TransactionRequest debitRequest = createTransferTransactionRequest(
                            senderAccount.getAccountNumber(), // Sender account
                            transferAmount,
                            TransactionRequest.TransactionType.DEBIT,
                            "Transfer to account " + receiverAccountNumber,
                            fee
                    );

                    // Create Receiver Transaction
                    log.info("Creating transaction for receiver account {}", receiverAccountNumber);
                    TransactionRequest creditRequest = createTransferTransactionRequest(
                            accountNumber, // Receiver account
                            transferAmount,
                            TransactionRequest.TransactionType.CREDIT,
                            "Transfer from account " + accountNumber,
                            BigDecimal.ZERO // Receiver does not get a fee
                    );

                    // Call Transaction Client for Both
                    Mono<TransactionResponse> debitTransactionMono = transactionClient.createTransaction(debitRequest)
                            .doOnSuccess(transactionResponse ->
                                    log.info("Debit transaction created for account {}, transaction ID: {}",
                                    accountNumber, transactionResponse.getId()))
                            .doOnError(e ->
                                    log.error("Failed to create debit transaction for account {}: {}", accountNumber, e.getMessage()));

                    Mono<TransactionResponse> creditTransactionMono = transactionClient.createTransaction(creditRequest)
                            .doOnSuccess(transactionResponse ->
                                    log.info("Credit transaction created for account {}, transaction ID: {}",
                                    receiverAccountNumber, transactionResponse.getId()))
                            .doOnError(e ->
                                    log.error("Failed to create credit transaction for account {}: {}", receiverAccountNumber, e.getMessage()));

                    // Returning the response from the DEBIT transaction
                    return Mono.zip(debitTransactionMono, creditTransactionMono)
                            .map(Tuple2::getT1);
                });
    }

    @Override
    public Flux<FeeReportResponse> generateFeeReportBetweenDate(String accountNumber,
                                                                LocalDateTime startDate,
                                                                LocalDateTime endDate) {
        return transactionClient.getTransactionsFeesByAccountNumberAndDateRange(accountNumber, startDate, endDate);
    }

    public Mono<Tuple2<Account, BigDecimal>> processTransferBetweenAccounts(Tuple2<Account, Account> accountsTuple,
                                                                            BigDecimal transferAmount) {
        Account senderAccount = accountsTuple.getT1();
        Account receiverAccount = accountsTuple.getT2();

        BigDecimal fee;
        // Check if Commission Fee for Sender is applicable
        if (senderAccount.getIsCommissionFeeActive()) {
            fee = senderAccount.getMovementCommissionFee();
            log.info("Applying commission fee of {} for sender account {}", fee, senderAccount.getAccountNumber());
        } else {
            fee = BigDecimal.ZERO;
        }

        BigDecimal totalDeduction = transferAmount.add(fee);

        if (senderAccount.getBalance().compareTo(totalDeduction) < 0) {
            log.warn("Insufficient funds in sender account {}. Required: {}", senderAccount.getAccountNumber(), totalDeduction);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient funds in sender account " +
                            senderAccount.getAccountNumber() +
                            " (Required: " + totalDeduction + ")"));
        }

        //  Update Account Balances
        senderAccount.setBalance(senderAccount.getBalance().subtract(totalDeduction));
        receiverAccount.setBalance(receiverAccount.getBalance().add(transferAmount));

        // Update movements for Savings Account
        if (senderAccount instanceof SavingsAccount savingsAccount) {
            log.info("Incrementeding movements for sender savings account {}", senderAccount.getAccountNumber());
            savingsAccount.setMovementsThisMonth(savingsAccount.getMovementsThisMonth() + 1);
        }

        return accountRepository.save(senderAccount)
                .flatMap(savedSender -> accountRepository.save(receiverAccount)
                        .map(savedReceiver -> {
                            log.info("Successfully transferred {} from account {} to account {}",
                                    transferAmount, senderAccount.getAccountNumber(), receiverAccount.getAccountNumber());
                            return Tuples.of(savedSender, fee);
                        })
                )
                .doOnError(e -> log.error("Error processing transfer between accounts: {}", e.getMessage()));
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
        AccountMovementProcessStrategy movementProcessStrategy = movementProcessStrategies.get(account.getAccountType());
        if (movementProcessStrategy == null) {
            log.error("No movement process strategy found for account type: {}", account.getAccountType());
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No movement process strategy found for account type: " + account.getAccountType()));
        }

        return movementProcessStrategy.processMovement(account)
                .flatMap(processedAccount -> {
                    log.info("Increasing movement count for account number: {}", processedAccount.getAccountNumber());
                    // Increase movement count
                    account.setMovementsThisMonth(account.getMovementsThisMonth() + 1);

                    // Check if commission fee should be activated
                    if (!account.getIsCommissionFeeActive() &&
                            account.getMovementsThisMonth().equals(account.getMaxMovementsFeeFreeThisMonth())) {
                        log.info("Commission activated for account number: {}", account.getAccountNumber());
                        account.setIsCommissionFeeActive(true);
                    }
                    return Mono.just(account);
                });
    }

    private Mono<Account> validateFixedAccountDeposit(Account account) {
        log.info("Validating Fixed Term account deposit for account number: {}", account.getAccountNumber());

        if(account.getAccountType() == Account.AccountType.FIXED_TERM && account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("One deposit has already been performed for Fixed Term account number: {}", account.getAccountNumber());
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One deposit has already been performed for this Fixed Term account"));
        }
        log.info("Fixed Term deposit validation passed for account number: {}", account.getAccountNumber());
        return Mono.just(account);
    }

    private Mono<Account> validateFixedAccountWithdraw(Account account) {
        log.info("Validating Fixed Term account withdrawal for account number: {}", account.getAccountNumber());
        if (account.getAccountType() == Account.AccountType.FIXED_TERM) {
            FixedTermAccount fixedTermAccount = (FixedTermAccount) account;
            if (fixedTermAccount.getAllowedWithdrawal().isAfter(LocalDate.now())) {
                log.warn("Withdrawal not allowed until {} for Fixed Term account number: {}",
                        fixedTermAccount.getAllowedWithdrawal(), account.getAccountNumber());
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Withdrawal not allowed until " + fixedTermAccount.getAllowedWithdrawal()));
            }
        }
        log.info("Fixed Term withdrawal validation passed for account number: {}", account.getAccountNumber());
        return Mono.just(account);
    }

    private TransactionRequest createTransactionRequest(Account savedAccount,
                                                        BigDecimal amount,
                                                        TransactionRequest.TransactionType transactionType,
                                                        String description) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(savedAccount.getAccountNumber());
        transactionRequest.setAmount(amount);
        transactionRequest.setTransactionType(transactionType);
        transactionRequest.setDescription(description);
        transactionRequest.setFee(savedAccount.getIsCommissionFeeActive()
                ? savedAccount.getMovementCommissionFee() : BigDecimal.ZERO);
        return transactionRequest;
    }

    private TransactionRequest createTransferTransactionRequest(String accountNumber,
                                                        BigDecimal amount,
                                                        TransactionRequest.TransactionType transactionType,
                                                        String description, BigDecimal fee) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(accountNumber);
        transactionRequest.setAmount(amount);
        transactionRequest.setTransactionType(transactionType);
        transactionRequest.setDescription(description);
        transactionRequest.setFee(fee);
        return transactionRequest;
    }

}
