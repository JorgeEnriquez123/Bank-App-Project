package com.jorge.credits.mapper;

import com.jorge.credits.model.CreditCard;
import com.jorge.credits.model.CreditCardRequest;
import com.jorge.credits.model.CreditCardResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreditCardMapper {
    public CreditCard mapToCreditCard(CreditCardRequest creditCardRequest) {
        CreditCard creditCard = new CreditCard();
        creditCard.setCardHolderId(creditCardRequest.getCardHolderId());
        creditCard.setType(CreditCard.CreditCardType.valueOf(creditCardRequest.getType().name()));
        creditCard.setCreditCardNumber(creditCardRequest.getCreditCardNumber());
        creditCard.setCvv(creditCardRequest.getCvv());
        creditCard.setExpiryDate(creditCardRequest.getExpiryDate());
        creditCard.setStatus(CreditCard.CreditCardStatus.valueOf(creditCardRequest.getStatus().name()));
        creditCard.setCreditLimit(creditCardRequest.getCreditLimit());
        creditCard.setAvailableBalance(creditCardRequest.getAvailableBalance());
        creditCard.setOutstandingBalance(creditCardRequest.getOutstandingBalance());
        creditCard.setCreatedAt(LocalDateTime.now());
        return creditCard;
    }

    public CreditCardResponse mapToCreditCardResponse(CreditCard creditCard) {
        CreditCardResponse creditCardResponse = new CreditCardResponse();
        creditCardResponse.setId(creditCard.getId());
        creditCardResponse.setCardHolderId(creditCard.getCardHolderId());
        creditCardResponse.setType(CreditCardResponse.TypeEnum.valueOf(creditCard.getType().name()));
        creditCardResponse.setCreditCardNumber(creditCard.getCreditCardNumber());
        creditCardResponse.setCvv(creditCard.getCvv());
        creditCardResponse.setExpiryDate(creditCard.getExpiryDate());
        creditCardResponse.setStatus(CreditCardResponse.StatusEnum.valueOf(creditCard.getStatus().name()));
        creditCardResponse.setCreditLimit(creditCard.getCreditLimit());
        creditCardResponse.setAvailableBalance(creditCard.getAvailableBalance());
        creditCardResponse.setOutstandingBalance(creditCard.getOutstandingBalance());
        creditCardResponse.setCreatedAt(creditCard.getCreatedAt());
        return creditCardResponse;
    }
}
