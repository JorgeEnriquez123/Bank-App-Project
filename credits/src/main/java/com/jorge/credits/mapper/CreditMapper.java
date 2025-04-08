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
        credit.setCreditHolderId(creditRequest.getCreditHolderId());
        credit.setCreditType(Credit.CreditType.valueOf(creditRequest.getCreditType().name()));
        credit.setStatus(Credit.Status.valueOf(creditRequest.getStatus().name()));
        credit.setCreditAmount(creditRequest.getCreditAmount());
        credit.setCreatedAt(LocalDateTime.now());
        return credit;
    }

    public CreditResponse mapToCreditResponse(Credit credit) {
        CreditResponse creditResponse = new CreditResponse();
        creditResponse.setId(credit.getId());
        creditResponse.setCreditHolderId(credit.getCreditHolderId());
        creditResponse.setCreditType(CreditResponse.CreditTypeEnum.valueOf(credit.getCreditType().name()));
        creditResponse.setStatus(CreditResponse.StatusEnum.valueOf(credit.getStatus().name()));
        creditResponse.setCreditAmount(credit.getCreditAmount());
        creditResponse.setCreatedAt(credit.getCreatedAt());
        return creditResponse;
    }
}
