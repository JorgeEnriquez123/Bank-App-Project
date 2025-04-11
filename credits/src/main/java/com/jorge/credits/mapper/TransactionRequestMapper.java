package com.jorge.credits.mapper;

import com.jorge.credits.model.ConsumptionRequest;
import com.jorge.credits.model.CreditPaymentByDebitCardRequest;
import com.jorge.credits.model.CreditPaymentRequest;
import com.jorge.credits.webclient.model.CreditCardTransactionRequest;
import com.jorge.credits.webclient.model.TransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransactionRequestMapper {
    public TransactionRequest mapPaymentRequestToTransactionRequest(CreditPaymentRequest creditPaymentRequest,
                                                                    String savedCreditId,
                                                                    String description,
                                                                    BigDecimal fee) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(creditPaymentRequest.getAccountNumber());
        transactionRequest.setRelatedCreditId(savedCreditId);
        transactionRequest.setAmount(creditPaymentRequest.getAmount());
        transactionRequest.setTransactionType(TransactionRequest.TransactionType.valueOf(creditPaymentRequest.getCreditType().name()));
        transactionRequest.setDescription(description);
        transactionRequest.setFee(fee);
        return transactionRequest;
    }

    public TransactionRequest mapDebitCardPaymentRequestToTransactionRequest(String accountNumber,
                                                                            CreditPaymentByDebitCardRequest creditPaymentByDebitCardRequest,
                                                                    String savedCreditId,
                                                                    String description,
                                                                    BigDecimal fee) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(accountNumber);
        transactionRequest.setRelatedCreditId(savedCreditId);
        transactionRequest.setAmount(creditPaymentByDebitCardRequest.getAmount());
        transactionRequest.setTransactionType(TransactionRequest.TransactionType.valueOf(creditPaymentByDebitCardRequest.getCreditType().name()));
        transactionRequest.setDescription(description);
        transactionRequest.setFee(fee);
        return transactionRequest;
    }

    public CreditCardTransactionRequest mapPaymentRequestToCreditCardTransactionRequest(CreditPaymentRequest creditPaymentRequest,
                                                                                        String creditCardNumber) {
        CreditCardTransactionRequest creditCardTransactionRequest = new CreditCardTransactionRequest();
        creditCardTransactionRequest.setCreditCardNumber(creditCardNumber);
        creditCardTransactionRequest.setTransactionType(CreditCardTransactionRequest.CreditCardTransactionType.valueOf(
                creditPaymentRequest.getCreditType().name()));
        creditCardTransactionRequest.setAmount(creditPaymentRequest.getAmount());
        creditCardTransactionRequest.setCreatedAt(LocalDateTime.now());
        return creditCardTransactionRequest;
    }

    public CreditCardTransactionRequest mapPaymentDebitCardRequestToCreditCardTransactionRequest(CreditPaymentByDebitCardRequest
                                                                                                             creditPaymentRequestWithDebitCard,
                                                                                                 String creditCardNumber) {
        CreditCardTransactionRequest creditCardTransactionRequest = new CreditCardTransactionRequest();
        creditCardTransactionRequest.setCreditCardNumber(creditCardNumber);
        creditCardTransactionRequest.setTransactionType(CreditCardTransactionRequest.CreditCardTransactionType.valueOf(
                creditPaymentRequestWithDebitCard.getCreditType().name()));
        creditCardTransactionRequest.setAmount(creditPaymentRequestWithDebitCard.getAmount());
        creditCardTransactionRequest.setCreatedAt(LocalDateTime.now());
        return creditCardTransactionRequest;
    }

    public CreditCardTransactionRequest mapConsumeRequestToTransactionRequest(ConsumptionRequest creditPaymentRequest, String creditCardNumber) {
        CreditCardTransactionRequest creditCardTransactionRequest = new CreditCardTransactionRequest();
        creditCardTransactionRequest.setCreditCardNumber(creditCardNumber);
        creditCardTransactionRequest.setTransactionType(CreditCardTransactionRequest.CreditCardTransactionType.CREDIT_CARD_CONSUMPTION);
        creditCardTransactionRequest.setAmount(creditPaymentRequest.getAmount());
        creditCardTransactionRequest.setCreatedAt(LocalDateTime.now());
        return creditCardTransactionRequest;
    }
}
