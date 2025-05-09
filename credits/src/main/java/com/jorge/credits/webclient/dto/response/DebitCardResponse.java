package com.jorge.credits.webclient.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class DebitCardResponse {
    private String id;
    private String cardHolderId;
    private List<String> linkedAccountsNumber;
    private String mainLinkedAccountNumber;
    private String debitCardNumber;
    private String cvv;
    private LocalDate expiryDate;
    private DebitCardStatus status;
    private LocalDateTime createdAt;

    public enum DebitCardStatus {
        ACTIVE,
        BLOCKED
    }
}
