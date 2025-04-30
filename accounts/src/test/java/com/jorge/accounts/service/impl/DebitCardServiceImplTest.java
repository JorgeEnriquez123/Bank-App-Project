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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

        Mono<BalanceResponse> balanceMono = debitCardServiceImpl.getBalanceByDebitCardNumber(debitCardNumber);

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

    @Test
    void whenGetAllDebitCards_thenReturnDebitCardList() {
        List<DebitCard> debitCardList = new ArrayList<>();
        debitCardList.add(debitCard);
        when(debitCardRepository.findAll()).thenReturn(Flux.fromIterable(debitCardList));

        Flux<DebitCardResponse> debitCardResponseFlux = debitCardServiceImpl.getAllDebitCards();

        StepVerifier.create(debitCardResponseFlux)
                .assertNext(debitCardResponse -> {
                    assertEquals(debitCard.getId(), debitCardResponse.getId());
                    assertEquals(debitCard.getCardHolderId(), debitCardResponse.getCardHolderId());
                    assertEquals(debitCard.getDebitCardNumber(), debitCardResponse.getDebitCardNumber());
                })
                .verifyComplete();
    }

    @Test
    void whenGetDebitCardById_WithExistingId_ThenReturnDebitCard() {
        String id = UUID.randomUUID().toString();
        debitCard.setId(id);
        when(debitCardRepository.findById(id)).thenReturn(Mono.just(debitCard));

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.getDebitCardById(id);

        StepVerifier.create(debitCardResponseMono)
                .assertNext(debitCardResponse -> {
                    assertEquals(debitCard.getId(), debitCardResponse.getId());
                    assertEquals(debitCard.getCardHolderId(), debitCardResponse.getCardHolderId());
                    assertEquals(debitCard.getDebitCardNumber(), debitCardResponse.getDebitCardNumber());
                })
                .verifyComplete();
    }

    @Test
    void whenGetDebitCardById_WithNonExistingId_ThenReturnNotFound() {
        String id = UUID.randomUUID().toString();
        when(debitCardRepository.findById(id)).thenReturn(Mono.empty());

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.getDebitCardById(id);

        StepVerifier.create(debitCardResponseMono)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void whenGetDebitCardByDebitCardNumber_WithExistingNumber_ThenReturnDebitCard() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.getDebitCardByDebitCardNumber(debitCardNumber);

        StepVerifier.create(debitCardResponseMono)
                .assertNext(debitCardResponse -> {
                    assertEquals(debitCard.getId(), debitCardResponse.getId());
                    assertEquals(debitCard.getCardHolderId(), debitCardResponse.getCardHolderId());
                    assertEquals(debitCard.getDebitCardNumber(), debitCardResponse.getDebitCardNumber());
                })
                .verifyComplete();
    }

    @Test
    void whenGetDebitCardByDebitCardNumber_WithNonExistingNumber_ThenReturnNotFound() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.empty());

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.getDebitCardByDebitCardNumber(debitCardNumber);

        StepVerifier.create(debitCardResponseMono)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void whenGetDebitCardsByCardHolderId_WithExistingCardHolderId_ThenReturnDebitCards() {
        String cardHolderId = "100";
        List<DebitCard> debitCardList = new ArrayList<>();
        debitCardList.add(debitCard);
        when(debitCardRepository.findByCardHolderId(cardHolderId)).thenReturn(Flux.fromIterable(debitCardList));

        Flux<DebitCardResponse> debitCardResponseFlux = debitCardServiceImpl.getDebitCardsByCardHolderId(cardHolderId);

        StepVerifier.create(debitCardResponseFlux)
                .assertNext(debitCardResponse -> {
                    assertEquals(debitCard.getId(), debitCardResponse.getId());
                    assertEquals(debitCard.getCardHolderId(), debitCardResponse.getCardHolderId());
                    assertEquals(debitCard.getDebitCardNumber(), debitCardResponse.getDebitCardNumber());
                })
                .verifyComplete();
    }

    @Test
    void whenCreateDebitCard_WithValidRequest_ThenReturnCreatedDebitCard() {
        DebitCardRequest debitCardRequest = new DebitCardRequest();
        debitCardRequest.setCardHolderId("100");
        debitCardRequest.setLinkedAccountsNumber(List.of(mainAccountNumber, secondAccountNumber));
        debitCardRequest.setMainLinkedAccountNumber(mainAccountNumber);
        debitCardRequest.setDebitCardNumber(debitCardNumber);
        debitCardRequest.setCvv("123");
        debitCardRequest.setExpiryDate(LocalDate.now().plusYears(1));
        debitCardRequest.setStatus(DebitCardRequest.StatusEnum.ACTIVE);

        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(debitCard));

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.createDebitCard(debitCardRequest);

        StepVerifier.create(debitCardResponseMono)
                .assertNext(debitCardResponse -> {
                    assertEquals(debitCard.getId(), debitCardResponse.getId());
                    assertEquals(debitCard.getCardHolderId(), debitCardResponse.getCardHolderId());
                    assertEquals(debitCard.getDebitCardNumber(), debitCardResponse.getDebitCardNumber());
                })
                .verifyComplete();
    }

    @Test
    void whenUpdateDebitCardByDebitCardNumber_WithExistingNumber_ThenReturnUpdatedDebitCard() {
        DebitCardRequest debitCardRequest = new DebitCardRequest();
        debitCardRequest.setCardHolderId("200");
        debitCardRequest.setLinkedAccountsNumber(List.of(mainAccountNumber));
        debitCardRequest.setMainLinkedAccountNumber(mainAccountNumber);
        debitCardRequest.setDebitCardNumber("9999999999999999");
        debitCardRequest.setCvv("456");
        debitCardRequest.setExpiryDate(LocalDate.now().plusYears(2));
        debitCardRequest.setStatus(DebitCardRequest.StatusEnum.ACTIVE);

        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(debitCard));

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.updateDebitCardByDebitCardNumber(debitCardNumber, debitCardRequest);

        StepVerifier.create(debitCardResponseMono)
                .assertNext(debitCardResponse -> {
                    assertEquals(debitCard.getId(), debitCardResponse.getId());
                    assertEquals(debitCard.getCardHolderId(), debitCardResponse.getCardHolderId()); //Still the old value because you are mocking the save method
                    assertEquals(debitCard.getDebitCardNumber(), debitCardResponse.getDebitCardNumber()); //Still the old value because you are mocking the save method
                })
                .verifyComplete();
    }

    @Test
    void whenUpdateDebitCardByDebitCardNumber_WithNonExistingNumber_ThenReturnNotFound() {
        DebitCardRequest debitCardRequest = new DebitCardRequest();
        debitCardRequest.setCardHolderId("200");
        debitCardRequest.setLinkedAccountsNumber(List.of(mainAccountNumber));
        debitCardRequest.setMainLinkedAccountNumber(mainAccountNumber);
        debitCardRequest.setDebitCardNumber("9999999999999999");
        debitCardRequest.setCvv("456");
        debitCardRequest.setExpiryDate(LocalDate.now().plusYears(2));
        debitCardRequest.setStatus(DebitCardRequest.StatusEnum.ACTIVE);

        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.empty());

        Mono<DebitCardResponse> debitCardResponseMono = debitCardServiceImpl.updateDebitCardByDebitCardNumber(debitCardNumber, debitCardRequest);

        StepVerifier.create(debitCardResponseMono)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void whenDeleteDebitCardByDebitCardNumber_WithExistingNumber_ThenDeleteDebitCard() {
        when(debitCardRepository.deleteByDebitCardNumber(debitCardNumber)).thenReturn(Mono.empty().then());

        Mono<Void> voidMono = debitCardServiceImpl.deleteDebitCardByDebitCardNumber(debitCardNumber);

        StepVerifier.create(voidMono)
                .verifyComplete();
    }

    @Test
    void whenWithdrawalByDebitCard_DebitCardNotFound_ThenReturnError() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.empty());

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(BigDecimal.valueOf(100.0));

        Mono<BalanceResponse> balanceMono = debitCardServiceImpl.withdrawByDebitCardNumber(debitCardNumber, withdrawalRequest);

        StepVerifier.create(balanceMono)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void whenWithdrawalByDebitCard_AccountNotFound_ThenReturnError() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));
        when(accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())).thenReturn(Mono.empty());

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(BigDecimal.valueOf(100.0));

        Mono<BalanceResponse> balanceMono = debitCardServiceImpl.withdrawByDebitCardNumber(debitCardNumber, withdrawalRequest);

        StepVerifier.create(balanceMono)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void whenGetTransactionsByDebitCardNumberLast10_DebitCardNotFound_ThenReturnError() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.empty());

        Flux<TransactionResponse> transactionsFlux = debitCardServiceImpl.getTransactionsByDebitCardNumberLast10(debitCardNumber);

        StepVerifier.create(transactionsFlux)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void whenGetTransactionsByDebitCardNumberLast10_AccountNotFound_ThenReturnError() {
        when(debitCardRepository.findByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCard));
        when(accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())).thenReturn(Mono.empty());

        Flux<TransactionResponse> transactionsFlux = debitCardServiceImpl.getTransactionsByDebitCardNumberLast10(debitCardNumber);

        StepVerifier.create(transactionsFlux)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }
}
