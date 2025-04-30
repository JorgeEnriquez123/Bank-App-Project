package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.service.strategy.CheckingAccountMovementProcessingStrategy;
import com.jorge.accounts.service.strategy.FixedAccountMovementProcessingStrategy;
import com.jorge.accounts.service.strategy.SavingsAccountMovementProcessingStrategy;
import com.jorge.accounts.service.strategy.business.AccountMovementProcessStrategy;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.request.TransactionRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionClient transactionClient;
    @Spy // Spy the mapper to use the real implementation
    private AccountMapper accountMapper;

    // We need a real Map of strategies, so we'll create and inject it manually
    private Map<Account.AccountType, AccountMovementProcessStrategy> movementProcessStrategies;

    @InjectMocks
    private AccountServiceImpl accountServiceImpl;

    private String accountNumber;
    private String customerId;
    private SavingsAccount savingsAccount;
    private CheckingAccount checkingAccount;
    private FixedTermAccount fixedTermAccount;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        accountNumber = "12345678901234";
        customerId = "customer123";

        // Manually create and populate the strategies map
        movementProcessStrategies = new EnumMap<>(Account.AccountType.class);
        movementProcessStrategies.put(Account.AccountType.SAVINGS, new SavingsAccountMovementProcessingStrategy());
        movementProcessStrategies.put(Account.AccountType.CHECKING, new CheckingAccountMovementProcessingStrategy());
        movementProcessStrategies.put(Account.AccountType.FIXED_TERM, new FixedAccountMovementProcessingStrategy());

        // Inject the manually created map into the service
        accountServiceImpl = new AccountServiceImpl(accountMapper, accountRepository, transactionClient, movementProcessStrategies);

        // Setup common account objects
        savingsAccount = new SavingsAccount();
        savingsAccount.setId(UUID.randomUUID().toString());
        savingsAccount.setAccountNumber(accountNumber);
        savingsAccount.setCustomerId(customerId);
        savingsAccount.setBalance(BigDecimal.valueOf(1000.0));
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setCreatedAt(LocalDateTime.now());
        savingsAccount.setMovementsThisMonth(0);
        savingsAccount.setMaxMovementsFeeFreeThisMonth(10);
        savingsAccount.setIsCommissionFeeActive(false);
        savingsAccount.setMovementCommissionFee(BigDecimal.ZERO);
        savingsAccount.setMonthlyMovementsLimit(20);

        checkingAccount = new CheckingAccount();
        checkingAccount.setId(UUID.randomUUID().toString());
        checkingAccount.setAccountNumber("98765432109876");
        checkingAccount.setCustomerId(customerId);
        checkingAccount.setBalance(BigDecimal.valueOf(2000.0));
        checkingAccount.setAccountType(Account.AccountType.CHECKING);
        checkingAccount.setCreatedAt(LocalDateTime.now());
        checkingAccount.setMovementsThisMonth(0);
        checkingAccount.setMaxMovementsFeeFreeThisMonth(Integer.MAX_VALUE);
        checkingAccount.setIsCommissionFeeActive(false);
        checkingAccount.setMovementCommissionFee(BigDecimal.ZERO);
        checkingAccount.setMaintenanceFee(BigDecimal.valueOf(10.0));
        checkingAccount.setHolders(List.of("holder1"));
        checkingAccount.setAuthorizedSigners(List.of("signer1"));


        fixedTermAccount = new FixedTermAccount();
        fixedTermAccount.setId(UUID.randomUUID().toString());
        fixedTermAccount.setAccountNumber("45678901234567");
        fixedTermAccount.setCustomerId(customerId);
        fixedTermAccount.setBalance(BigDecimal.valueOf(5000.0));
        fixedTermAccount.setAccountType(Account.AccountType.FIXED_TERM);
        fixedTermAccount.setCreatedAt(LocalDateTime.now());
        fixedTermAccount.setMovementsThisMonth(0);
        fixedTermAccount.setMaxMovementsFeeFreeThisMonth(1);
        fixedTermAccount.setIsCommissionFeeActive(false);
        fixedTermAccount.setMovementCommissionFee(BigDecimal.ZERO);
        fixedTermAccount.setAllowedWithdrawal(LocalDate.now().minusDays(1)); // Allowed to withdraw

        transactionResponse = new TransactionResponse();
        transactionResponse.setId("txn123");
        transactionResponse.setAccountNumber(accountNumber);
        transactionResponse.setAmount(BigDecimal.valueOf(500.0));
        transactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.DEPOSIT);
        transactionResponse.setDescription("Test Deposit");
        transactionResponse.setCreatedAt(LocalDateTime.now());

        // Lenient stubbing for general mocks that might be called in different tests
        lenient().when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(transactionResponse));
    }

    @Test
    void whenGetAllAccounts_ThenReturnFluxOfAccountResponse() {
        when(accountRepository.findAll()).thenReturn(Flux.just(savingsAccount, checkingAccount));

        StepVerifier.create(accountServiceImpl.getAllAccounts())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void whenGetAccountByAccountNumber_WithExistingAccount_ThenReturnAccountResponse() {
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(savingsAccount));

        StepVerifier.create(accountServiceImpl.getAccountByAccountNumber(accountNumber))
                .expectNextMatches(response -> response.getAccountNumber().equals(accountNumber))
                .verifyComplete();
    }

    @Test
    void whenGetAccountByAccountNumber_WithNonExistingAccount_ThenThrowNotFoundException() {
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.empty());

        StepVerifier.create(accountServiceImpl.getAccountByAccountNumber(accountNumber))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Account with account number: " + accountNumber + " not found"))
                .verify();
    }

    @Test
    void whenDeleteAccountByAccountNumber_ThenReturnVoid() {
        when(accountRepository.deleteByAccountNumber(accountNumber)).thenReturn(Mono.empty());

        StepVerifier.create(accountServiceImpl.deleteAccountByAccountNumber(accountNumber))
                .verifyComplete();
    }

    @Test
    void whenGetBalanceByAccountNumber_WithExistingAccount_ThenReturnBalanceResponse() {
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(savingsAccount));

        StepVerifier.create(accountServiceImpl.getBalanceByAccountNumber(accountNumber))
                .expectNextMatches(response -> response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(1000.0)) == 0 &&
                        response.getAccountType() == BalanceResponse.AccountTypeEnum.SAVINGS)
                .verifyComplete();
    }

    @Test
    void whenGetBalanceByAccountNumber_WithNonExistingAccount_ThenThrowNotFoundException() {
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.empty());

        StepVerifier.create(accountServiceImpl.getBalanceByAccountNumber(accountNumber))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Account with account number: " + accountNumber + " not found"))
                .verify();
    }

    @Test
    void whenGetAccountsByCustomerId_ThenReturnFluxOfAccountResponse() {
        when(accountRepository.findByCustomerId(customerId)).thenReturn(Flux.just(savingsAccount, checkingAccount));

        StepVerifier.create(accountServiceImpl.getAccountsByCustomerId(customerId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void whenIncreaseBalanceByAccountNumber_WithExistingAccount_ThenReturnBalanceResponse() {
        BigDecimal increaseAmount = BigDecimal.valueOf(500.0);
        Account accountAfterIncrease = new SavingsAccount();
        accountAfterIncrease.setAccountNumber(accountNumber);
        accountAfterIncrease.setBalance(savingsAccount.getBalance().add(increaseAmount));
        accountAfterIncrease.setAccountType(Account.AccountType.SAVINGS); // Ensure type is set for mapping

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(accountAfterIncrease));

        StepVerifier.create(accountServiceImpl.increaseBalanceByAccountNumber(accountNumber, increaseAmount))
                .expectNextMatches(response -> response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(1500.0)) == 0)
                .verifyComplete();
    }

    @Test
    void whenDecreaseBalanceByAccountNumber_WithSufficientBalance_ThenReturnBalanceResponse() {
        BigDecimal decreaseAmount = BigDecimal.valueOf(200.0);
        Account accountAfterDecrease = new SavingsAccount();
        accountAfterDecrease.setAccountNumber(accountNumber);
        accountAfterDecrease.setBalance(savingsAccount.getBalance().subtract(decreaseAmount));
        accountAfterDecrease.setAccountType(Account.AccountType.SAVINGS); // Ensure type is set for mapping


        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(accountAfterDecrease));

        StepVerifier.create(accountServiceImpl.decreaseBalanceByAccountNumber(accountNumber, decreaseAmount))
                .expectNextMatches(response -> response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(800.0)) == 0)
                .verifyComplete();
    }

    @Test
    void whenDepositByAccountNumber_WithSavingsAccountAndNoFee_ThenReturnAccountResponse() {
        BigDecimal depositAmount = BigDecimal.valueOf(500.0);
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(depositAmount);

        SavingsAccount accountAfterMovementProcess = new SavingsAccount();
        accountAfterMovementProcess.setId(savingsAccount.getId());
        accountAfterMovementProcess.setAccountNumber(accountNumber);
        accountAfterMovementProcess.setCustomerId(customerId);
        accountAfterMovementProcess.setBalance(savingsAccount.getBalance());
        accountAfterMovementProcess.setAccountType(Account.AccountType.SAVINGS);
        accountAfterMovementProcess.setCreatedAt(savingsAccount.getCreatedAt());
        accountAfterMovementProcess.setMovementsThisMonth(savingsAccount.getMovementsThisMonth() + 1); // Simulate movement count increase
        accountAfterMovementProcess.setMaxMovementsFeeFreeThisMonth(savingsAccount.getMaxMovementsFeeFreeThisMonth());
        accountAfterMovementProcess.setIsCommissionFeeActive(false); // No fee active
        accountAfterMovementProcess.setMovementCommissionFee(BigDecimal.ZERO);
        accountAfterMovementProcess.setMonthlyMovementsLimit(savingsAccount.getMonthlyMovementsLimit());


        SavingsAccount accountAfterDeposit = new SavingsAccount();
        accountAfterDeposit.setId(savingsAccount.getId());
        accountAfterDeposit.setAccountNumber(accountNumber);
        accountAfterDeposit.setCustomerId(customerId);
        accountAfterDeposit.setBalance(accountAfterMovementProcess.getBalance().add(depositAmount)); // Balance after deposit
        accountAfterDeposit.setAccountType(Account.AccountType.SAVINGS);
        accountAfterDeposit.setCreatedAt(savingsAccount.getCreatedAt());
        accountAfterDeposit.setMovementsThisMonth(accountAfterMovementProcess.getMovementsThisMonth());
        accountAfterDeposit.setMaxMovementsFeeFreeThisMonth(accountAfterMovementProcess.getMaxMovementsFeeFreeThisMonth());
        accountAfterDeposit.setIsCommissionFeeActive(accountAfterMovementProcess.getIsCommissionFeeActive());
        accountAfterDeposit.setMovementCommissionFee(accountAfterMovementProcess.getMovementCommissionFee());
        accountAfterDeposit.setMonthlyMovementsLimit(accountAfterMovementProcess.getMonthlyMovementsLimit());


        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(savingsAccount));
        // Simulate the behavior after processAccountMovement (which increments movements)
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(accountAfterDeposit));


        StepVerifier.create(accountServiceImpl.depositByAccountNumber(accountNumber, depositRequest))
                .expectNextMatches(response -> response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(1500.0)) == 0 &&
                        response.getMovementsThisMonth() == 1) // Verify movement count increased
                .verifyComplete();
    }

    @Test
    void whenWithdrawByAccountNumber_WithSavingsAccountAndInsufficientBalance_ThenThrowBadRequestException() {
        BigDecimal withdrawalAmount = BigDecimal.valueOf(1500.0);
        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(withdrawalAmount);

        SavingsAccount accountAfterMovementProcess = new SavingsAccount();
        accountAfterMovementProcess.setId(savingsAccount.getId());
        accountAfterMovementProcess.setAccountNumber(accountNumber);
        accountAfterMovementProcess.setCustomerId(customerId);
        accountAfterMovementProcess.setBalance(savingsAccount.getBalance()); // Still the initial balance
        accountAfterMovementProcess.setAccountType(Account.AccountType.SAVINGS);
        accountAfterMovementProcess.setCreatedAt(savingsAccount.getCreatedAt());
        accountAfterMovementProcess.setMovementsThisMonth(savingsAccount.getMovementsThisMonth() + 1); // Simulate movement count increase
        accountAfterMovementProcess.setMaxMovementsFeeFreeThisMonth(savingsAccount.getMaxMovementsFeeFreeThisMonth());
        accountAfterMovementProcess.setIsCommissionFeeActive(false); // No fee active
        accountAfterMovementProcess.setMovementCommissionFee(BigDecimal.ZERO);
        accountAfterMovementProcess.setMonthlyMovementsLimit(savingsAccount.getMonthlyMovementsLimit());

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(savingsAccount));

        StepVerifier.create(accountServiceImpl.withdrawByAccountNumber(accountNumber, withdrawalRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Insufficient balance for withdrawal"))
                .verify();
    }

    @Test
    void whenWithdrawByAccountNumber_WithSavingsAccountAndFeeActive_ThenApplyFee() {
        BigDecimal withdrawalAmount = BigDecimal.valueOf(100.0);
        BigDecimal fee = BigDecimal.valueOf(5.0);
        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(withdrawalAmount);

        // Account state when fee is active
        SavingsAccount accountWithFee = new SavingsAccount();
        accountWithFee.setId(savingsAccount.getId());
        accountWithFee.setAccountNumber(accountNumber);
        accountWithFee.setCustomerId(customerId);
        accountWithFee.setBalance(BigDecimal.valueOf(500.0)); // Sufficient balance
        accountWithFee.setAccountType(Account.AccountType.SAVINGS);
        accountWithFee.setCreatedAt(LocalDateTime.now());
        accountWithFee.setMovementsThisMonth(11); // Exceeded free movements
        accountWithFee.setMaxMovementsFeeFreeThisMonth(10);
        accountWithFee.setIsCommissionFeeActive(true); // Fee is active
        accountWithFee.setMovementCommissionFee(fee);
        accountWithFee.setMonthlyMovementsLimit(20);

        // Account state after successful withdrawal and fee
        SavingsAccount accountAfterWithdrawalAndFee = new SavingsAccount();
        accountAfterWithdrawalAndFee.setId(accountWithFee.getId());
        accountAfterWithdrawalAndFee.setAccountNumber(accountNumber);
        accountAfterWithdrawalAndFee.setCustomerId(customerId);
        accountAfterWithdrawalAndFee.setBalance(accountWithFee.getBalance().subtract(withdrawalAmount).subtract(fee)); // Balance after deduction
        accountAfterWithdrawalAndFee.setAccountType(Account.AccountType.SAVINGS);
        accountAfterWithdrawalAndFee.setCreatedAt(accountWithFee.getCreatedAt());
        accountAfterWithdrawalAndFee.setMovementsThisMonth(accountWithFee.getMovementsThisMonth() + 1); // Simulate movement count increase
        accountAfterWithdrawalAndFee.setMaxMovementsFeeFreeThisMonth(accountWithFee.getMaxMovementsFeeFreeThisMonth());
        accountAfterWithdrawalAndFee.setIsCommissionFeeActive(accountWithFee.getIsCommissionFeeActive());
        accountAfterWithdrawalAndFee.setMovementCommissionFee(accountWithFee.getMovementCommissionFee());
        accountAfterWithdrawalAndFee.setMonthlyMovementsLimit(accountWithFee.getMonthlyMovementsLimit());

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(accountWithFee));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(accountAfterWithdrawalAndFee));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse())); // Mock transaction creation

        StepVerifier.create(accountServiceImpl.withdrawByAccountNumber(accountNumber, withdrawalRequest))
                .expectNextMatches(response -> response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(500.0).subtract(BigDecimal.valueOf(100.0)).subtract(BigDecimal.valueOf(5.0))) == 0)
                .verifyComplete();
    }


    @Test
    void whenTransfer_WithValidSavingsAccounts_ThenCreateTransactions() {
        String receiverAccountNumber = "98765432109876";
        BigDecimal transferAmount = BigDecimal.valueOf(100.0);
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setReceiverAccountNumber(receiverAccountNumber);
        transferRequest.setAmount(transferAmount);

        SavingsAccount senderAccount = new SavingsAccount();
        senderAccount.setId(UUID.randomUUID().toString());
        senderAccount.setAccountNumber(accountNumber);
        senderAccount.setCustomerId(customerId);
        senderAccount.setBalance(BigDecimal.valueOf(1000.0));
        senderAccount.setAccountType(Account.AccountType.SAVINGS);
        senderAccount.setMovementsThisMonth(0);
        senderAccount.setMonthlyMovementsLimit(20);
        senderAccount.setIsCommissionFeeActive(false); // No fee for sender
        senderAccount.setMovementCommissionFee(BigDecimal.ZERO);

        Account receiverAccount = new SavingsAccount(); // Receiver can be Savings
        receiverAccount.setId(UUID.randomUUID().toString());
        receiverAccount.setAccountNumber(receiverAccountNumber);
        receiverAccount.setCustomerId("receiverCustomer");
        receiverAccount.setBalance(BigDecimal.valueOf(500.0));
        receiverAccount.setAccountType(Account.AccountType.SAVINGS);


        SavingsAccount senderAccountAfterTransfer = new SavingsAccount();
        senderAccountAfterTransfer.setId(senderAccount.getId());
        senderAccountAfterTransfer.setAccountNumber(accountNumber);
        senderAccountAfterTransfer.setCustomerId(customerId);
        senderAccountAfterTransfer.setBalance(senderAccount.getBalance().subtract(transferAmount)); // Balance after transfer
        senderAccountAfterTransfer.setAccountType(Account.AccountType.SAVINGS);
        senderAccountAfterTransfer.setMovementsThisMonth(senderAccount.getMovementsThisMonth() + 1); // Movements incremented
        senderAccountAfterTransfer.setMonthlyMovementsLimit(senderAccount.getMonthlyMovementsLimit());
        senderAccountAfterTransfer.setIsCommissionFeeActive(senderAccount.getIsCommissionFeeActive());
        senderAccountAfterTransfer.setMovementCommissionFee(senderAccount.getMovementCommissionFee());

        Account receiverAccountAfterTransfer = new SavingsAccount();
        receiverAccountAfterTransfer.setId(receiverAccount.getId());
        receiverAccountAfterTransfer.setAccountNumber(receiverAccountNumber);
        receiverAccountAfterTransfer.setCustomerId("receiverCustomer");
        receiverAccountAfterTransfer.setBalance(receiverAccount.getBalance().add(transferAmount)); // Balance after transfer
        receiverAccountAfterTransfer.setAccountType(Account.AccountType.SAVINGS);


        TransactionResponse debitTransactionResponse = new TransactionResponse();
        debitTransactionResponse.setId("debitTxn");
        debitTransactionResponse.setAccountNumber(accountNumber);
        debitTransactionResponse.setAmount(transferAmount);
        debitTransactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.DEBIT);

        TransactionResponse creditTransactionResponse = new TransactionResponse();
        creditTransactionResponse.setId("creditTxn");
        creditTransactionResponse.setAccountNumber(receiverAccountNumber);
        creditTransactionResponse.setAmount(transferAmount);
        creditTransactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.CREDIT);


        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(senderAccount));
        when(accountRepository.findByAccountNumber(receiverAccountNumber)).thenReturn(Mono.just(receiverAccount));
        when(accountRepository.save(senderAccount)).thenReturn(Mono.just(senderAccountAfterTransfer));
        when(accountRepository.save(receiverAccount)).thenReturn(Mono.just(receiverAccountAfterTransfer));

        // Mock transaction creation for both debit and credit
        when(transactionClient.createTransaction(any(TransactionRequest.class)))
                .thenAnswer(invocation -> {
                    TransactionRequest req = invocation.getArgument(0);
                    if (req.getTransactionType() == TransactionRequest.TransactionType.DEBIT) {
                        return Mono.just(debitTransactionResponse);
                    } else if (req.getTransactionType() == TransactionRequest.TransactionType.CREDIT) {
                        return Mono.just(creditTransactionResponse);
                    }
                    return Mono.error(new RuntimeException("Unexpected transaction type"));
                });


        StepVerifier.create(accountServiceImpl.transfer(accountNumber, transferRequest))
                .expectNextMatches(response -> response.getId().equals("debitTxn") && // The method returns the debit transaction response
                        response.getAccountNumber().equals(accountNumber) &&
                        response.getAmount().compareTo(transferAmount) == 0 &&
                        response.getTransactionType() == TransactionResponse.TransactionTypeEnum.DEBIT)
                .verifyComplete();
    }

    @Test
    void whenTransfer_WithFixedTermSender_ThenThrowBadRequestException() {
        String receiverAccountNumber = "98765432109876";
        BigDecimal transferAmount = BigDecimal.valueOf(100.0);
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setReceiverAccountNumber(receiverAccountNumber);
        transferRequest.setAmount(transferAmount);

        fixedTermAccount.setAllowedWithdrawal(LocalDate.now().minusDays(1));

        Account dummyReceiverAccount = new SavingsAccount();
        dummyReceiverAccount.setAccountNumber(receiverAccountNumber);
        dummyReceiverAccount.setAccountType(Account.AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(fixedTermAccount));

        when(accountRepository.findByAccountNumber(receiverAccountNumber)).thenReturn(Mono.just(dummyReceiverAccount));

        StepVerifier.create(accountServiceImpl.transfer(accountNumber, transferRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Fixed Term accounts cannot initiate transfers."))
                .verify();
    }

    @Test
    void whenTransfer_ToFixedTermReceiver_ThenThrowBadRequestException() {
        String receiverAccountNumber = "45678901234567"; // Fixed Term Account number
        BigDecimal transferAmount = BigDecimal.valueOf(100.0);
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setReceiverAccountNumber(receiverAccountNumber);
        transferRequest.setAmount(transferAmount);

        SavingsAccount senderAccount = new SavingsAccount();
        senderAccount.setAccountNumber(accountNumber);
        senderAccount.setBalance(BigDecimal.valueOf(1000.0));
        senderAccount.setAccountType(Account.AccountType.SAVINGS);
        senderAccount.setMovementsThisMonth(0);
        senderAccount.setMonthlyMovementsLimit(20);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(senderAccount));
        when(accountRepository.findByAccountNumber(receiverAccountNumber)).thenReturn(Mono.just(fixedTermAccount)); // Receiver is Fixed Term

        StepVerifier.create(accountServiceImpl.transfer(accountNumber, transferRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Cannot transfer to a Fixed Term account."))
                .verify();
    }


    @Test
    void whenGetTransactionsByAccountNumber_ThenReturnFluxOfTransactionResponse() {
        when(transactionClient.getTransactionsByAccountNumber(accountNumber)).thenReturn(Flux.just(transactionResponse, new TransactionResponse()));

        StepVerifier.create(accountServiceImpl.getTransactionsByAccountNumber(accountNumber))
                .expectNextCount(2)
                .verifyComplete();
    }

    // Note: AverageMonthlyDailyBalance calculation is complex and involves date logic and transaction processing.
    // Testing this thoroughly would require mocking the transactionClient.getTransactionsByAccountNumberAndDateRange
    // with various scenarios (transactions before and after the month, multiple transactions in a day, etc.).
    // Due to the complexity and the request for limited tests, we'll skip a detailed test for this method.
    // A full test would involve:
    // 1. Mocking transactionClient.getTransactionsByAccountNumberAndDateRange for transactions *before* the start date to calculate initial balance.
    // 2. Mocking transactionClient.getTransactionsByAccountNumberAndDateRange for transactions *within* the date range.
    // 3. Carefully constructing TransactionResponse objects with specific amounts and dates.
    // 4. Asserting the calculated average daily balance.


    @Test
    void whenGenerateFeeReportBetweenDate_ThenReturnFluxOfFeeReportResponse() {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = LocalDateTime.now();

        FeeReportResponse feeReportResponse1 = new FeeReportResponse();
        feeReportResponse1.setAmount(BigDecimal.valueOf(10.0));
        feeReportResponse1.setType(FeeReportResponse.TypeEnum.TRANSACTION_FEE);
        feeReportResponse1.setDate(LocalDateTime.now().minusDays(15));

        FeeReportResponse feeReportResponse2 = new FeeReportResponse();
        feeReportResponse2.setAmount(BigDecimal.valueOf(15.0));
        feeReportResponse2.setType(FeeReportResponse.TypeEnum.MAINTENANCE_FEE);
        feeReportResponse2.setDate(LocalDateTime.now().minusDays(5));


        when(transactionClient.getTransactionsFeesByAccountNumberAndDateRange(accountNumber, startDate, endDate))
                .thenReturn(Flux.just(feeReportResponse1, feeReportResponse2));

        StepVerifier.create(accountServiceImpl.generateFeeReportBetweenDate(accountNumber, startDate, endDate))
                .expectNextMatches(response -> response.getAmount().compareTo(BigDecimal.valueOf(10.0)) == 0 &&
                        response.getType() == FeeReportResponse.TypeEnum.TRANSACTION_FEE)
                .expectNextMatches(response -> response.getAmount().compareTo(BigDecimal.valueOf(15.0)) == 0 &&
                        response.getType() == FeeReportResponse.TypeEnum.MAINTENANCE_FEE)
                .verifyComplete();
    }
}
