package com.jorge.accounts.webclient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditResponse {
    private String id;
    private String creditHolderId;
    private CreditType creditType;
    private Status status;
    private BigDecimal creditAmount;
    private LocalDateTime createdAt;
    private LocalDate dueDate;

    public enum CreditType {
        PERSONAL,
        BUSINESS
    }

    public enum Status {
        ACTIVE,
        PAID
    }
}
