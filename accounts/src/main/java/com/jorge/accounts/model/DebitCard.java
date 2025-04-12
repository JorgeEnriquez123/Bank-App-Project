package com.jorge.accounts.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "debit-cards")
public class DebitCard {
    @Id
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
