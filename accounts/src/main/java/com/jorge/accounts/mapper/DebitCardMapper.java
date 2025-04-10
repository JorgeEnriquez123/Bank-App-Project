package com.jorge.accounts.mapper;

import com.jorge.accounts.model.DebitCard;
import com.jorge.accounts.model.DebitCardRequest;
import com.jorge.accounts.model.DebitCardResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DebitCardMapper {
    public DebitCard mapToDebitCard(DebitCardRequest debitCardRequest) {
        DebitCard debitCard = new DebitCard();
        debitCard.setCardHolderId(debitCardRequest.getCardHolderId());
        debitCard.setLinkedAccountsNumber(debitCardRequest.getLinkedAccountsNumber());
        debitCard.setMainLinkedAccountNumber(debitCardRequest.getMainLinkedAccountNumber());
        debitCard.setDebitCardNumber(debitCardRequest.getDebitCardNumber());
        debitCard.setCvv(debitCardRequest.getCvv());
        debitCard.setExpiryDate(debitCardRequest.getExpiryDate());
        debitCard.setStatus(DebitCard.DebitCardStatus.valueOf(debitCardRequest.getStatus().name()));
        debitCard.setCreatedAt(LocalDateTime.now());
        return debitCard;
    }

    public DebitCardResponse mapToDebitCardResponse(DebitCard debitCard) {
        DebitCardResponse debitCardResponse = new DebitCardResponse();
        debitCardResponse.setId(debitCard.getId());
        debitCardResponse.setCardHolderId(debitCard.getCardHolderId());
        debitCardResponse.setLinkedAccountsNumber(debitCard.getLinkedAccountsNumber());
        debitCardResponse.setMainLinkedAccountNumber(debitCard.getMainLinkedAccountNumber());
        debitCardResponse.setDebitCardNumber(debitCard.getDebitCardNumber());
        debitCardResponse.setCvv(debitCard.getCvv());
        debitCardResponse.setExpiryDate(debitCard.getExpiryDate());
        debitCardResponse.setStatus(DebitCardResponse.StatusEnum.valueOf(debitCard.getStatus().name()));
        debitCardResponse.setCreatedAt(debitCard.getCreatedAt());
        return debitCardResponse;
    }
}
