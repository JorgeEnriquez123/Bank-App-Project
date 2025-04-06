package com.jorge.accounts.webclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private String id;
    private CustomerType customerType; // Tipo de cliente (PERSONAL o BUSINESS)
    private String firstName;
    private String lastName;
    private String dni;
    private String email;
    private String phoneNumber;
    private String address;

    private Boolean isVIP;
    private Boolean isPYME;

    public enum CustomerType {
        PERSONAL,
        BUSINESS
    }
}
