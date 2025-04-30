package com.jorge.transactions.service.impl;

import com.jorge.transactions.mapper.CreditCardTransactionMapper;
import com.jorge.transactions.model.CreditCardTransaction;
import com.jorge.transactions.model.CreditCardTransactionRequest;
import com.jorge.transactions.model.CreditCardTransactionResponse;
import com.jorge.transactions.repository.CreditCardTransactionRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreditCardTransactionServiceImplTest {
    @Mock
    private CreditCardTransactionRepository creditCardTransactionRepository;

    @Spy
    private CreditCardTransactionMapper creditCardTransactionMapper;

    @InjectMocks
    private CreditCardTransactionServiceImpl creditCardTransactionServiceImpl;

    private CreditCardTransaction creditCardTransaction;
    private CreditCardTransactionRequest creditCardTransactionRequest;
    private CreditCardTransactionResponse creditCardTransactionResponse;

    @BeforeEach
    void setUp() {
        creditCardTransaction = CreditCardTransaction.builder()
                .id(UUID.randomUUID().toString())
                .creditCardNumber("1111222233334444")
                .transactionType(CreditCardTransaction.CreditCardTransactionType.CREDIT_CARD_CONSUMPTION)
                .amount(BigDecimal.valueOf(50.0))
                .createdAt(LocalDateTime.now())
                .build();

        creditCardTransactionRequest = new CreditCardTransactionRequest();
        creditCardTransactionRequest.setCreditCardNumber("1111222233334444");
        creditCardTransactionRequest.setTransactionType(CreditCardTransactionRequest.TransactionTypeEnum.CREDIT_CARD_CONSUMPTION);
        creditCardTransactionRequest.setAmount(BigDecimal.valueOf(50.0));

        creditCardTransactionResponse = new CreditCardTransactionResponse();
        creditCardTransactionResponse.setId(creditCardTransaction.getId());
        creditCardTransactionResponse.setCreditCardNumber(creditCardTransaction.getCreditCardNumber());
        creditCardTransactionResponse.setTransactionType(CreditCardTransactionResponse.TransactionTypeEnum.CREDIT_CARD_CONSUMPTION);
        creditCardTransactionResponse.setAmount(creditCardTransaction.getAmount());
        creditCardTransactionResponse.setCreatedAt(creditCardTransaction.getCreatedAt());
    }

    @Test
    void whenGetAllCreditCardTransactions_ThenReturnFluxOfCreditCardTransactionResponse() {
        when(creditCardTransactionRepository.findAll()).thenReturn(Flux.just(creditCardTransaction));

        Flux<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.getAllCreditCardTransactions();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(creditCardTransactionResponse.getId(), response.getId());
                    assertEquals(creditCardTransactionResponse.getCreditCardNumber(), response.getCreditCardNumber());
                    assertEquals(creditCardTransactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(creditCardTransactionResponse.getAmount(), response.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void whenCreateCreditCardTransaction_ThenReturnMonoOfCreditCardTransactionResponse() {
        when(creditCardTransactionRepository.save(any(CreditCardTransaction.class))).thenReturn(Mono.just(creditCardTransaction));

        Mono<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.createCreditCardTransaction(creditCardTransactionRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(creditCardTransactionResponse.getId(), response.getId());
                    assertEquals(creditCardTransactionResponse.getCreditCardNumber(), response.getCreditCardNumber());
                    assertEquals(creditCardTransactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(creditCardTransactionResponse.getAmount(), response.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void whenGetCreditCardTransactionById_WithExistingId_ThenReturnMonoOfCreditCardTransactionResponse() {
        when(creditCardTransactionRepository.findById(anyString())).thenReturn(Mono.just(creditCardTransaction));

        Mono<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.getCreditCardTransactionById("someId");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(creditCardTransactionResponse.getId(), response.getId());
                    assertEquals(creditCardTransactionResponse.getCreditCardNumber(), response.getCreditCardNumber());
                    assertEquals(creditCardTransactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(creditCardTransactionResponse.getAmount(), response.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void whenGetCreditCardTransactionById_WithNonExistingId_ThenReturnNotFoundException() {
        when(creditCardTransactionRepository.findById(anyString())).thenReturn(Mono.empty());

        Mono<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.getCreditCardTransactionById("nonExistingId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Credit card transaction with id: nonExistingId not found"))
                .verify();
    }

    @Test
    void whenUpdateCreditCardTransaction_WithExistingId_ThenReturnMonoOfCreditCardTransactionResponse() {
        CreditCardTransaction updatedTransaction = CreditCardTransaction.builder()
                .id(creditCardTransaction.getId())
                .creditCardNumber("1111222233334444")
                .transactionType(CreditCardTransaction.CreditCardTransactionType.CREDIT_CARD_PAYMENT)
                .amount(BigDecimal.valueOf(100.0))
                .createdAt(creditCardTransaction.getCreatedAt())
                .build();

        CreditCardTransactionRequest updateRequest = new CreditCardTransactionRequest();
        updateRequest.setCreditCardNumber("1111222233334444");
        updateRequest.setTransactionType(CreditCardTransactionRequest.TransactionTypeEnum.CREDIT_CARD_PAYMENT);
        updateRequest.setAmount(BigDecimal.valueOf(100.0));

        CreditCardTransactionResponse updatedResponse = new CreditCardTransactionResponse();
        updatedResponse.setId(updatedTransaction.getId());
        updatedResponse.setCreditCardNumber(updatedTransaction.getCreditCardNumber());
        updatedResponse.setTransactionType(CreditCardTransactionResponse.TransactionTypeEnum.CREDIT_CARD_PAYMENT);
        updatedResponse.setAmount(updatedTransaction.getAmount());
        updatedResponse.setCreatedAt(updatedTransaction.getCreatedAt());

        when(creditCardTransactionRepository.findById(anyString())).thenReturn(Mono.just(creditCardTransaction));
        when(creditCardTransactionRepository.save(any(CreditCardTransaction.class))).thenReturn(Mono.just(updatedTransaction));

        Mono<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.updateCreditCardTransaction("someId", updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(updatedResponse.getId(), response.getId());
                    assertEquals(updatedResponse.getCreditCardNumber(), response.getCreditCardNumber());
                    assertEquals(updatedResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(updatedResponse.getAmount(), response.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void whenUpdateCreditCardTransaction_WithNonExistingId_ThenReturnNotFoundException() {
        when(creditCardTransactionRepository.findById(anyString())).thenReturn(Mono.empty());

        Mono<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.updateCreditCardTransaction("nonExistingId", creditCardTransactionRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Credit card transaction with id: nonExistingId not found"))
                .verify();
    }

    @Test
    void whenDeleteCreditCardTransactionById_ThenReturnMonoVoid() {
        when(creditCardTransactionRepository.deleteById(anyString())).thenReturn(Mono.empty());

        Mono<Void> result = creditCardTransactionServiceImpl.deleteCreditCardTransactionById("someId");

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void whenGetCreditCardTransactionsByCreditCardNumber_ThenReturnFluxOfCreditCardTransactionResponseSorted() {
        CreditCardTransaction transaction1 = CreditCardTransaction.builder()
                .id(UUID.randomUUID().toString())
                .creditCardNumber("1111222233334444")
                .transactionType(CreditCardTransaction.CreditCardTransactionType.CREDIT_CARD_CONSUMPTION)
                .amount(BigDecimal.valueOf(20.0))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        CreditCardTransaction transaction2 = CreditCardTransaction.builder()
                .id(UUID.randomUUID().toString())
                .creditCardNumber("1111222233334444")
                .transactionType(CreditCardTransaction.CreditCardTransactionType.CREDIT_CARD_CONSUMPTION)
                .amount(BigDecimal.valueOf(50.0))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(creditCardTransactionRepository.findByCreditCardNumber(anyString())).thenReturn(Flux.just(transaction1, transaction2));

        Flux<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.getCreditCardTransactionsByCreditCardNumber("1111222233334444");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transaction2.getId(), response.getId()); // Expecting the later transaction first
                    assertEquals(transaction2.getCreditCardNumber(), response.getCreditCardNumber());
                    assertEquals(CreditCardTransactionResponse.TransactionTypeEnum.CREDIT_CARD_CONSUMPTION, response.getTransactionType());
                    assertEquals(transaction2.getAmount(), response.getAmount());
                })
                .assertNext(response -> {
                    assertEquals(transaction1.getId(), response.getId()); // Expecting the earlier transaction second
                    assertEquals(transaction1.getCreditCardNumber(), response.getCreditCardNumber());
                    assertEquals(CreditCardTransactionResponse.TransactionTypeEnum.CREDIT_CARD_CONSUMPTION, response.getTransactionType());
                    assertEquals(transaction1.getAmount(), response.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void whenGetCreditCardTransactionsByCreditCardNumberLast10_ThenReturnFluxOfLast10CreditCardTransactionResponse() {
        List<CreditCardTransaction> transactions = new ArrayList<>();
        // Create more than 10 transactions to test the take(10) functionality
        for (int i = 0; i < 15; i++) {
            transactions.add(CreditCardTransaction.builder()
                    .id(UUID.randomUUID().toString())
                    .creditCardNumber("1111222233334444")
                    .transactionType(CreditCardTransaction.CreditCardTransactionType.CREDIT_CARD_CONSUMPTION)
                    .amount(BigDecimal.valueOf(10.0 + i))
                    .createdAt(LocalDateTime.now().minusDays(14 - i)) // Still good practice to have some ordering
                    .build());
        }

        when(creditCardTransactionRepository.findByCreditCardNumber(anyString())).thenReturn(Flux.fromIterable(transactions));

        Flux<CreditCardTransactionResponse> result = creditCardTransactionServiceImpl.getCreditCardTransactionsByCreditCardNumberLast10("1111222233334444");

        StepVerifier.create(result)
                .expectNextCount(10) // Verify that exactly 10 items are emitted
                .verifyComplete(); // Verify that the Flux completes after emitting 10 items
    }
}
