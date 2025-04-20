package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.DebitCardMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.repository.DebitCardRepository;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.request.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DebitCardServiceImplTest {
    @Mock
    private DebitCardRepository debitCardRepository;
    @Mock
    private AccountRepository accountRepository;
    @Spy
    private DebitCardMapper debitCardMapper;
    @Mock
    private TransactionClient transactionClient;
    @InjectMocks
    private DebitCardServiceImpl debitCardServiceImpl;

    private String debitCardNumber;
    private DebitCard debitCard;

    private String mainAccountNumber;
    private Account account;

    private String secondAccountNumber;
    private Account secondAccount;

    @BeforeEach
    void setUp(){
        mainAccountNumber = "1234";
        secondAccountNumber = "5678";

        debitCardNumber = "1234123412341234";
        debitCard = DebitCard.builder()
                .id(UUID.randomUUID().toString())
                .cardHolderId("100")
                .linkedAccountsNumber(List.of(mainAccountNumber, secondAccountNumber))
                .mainLinkedAccountNumber(mainAccountNumber)
                .debitCardNumber(debitCardNumber)
                .cvv("123")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(DebitCard.DebitCardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void whenGetBalanceByDebitCardNumber_WithExistingDebitCard_ThenReturnBalance() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));

        account = new SavingsAccount();
        account.setAccountNumber(debitCard.getMainLinkedAccountNumber());
        account.setBalance(BigDecimal.valueOf(1000.0));
        account.setAccountType(Account.AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())).thenReturn(Mono.just(account));

        // Call the method under test
        Mono<BalanceResponse> balanceMono = debitCardServiceImpl.getBalanceByDebitCardNumber(debitCardNumber);

        // Verify the result
        StepVerifier.create(balanceMono)
                .assertNext(balanceResponse -> {
                    assertEquals(debitCard.getMainLinkedAccountNumber(), balanceResponse.getAccountNumber());
                    assertEquals(account.getBalance(), balanceResponse.getBalance());
                    assertEquals(account.getAccountType().name(), balanceResponse.getAccountType().name());
                })
                .verifyComplete();
    }

    @Test
    void whenWithdrawalByDebitCard_WithExistingDebitCard_ThenReturnBalance() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));

        account = new SavingsAccount();
        account.setAccountNumber(debitCard.getMainLinkedAccountNumber());
        account.setBalance(BigDecimal.valueOf(1000.0));
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setIsCommissionFeeActive(true);
        account.setMovementCommissionFee(BigDecimal.valueOf(0.50));

        when(accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())).thenReturn(Mono.just(account));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(account));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse()));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(BigDecimal.valueOf(500.0));

        Mono<BalanceResponse> balanceMono = debitCardServiceImpl.withdrawByDebitCardNumber(debitCardNumber, withdrawalRequest);

        StepVerifier.create(balanceMono)
                .assertNext(balanceResponse -> {
                    assertEquals(debitCard.getMainLinkedAccountNumber(), balanceResponse.getAccountNumber());
                    assertEquals(account.getBalance(), balanceResponse.getBalance());
                    assertEquals(account.getAccountType().name(), balanceResponse.getAccountType().name());
                })
                .verifyComplete();
    }

    @Test
    void whenWithdrawalByDebitCard_UseSecondAccountBalance_ThenReturnBalance(){
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));

        account = new SavingsAccount();
        account.setAccountNumber(debitCard.getMainLinkedAccountNumber());
        account.setBalance(BigDecimal.valueOf(50.0));
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setIsCommissionFeeActive(false);

        secondAccount = new SavingsAccount();
        secondAccount.setAccountNumber(debitCard.getMainLinkedAccountNumber());
        secondAccount.setBalance(BigDecimal.valueOf(1000.0));
        secondAccount.setAccountType(Account.AccountType.SAVINGS);
        secondAccount.setIsCommissionFeeActive(false);

        when(accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())).thenReturn(Mono.just(account));

        when(accountRepository.findByAccountNumber(secondAccountNumber)).thenReturn(Mono.just(secondAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(secondAccount));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse()));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(BigDecimal.valueOf(100.0));

        Mono<BalanceResponse> balanceMono = debitCardServiceImpl.withdrawByDebitCardNumber(debitCardNumber, withdrawalRequest);

        StepVerifier.create(balanceMono)
                .assertNext(balanceResponse -> {
                    assertEquals(debitCard.getMainLinkedAccountNumber(), balanceResponse.getAccountNumber());
                    assertEquals(secondAccount.getBalance(), balanceResponse.getBalance());
                    assertEquals(secondAccount.getAccountType().name(), balanceResponse.getAccountType().name());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionsByDebitCardNumberLast10_WithExistingDebitCard_ThenReturnTransactions(){
        account = new SavingsAccount();
        account.setAccountNumber(debitCard.getMainLinkedAccountNumber());
        account.setBalance(BigDecimal.valueOf(50.0));
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setIsCommissionFeeActive(false);

        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));
        when(accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())).thenReturn(Mono.just(account));

        List<TransactionResponse> transactions = new ArrayList<>();
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setId(UUID.randomUUID().toString());
        transactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.DEPOSIT);
        transactionResponse.setAmount(BigDecimal.valueOf(100.0));
        transactionResponse.setAccountNumber(debitCard.getMainLinkedAccountNumber());
        transactions.add(transactionResponse);

        when(transactionClient.getTransactionsByAccountNumber(account.getAccountNumber())).thenReturn(Flux.fromIterable(transactions));

        Flux<TransactionResponse> transactionsFlux = debitCardServiceImpl.getTransactionsByDebitCardNumberLast10(debitCardNumber);

        StepVerifier.create(transactionsFlux)
                .assertNext(transaction1 -> {
                    assertEquals(transactionResponse.getId(), transaction1.getId());
                    assertEquals(transactionResponse.getTransactionType(), transaction1.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), transaction1.getAmount());
                    assertEquals(transactionResponse.getAccountNumber(), transaction1.getAccountNumber());
                })
                .verifyComplete();
    }
}
