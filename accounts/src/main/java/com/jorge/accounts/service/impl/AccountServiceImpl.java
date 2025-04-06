package com.jorge.accounts.service.impl;

import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.model.CustomerResponse;
import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.service.AccountService;
import com.jorge.accounts.utils.AccountUtils;
import com.jorge.accounts.webclient.model.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                                if (savedAccount.getIsCommissionFeeActive()) transactionRequest.setFee(savedAccount.getMovementCommissionFee());
                                else transactionRequest.setFee(BigDecimal.ZERO);

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

                    // If the withdrawal is on a FIXED_TERM account
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
