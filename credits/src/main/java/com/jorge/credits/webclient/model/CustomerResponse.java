package com.jorge.credits.webclient.model;

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

    public enum CustomerType {
        PERSONAL,
        BUSINESS
    }
}
