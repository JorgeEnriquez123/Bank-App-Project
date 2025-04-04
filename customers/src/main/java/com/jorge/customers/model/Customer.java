package com.jorge.customers.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class Customer {
    @Id
    private String id;
    private CustomerType customerType; // Tipo de cliente (PERSONAL o BUSINESS)
    private String firstName;
    private String lastName;
    private String dni;
    private String email;
    private String phoneNumber;
    private String address;
}