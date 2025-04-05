package com.jorge.credits.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credits")
public class Credit {
    @Id
    private String id;
    private String creditHolderId;
    private CreditType creditType;
    private Status status;
    private BigDecimal creditAmount;
    private LocalDateTime createdAt;

    public enum CreditType {
        PERSONAL,
        BUSINESS
    }

    public enum Status {
        ACTIVE,
        PAID
    }
}
