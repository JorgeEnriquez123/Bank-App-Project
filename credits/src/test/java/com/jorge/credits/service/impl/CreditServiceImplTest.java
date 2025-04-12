package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditMapper;
import com.jorge.credits.mapper.TransactionRequestMapper;
import com.jorge.credits.model.*;
import com.jorge.credits.repository.CreditRepository;
import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.TransactionClient;
import com.jorge.credits.webclient.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreditServiceImplTest {
    @Mock
    private AccountClient accountClient;
    @Mock
    private TransactionClient transactionClient;
    @Spy
    private CreditMapper creditMapper;
    @Mock
    private CreditRepository creditRepository;
    @Spy
    private TransactionRequestMapper transactionRequestMapper;
    @InjectMocks
    private CreditServiceImpl creditServiceImpl;

    private String creditId;
    private Credit credit;

    private String debitCardNumber;
    private DebitCardResponse debitCardResponse;

    private String accountNumber;
    private AccountResponse accountResponse;

    private CreditPaymentByDebitCardRequest creditPaymentByDebitCardRequest;
    private CreditPaymentRequest creditPaymentRequest;

    private AccountBalanceUpdateRequest accountBalanceUpdateRequestFromDebitCardPayment;
    private AccountBalanceUpdateRequest accountBalanceUpdateRequestFromAccountNumberPayment;

    @BeforeEach
    void setUp(){
        String customerId = UUID.randomUUID().toString();

        creditId = UUID.randomUUID().toString();
        credit = Credit.builder()
                .id(creditId)
                .creditHolderId(customerId)
                .creditType(Credit.CreditType.PERSONAL)
                .status(Credit.Status.ACTIVE)
                .creditAmount(BigDecimal.valueOf(1000.0))
                .createdAt(LocalDateTime.now())
                .dueDate(LocalDate.now().plusYears(1))
                .build();

        accountNumber = UUID.randomUUID().toString();
        accountResponse = AccountResponse.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .accountType(AccountResponse.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000.0))
                .customerId(customerId)
                .build();

        debitCardNumber = "1234123412341234";
        debitCardResponse = DebitCardResponse.builder()
                .id(UUID.randomUUID().toString())
                .cardHolderId(customerId)
                .debitCardNumber(debitCardNumber)
                .mainLinkedAccountNumber(accountNumber)
                .linkedAccountsNumber(List.of(accountNumber, "1234567890"))
                .expiryDate(LocalDate.now().plusYears(1))
                .cvv("123")
                .build();

        // Credit payment with existing Debit Card
        creditPaymentByDebitCardRequest = new CreditPaymentByDebitCardRequest();
        creditPaymentByDebitCardRequest.debitCardNumber(debitCardNumber);
        creditPaymentByDebitCardRequest.amount(BigDecimal.valueOf(100.0));
        creditPaymentByDebitCardRequest.creditType(CreditPaymentByDebitCardRequest.CreditTypeEnum.CREDIT_PAYMENT);

        accountBalanceUpdateRequestFromDebitCardPayment = new AccountBalanceUpdateRequest();
        accountBalanceUpdateRequestFromDebitCardPayment.setBalance(creditPaymentByDebitCardRequest.getAmount());

        // Credit payment with external Account Number
        creditPaymentRequest = new CreditPaymentRequest();
        creditPaymentRequest.setAccountNumber(accountNumber);
        creditPaymentRequest.amount(BigDecimal.valueOf(50.0));
        creditPaymentRequest.setCreditType(CreditPaymentRequest.CreditTypeEnum.CREDIT_PAYMENT);

        accountBalanceUpdateRequestFromAccountNumberPayment = new AccountBalanceUpdateRequest();
        accountBalanceUpdateRequestFromAccountNumberPayment.setBalance(creditPaymentRequest.getAmount());

    }

    @Test
    void whenPayCreditByIdWithDebitCard_WithExistingDebitCard_ThenReturnCreditResponse(){
        when(creditRepository.findById(creditId)).thenReturn(Mono.just(credit));
        when(accountClient.getDebitCardByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCardResponse));
        when(accountClient.getAccountByAccountNumber(accountNumber)).thenReturn(Mono.just(accountResponse));
        when(accountClient.reduceBalanceByAccountNumber(accountNumber, accountBalanceUpdateRequestFromDebitCardPayment)).thenReturn(Mono.just(new AccountBalanceResponse()));
        when(creditRepository.save(any(Credit.class))).thenReturn(Mono.just(credit));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse()));

        Mono<CreditResponse> creditResponseMono = creditServiceImpl.payCreditByIdWithDebitCard(creditId, creditPaymentByDebitCardRequest);

        StepVerifier.create(creditResponseMono)
                .assertNext(creditResponse -> {
                    Assertions.assertEquals(creditId, creditResponse.getId());
                    Assertions.assertEquals(credit.getCreditHolderId(), creditResponse.getCreditHolderId());
                    Assertions.assertEquals(credit.getCreditType().name(), creditResponse.getCreditType().name());
                    Assertions.assertEquals(credit.getStatus().name(), creditResponse.getStatus().name());
                    Assertions.assertEquals(credit.getCreditAmount(), creditResponse.getCreditAmount());
                    Assertions.assertEquals(credit.getCreatedAt(), creditResponse.getCreatedAt());
                    Assertions.assertEquals(credit.getDueDate(), creditResponse.getDueDate());}
                )
                .verifyComplete();

        Mockito.verify(creditRepository).findById(creditId);
        Mockito.verify(accountClient).getDebitCardByDebitCardNumber(debitCardNumber);
        Mockito.verify(accountClient).getAccountByAccountNumber(accountNumber);
        Mockito.verify(accountClient).reduceBalanceByAccountNumber(accountNumber, accountBalanceUpdateRequestFromDebitCardPayment);
        Mockito.verify(creditRepository).save(any(Credit.class));
        Mockito.verify(transactionClient).createTransaction(any(TransactionRequest.class));
    }

    @Test
    void whenPayCreditById_WithAccountNumber_ThenReturnCreditResponse(){
        when(creditRepository.findById(creditId)).thenReturn(Mono.just(credit));
        when(accountClient.reduceBalanceByAccountNumber(accountNumber, accountBalanceUpdateRequestFromAccountNumberPayment))
                .thenReturn(Mono.just(new AccountBalanceResponse()));
        when(creditRepository.save(any(Credit.class))).thenReturn(Mono.just(credit));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse()));

        Mono<CreditResponse> creditResponseMono = creditServiceImpl.payCreditById(creditId, creditPaymentRequest);

        StepVerifier.create(creditResponseMono)
                .assertNext(creditResponse -> {
                    Assertions.assertEquals(creditId, creditResponse.getId());
                    Assertions.assertEquals(credit.getCreditHolderId(), creditResponse.getCreditHolderId());
                    Assertions.assertEquals(credit.getCreditType().name(), creditResponse.getCreditType().name());
                    Assertions.assertEquals(credit.getStatus().name(), creditResponse.getStatus().name());
                    Assertions.assertEquals(credit.getCreditAmount(), creditResponse.getCreditAmount());
                    Assertions.assertEquals(credit.getCreatedAt(), creditResponse.getCreatedAt());
                })
                .verifyComplete();

        Mockito.verify(creditRepository).findById(creditId);
        Mockito.verify(accountClient).reduceBalanceByAccountNumber(accountNumber, accountBalanceUpdateRequestFromAccountNumberPayment);
        Mockito.verify(creditRepository).save(any(Credit.class));
        Mockito.verify(transactionClient).createTransaction(any(TransactionRequest.class));
    }
}
