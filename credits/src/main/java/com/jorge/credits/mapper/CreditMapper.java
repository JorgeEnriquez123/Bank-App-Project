package com.jorge.credits.mapper;

import com.jorge.credits.model.Credit;
import com.jorge.credits.model.CreditRequest;
import com.jorge.credits.model.CreditResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreditMapper {

    public Credit mapToCredit(CreditRequest creditRequest) {
        Credit credit = new Credit();
        credit.setCustomerDni(creditRequest.getCustomerDni());
        credit.setCreditType(Credit.CreditType.valueOf(creditRequest.getCreditType().name()));
        credit.setStatus(Credit.Status.valueOf(creditRequest.getStatus().name()));
        credit.setCreditAmount(creditRequest.getCreditAmount());
        credit.setCreatedAt(LocalDateTime.now());

        switch (creditRequest.getCreditType()) {
            case PERSONAL_CREDIT_CARD, BUSINESS_CREDIT_CARD -> {
                credit.setAvailableBalance(creditRequest.getAvailableBalance());
                credit.setOutstandingBalance(creditRequest.getOutstandingBalance());
                credit.setCreditCardNumber(creditRequest.getCreditCardNumber());
                credit.setCvv(creditRequest.getCvv());
                credit.setExpiryDate(creditRequest.getExpiryDate());
            }
        }
        return credit;
    }

    public CreditResponse mapToCreditResponse(Credit credit) {
        CreditResponse creditResponse = new CreditResponse();
        creditResponse.setId(credit.getId());
        creditResponse.setCustomerDni(credit.getCustomerDni());
        creditResponse.setCreditType(CreditResponse.CreditTypeEnum.valueOf(credit.getCreditType().name()));
        creditResponse.setStatus(CreditResponse.StatusEnum.valueOf(credit.getStatus().name()));
        creditResponse.setCreditAmount(credit.getCreditAmount());
        creditResponse.setCreatedAt(credit.getCreatedAt());
        creditResponse.setAvailableBalance(credit.getAvailableBalance());
        creditResponse.setOutstandingBalance(credit.getOutstandingBalance());
        creditResponse.setCreditCardNumber(credit.getCreditCardNumber());
        creditResponse.setCvv(credit.getCvv());
        creditResponse.setExpiryDate(credit.getExpiryDate());
        return creditResponse;
    }
}
