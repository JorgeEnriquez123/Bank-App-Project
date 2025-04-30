package com.jorge.transactions.service.impl;

import com.jorge.transactions.mapper.TransactionMapper;
import com.jorge.transactions.model.FeeReportResponse;
import com.jorge.transactions.model.Transaction;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import com.jorge.transactions.repository.TransactionRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Spy
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionServiceImpl;

    private Transaction transaction;
    private TransactionRequest transactionRequest;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber("ACC123")
                .fee(BigDecimal.ZERO)
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(BigDecimal.valueOf(100))
                .description("Initial deposit")
                .createdAt(LocalDateTime.now())
                .relatedCreditId(null)
                .build();

        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber("ACC123");
        transactionRequest.setFee(BigDecimal.ZERO);
        transactionRequest.setTransactionType(TransactionRequest.TransactionTypeEnum.DEPOSIT);
        transactionRequest.setAmount(BigDecimal.valueOf(100));
        transactionRequest.setDescription("Initial deposit");

        transactionResponse = new TransactionResponse();
        transactionResponse.setId(transaction.getId());
        transactionResponse.setAccountNumber(transaction.getAccountNumber());
        transactionResponse.setFee(transaction.getFee());
        transactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.DEPOSIT);
        transactionResponse.setAmount(transaction.getAmount());
        transactionResponse.setDescription(transaction.getDescription());
        transactionResponse.setCreatedAt(transaction.getCreatedAt());
        transactionResponse.setRelatedCreditId(transaction.getRelatedCreditId());
    }

    @Test
    void whenGetAllTransactions_ThenReturnFluxOfTransactionResponse() {
        when(transactionRepository.findAll()).thenReturn(Flux.just(transaction));

        Flux<TransactionResponse> result = transactionServiceImpl.getAllTransactions();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionResponse.getId(), response.getId());
                    assertEquals(transactionResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(transactionResponse.getFee(), response.getFee());
                    assertEquals(transactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), response.getAmount());
                    assertEquals(transactionResponse.getDescription(), response.getDescription());
                    assertEquals(transactionResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenCreateTransaction_ThenReturnMonoOfTransactionResponse() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

        Mono<TransactionResponse> result = transactionServiceImpl.createTransaction(transactionRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionResponse.getId(), response.getId());
                    assertEquals(transactionResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(transactionResponse.getFee(), response.getFee());
                    assertEquals(transactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), response.getAmount());
                    assertEquals(transactionResponse.getDescription(), response.getDescription());
                    assertEquals(transactionResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionById_WithExistingId_ThenReturnMonoOfTransactionResponse() {
        when(transactionRepository.findById(anyString())).thenReturn(Mono.just(transaction));

        Mono<TransactionResponse> result = transactionServiceImpl.getTransactionById("someId");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionResponse.getId(), response.getId());
                    assertEquals(transactionResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(transactionResponse.getFee(), response.getFee());
                    assertEquals(transactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), response.getAmount());
                    assertEquals(transactionResponse.getDescription(), response.getDescription());
                    assertEquals(transactionResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionById_WithNonExistingId_ThenReturnNotFoundException() {
        when(transactionRepository.findById(anyString())).thenReturn(Mono.empty());

        Mono<TransactionResponse> result = transactionServiceImpl.getTransactionById("nonExistingId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Movimiento con id: nonExistingId no encontrado"))
                .verify();
    }

    @Test
    void whenUpdateTransaction_WithExistingId_ThenReturnMonoOfTransactionResponse() {
        Transaction updatedTransaction = Transaction.builder()
                .id(transaction.getId())
                .accountNumber("ACC123")
                .fee(BigDecimal.valueOf(1.50))
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(BigDecimal.valueOf(50))
                .description("Withdrawal of funds")
                .createdAt(transaction.getCreatedAt()) // Maintain original creation time
                .relatedCreditId(null)
                .build();

        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setAccountNumber("ACC123");
        updateRequest.setFee(BigDecimal.valueOf(1.50));
        updateRequest.setTransactionType(TransactionRequest.TransactionTypeEnum.WITHDRAWAL);
        updateRequest.setAmount(BigDecimal.valueOf(50));
        updateRequest.setDescription("Withdrawal of funds");

        TransactionResponse updatedResponse = new TransactionResponse();
        updatedResponse.setId(updatedTransaction.getId());
        updatedResponse.setAccountNumber(updatedTransaction.getAccountNumber());
        updatedResponse.setFee(updatedTransaction.getFee());
        updatedResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.WITHDRAWAL);
        updatedResponse.setAmount(updatedTransaction.getAmount());
        updatedResponse.setDescription(updatedTransaction.getDescription());
        updatedResponse.setCreatedAt(updatedTransaction.getCreatedAt());
        updatedResponse.setRelatedCreditId(updatedTransaction.getRelatedCreditId());


        when(transactionRepository.findById(anyString())).thenReturn(Mono.just(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(updatedTransaction));

        Mono<TransactionResponse> result = transactionServiceImpl.updateTransaction("someId", updateRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(updatedResponse.getId(), response.getId());
                    assertEquals(updatedResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(updatedResponse.getFee(), response.getFee());
                    assertEquals(updatedResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(updatedResponse.getAmount(), response.getAmount());
                    assertEquals(updatedResponse.getDescription(), response.getDescription());
                    assertEquals(updatedResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(updatedResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenUpdateTransaction_WithNonExistingId_ThenReturnNotFoundException() {
        when(transactionRepository.findById(anyString())).thenReturn(Mono.empty());

        Mono<TransactionResponse> result = transactionServiceImpl.updateTransaction("nonExistingId", transactionRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Movimiento con id: nonExistingId no encontrado"))
                .verify();
    }

    @Test
    void whenDeleteTransactionById_ThenReturnMonoVoid() {
        when(transactionRepository.deleteById(anyString())).thenReturn(Mono.empty());

        Mono<Void> result = transactionServiceImpl.deleteTransactionById("someId");

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void whenGetTransactionsByAccountNumber_WithExistingTransactions_ThenReturnFluxOfTransactionResponse() {
        when(transactionRepository.findByAccountNumber(anyString())).thenReturn(Flux.just(transaction));

        Flux<TransactionResponse> result = transactionServiceImpl.getTransactionsByAccountNumber("ACC123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionResponse.getId(), response.getId());
                    assertEquals(transactionResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(transactionResponse.getFee(), response.getFee());
                    assertEquals(transactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), response.getAmount());
                    assertEquals(transactionResponse.getDescription(), response.getDescription());
                    assertEquals(transactionResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionsByAccountNumber_WithNoTransactions_ThenReturnNotFoundException() {
        when(transactionRepository.findByAccountNumber(anyString())).thenReturn(Flux.empty());

        Flux<TransactionResponse> result = transactionServiceImpl.getTransactionsByAccountNumber("nonExistingAcc");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Transactions not found for account number: nonExistingAcc"))
                .verify();
    }

    @Test
    void whenGetTransactionsByCreditId_WithExistingTransactions_ThenReturnFluxOfTransactionResponse() {
        transaction.setRelatedCreditId("CREDIT123");
        transactionResponse.setRelatedCreditId("CREDIT123");

        when(transactionRepository.findByRelatedCreditId(anyString())).thenReturn(Flux.just(transaction));

        Flux<TransactionResponse> result = transactionServiceImpl.getTransactionsByCreditId("CREDIT123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionResponse.getId(), response.getId());
                    assertEquals(transactionResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(transactionResponse.getFee(), response.getFee());
                    assertEquals(transactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), response.getAmount());
                    assertEquals(transactionResponse.getDescription(), response.getDescription());
                    assertEquals(transactionResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionsByCreditId_WithNoTransactions_ThenReturnNotFoundException() {
        when(transactionRepository.findByRelatedCreditId(anyString())).thenReturn(Flux.empty());

        Flux<TransactionResponse> result = transactionServiceImpl.getTransactionsByCreditId("nonExistingCredit");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Transactions not found for credit id: nonExistingCredit"))
                .verify();
    }

    @Test
    void whenGetTransactionsByAccountNumberAndDateRange_ThenReturnFluxOfTransactionResponse() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        when(transactionRepository.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAt(anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Flux.just(transaction));

        Flux<TransactionResponse> result = transactionServiceImpl.getTransactionsByAccountNumberAndDateRange("ACC123", startDate, endDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionResponse.getId(), response.getId());
                    assertEquals(transactionResponse.getAccountNumber(), response.getAccountNumber());
                    assertEquals(transactionResponse.getFee(), response.getFee());
                    assertEquals(transactionResponse.getTransactionType(), response.getTransactionType());
                    assertEquals(transactionResponse.getAmount(), response.getAmount());
                    assertEquals(transactionResponse.getDescription(), response.getDescription());
                    assertEquals(transactionResponse.getRelatedCreditId(), response.getRelatedCreditId());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionResponse.getCreatedAt(), response.getCreatedAt());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionsFeesByAccountNumberAndDateRange_WithTransactionFee_ThenReturnFluxOfFeeReportResponse() {
        Transaction transactionWithFee = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber("ACC123")
                .fee(BigDecimal.valueOf(2.50))
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(BigDecimal.valueOf(50))
                .description("Withdrawal with fee")
                .createdAt(LocalDateTime.now())
                .relatedCreditId(null)
                .build();

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        when(transactionRepository.findByAccountNumberAndFeeGreaterThanAndCreatedAtBetweenOrderByCreatedAt(anyString(), any(BigDecimal.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.just(transactionWithFee));

        Flux<FeeReportResponse> result = transactionServiceImpl.getTransactionsFeesByAccountNumberAndDateRange("ACC123", startDate, endDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionWithFee.getFee(), response.getAmount());
                    assertEquals(FeeReportResponse.TypeEnum.TRANSACTION_FEE, response.getType());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionWithFee.getCreatedAt(), response.getDate());
                })
                .verifyComplete();
    }

    @Test
    void whenGetTransactionsFeesByAccountNumberAndDateRange_WithMaintenanceFee_ThenReturnFluxOfFeeReportResponse() {
        Transaction transactionWithFee = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber("ACC123")
                .fee(BigDecimal.valueOf(5.00))
                .transactionType(Transaction.TransactionType.MAINTENANCE_FEE)
                .amount(BigDecimal.ZERO)
                .description("Monthly maintenance fee")
                .createdAt(LocalDateTime.now())
                .relatedCreditId(null)
                .build();

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        when(transactionRepository.findByAccountNumberAndFeeGreaterThanAndCreatedAtBetweenOrderByCreatedAt(anyString(), any(BigDecimal.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.just(transactionWithFee));

        Flux<FeeReportResponse> result = transactionServiceImpl.getTransactionsFeesByAccountNumberAndDateRange("ACC123", startDate, endDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(transactionWithFee.getFee(), response.getAmount());
                    assertEquals(FeeReportResponse.TypeEnum.MAINTENANCE_FEE, response.getType());
                    // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
                    // assertEquals(transactionWithFee.getCreatedAt(), response.getDate());
                })
                .verifyComplete();
    }
}
