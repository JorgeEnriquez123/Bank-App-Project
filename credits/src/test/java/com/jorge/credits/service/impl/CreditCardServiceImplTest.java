package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditCardMapper;
import com.jorge.credits.mapper.TransactionRequestMapper;
import com.jorge.credits.model.*;
import com.jorge.credits.repository.CreditCardRepository;
import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.client.TransactionClient;
import com.jorge.credits.webclient.model.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardServiceImplTest {
    @Mock
    private AccountClient accountClient;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private TransactionClient transactionClient;

    @Mock
    private CreditCardRepository creditCardRepository;
    @Spy
    private CreditCardMapper creditCardMapper;
    @Spy
    private TransactionRequestMapper transactionRequestMapper;
    @InjectMocks
    private CreditCardServiceImpl creditCardServiceImpl;

    String customerId = UUID.randomUUID().toString(); //Holder ID

    private String creditCardNumber;
    private CreditCard creditCard;

    private String debitCardNumber;
    private DebitCardResponse debitCardResponse;

    private String mainAccountNumber;
    private AccountResponse accountResponse;

    private String secondAccountNumber;
    private AccountResponse secondAccountResponse;


    private CreditPaymentByDebitCardRequest creditPaymentByDebitCardRequest;
    private AccountBalanceUpdateRequest accountBalanceUpdateRequestFromDebitCardPayment;


    @BeforeEach
    void setUp(){
        mainAccountNumber = "1234";
        secondAccountNumber = "5678";

        creditCardNumber = "1111222233334444";
        creditCard = CreditCard.builder()
                .id("1234")
                .cardHolderId("100")
                .creditCardNumber(creditCardNumber)
                .expiryDate(LocalDate.now().plusYears(5))
                .cvv("123")
                .type(CreditCard.CreditCardType.PERSONAL_CREDIT_CARD)
                .status(CreditCard.CreditCardStatus.ACTIVE)
                .creditLimit(BigDecimal.valueOf(5000.0))
                .availableBalance(BigDecimal.valueOf(4800.0))
                .outstandingBalance(BigDecimal.valueOf(200.0))
                .build();

        debitCardNumber = "1234123412341234";
        debitCardResponse = DebitCardResponse.builder()
                .id(UUID.randomUUID().toString())
                .cardHolderId(customerId)
                .debitCardNumber(debitCardNumber)
                .mainLinkedAccountNumber(mainAccountNumber)
                .linkedAccountsNumber(List.of(mainAccountNumber, secondAccountNumber))
                .expiryDate(LocalDate.now().plusYears(1))
                .cvv("123")
                .build();
    }

    @Test
    void whenGetCreditCardTransactions_ThenReturnTransactions() {
        List<CreditCardTransactionResponse> transactions = new ArrayList<>();
        CreditCardTransactionResponse creditCardTransactionResponse = new CreditCardTransactionResponse();
        creditCardTransactionResponse.setId("1");
        creditCardTransactionResponse.setCreditCardNumber(creditCardNumber);
        creditCardTransactionResponse.setTransactionType(CreditCardTransactionResponse.TransactionTypeEnum.CREDIT_CARD_CONSUMPTION);
        creditCardTransactionResponse.setAmount(BigDecimal.valueOf(200.0));
        transactions.add(creditCardTransactionResponse);

        when(creditCardRepository.findByCreditCardNumber(creditCardNumber)).thenReturn(Mono.just(creditCard));
        when(transactionClient.getCreditCardTransactionsByCreditCardNumberLast10(creditCardNumber)).thenReturn(Flux.fromIterable(transactions));

        StepVerifier.create(creditCardServiceImpl.getCreditCardTransactionsByCreditCardNumberLast10(creditCardNumber))
                .assertNext(transaction -> {
                    assertEquals(creditCardNumber, transaction.getCreditCardNumber());
                    assertEquals(CreditCardTransactionResponse.TransactionTypeEnum.CREDIT_CARD_CONSUMPTION, transaction.getTransactionType());
                    assertEquals(BigDecimal.valueOf(200.0), transaction.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void whenPayCreditCardByDebitCardNumber_WithExistingDebitCard_ThenReturnCreditCardResponse(){
        accountResponse = AccountResponse.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(mainAccountNumber)
                .accountType(AccountResponse.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(200.0))
                .customerId(customerId)
                .build();

        // Credit payment with existing Debit Card
        creditPaymentByDebitCardRequest = new CreditPaymentByDebitCardRequest();
        creditPaymentByDebitCardRequest.debitCardNumber(debitCardNumber);
        creditPaymentByDebitCardRequest.amount(BigDecimal.valueOf(100.0));
        creditPaymentByDebitCardRequest.creditType(CreditPaymentByDebitCardRequest.CreditTypeEnum.CREDIT_CARD_PAYMENT);

        accountBalanceUpdateRequestFromDebitCardPayment = new AccountBalanceUpdateRequest();
        accountBalanceUpdateRequestFromDebitCardPayment.setBalance(creditPaymentByDebitCardRequest.getAmount());

        when(creditCardRepository.findByCreditCardNumber(creditCardNumber)).thenReturn(Mono.just(creditCard));
        when(accountClient.getDebitCardByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCardResponse));
        when(accountClient.getAccountByAccountNumber(debitCardResponse.getMainLinkedAccountNumber()))
                .thenReturn(Mono.just(accountResponse));
        when(accountClient.reduceBalanceByAccountNumber(mainAccountNumber, accountBalanceUpdateRequestFromDebitCardPayment)).thenReturn(Mono.just(new AccountBalanceResponse()));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(Mono.just(creditCard));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse()));
        when(transactionClient.createCreditCardTransaction(any(CreditCardTransactionRequest.class)))
                .thenReturn(Mono.just(new CreditCardTransactionResponse()));

        StepVerifier.create(creditCardServiceImpl.payCreditCardWithDebitCard(creditCardNumber, creditPaymentByDebitCardRequest))
                .assertNext(response -> {
                    assertEquals(creditCard.getCreditLimit(), response.getCreditLimit());
                    assertEquals(creditCard.getAvailableBalance(), response.getAvailableBalance());
                    assertEquals(creditCard.getOutstandingBalance(), response.getOutstandingBalance());
                })
                .verifyComplete();
    }

    @Test
    void whenPayCreditCardByDebitCardNumber_MainAccountInsufficientBalance_ThenUseSecondAccount() {
        accountResponse = AccountResponse.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(mainAccountNumber)
                .accountType(AccountResponse.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(50.0))
                .customerId(customerId)
                .build();

        secondAccountResponse = AccountResponse.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(secondAccountNumber)
                .accountType(AccountResponse.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(1000.0))
                .customerId(customerId)
                .build();

        // Credit payment with existing Debit Card
        creditPaymentByDebitCardRequest = new CreditPaymentByDebitCardRequest();
        creditPaymentByDebitCardRequest.debitCardNumber(debitCardNumber);
        creditPaymentByDebitCardRequest.amount(BigDecimal.valueOf(100.0));
        creditPaymentByDebitCardRequest.creditType(CreditPaymentByDebitCardRequest.CreditTypeEnum.CREDIT_CARD_PAYMENT);

        accountBalanceUpdateRequestFromDebitCardPayment = new AccountBalanceUpdateRequest();
        accountBalanceUpdateRequestFromDebitCardPayment.setBalance(creditPaymentByDebitCardRequest.getAmount());

        when(creditCardRepository.findByCreditCardNumber(creditCardNumber)).thenReturn(Mono.just(creditCard));
        when(accountClient.getDebitCardByDebitCardNumber(debitCardNumber)).thenReturn(Mono.just(debitCardResponse));
        when(accountClient.getAccountByAccountNumber(mainAccountNumber)).thenReturn(Mono.just(accountResponse));
        when(accountClient.getAccountByAccountNumber(secondAccountNumber)).thenReturn(Mono.just(secondAccountResponse));

        when(accountClient.reduceBalanceByAccountNumber(secondAccountNumber, accountBalanceUpdateRequestFromDebitCardPayment))
                .thenReturn(Mono.just(new AccountBalanceResponse()));

        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(Mono.just(creditCard));
        when(transactionClient.createTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(new TransactionResponse()));
        when(transactionClient.createCreditCardTransaction(any(CreditCardTransactionRequest.class)))
                .thenReturn(Mono.just(new CreditCardTransactionResponse()));

        StepVerifier.create(creditCardServiceImpl.payCreditCardWithDebitCard(creditCardNumber, creditPaymentByDebitCardRequest))
                .assertNext(response -> {
                    assertEquals(creditCard.getCreditLimit(), response.getCreditLimit());
                    assertEquals(creditCard.getAvailableBalance(), response.getAvailableBalance());
                    assertEquals(creditCard.getOutstandingBalance(), response.getOutstandingBalance());
                })
                .verifyComplete();

        verify(accountClient).reduceBalanceByAccountNumber(secondAccountNumber, accountBalanceUpdateRequestFromDebitCardPayment);
    }
}
