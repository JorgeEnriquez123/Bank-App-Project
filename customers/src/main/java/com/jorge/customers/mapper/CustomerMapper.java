package com.jorge.customers.mapper;

import com.jorge.customers.model.*;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    public Customer mapToCustomer(CustomerRequest customerRequest) {
        return Customer.builder()
                .customerType(Customer.CustomerType.valueOf(customerRequest.getCustomerType().getValue()))
                .email(customerRequest.getEmail())
                .phoneNumber(customerRequest.getPhoneNumber())
                .address(customerRequest.getAddress())
                .dni(customerRequest.getDni())
                .firstName(customerRequest.getFirstName())
                .lastName(customerRequest.getLastName())
                .isVIP(customerRequest.getIsVIP())
                .isPYME(customerRequest.getIsPYME())
                .build();
    }

    public CustomerResponse mapToCustomerResponse(Customer customer) {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setId(customer.getId());
        customerResponse.setCustomerType(CustomerResponse.CustomerTypeEnum.valueOf(customer.getCustomerType().name()));
        customerResponse.setEmail(customer.getEmail());
        customerResponse.setPhoneNumber(customer.getPhoneNumber());
        customerResponse.setAddress(customer.getAddress());
        customerResponse.setDni(customer.getDni());
        customerResponse.setFirstName(customer.getFirstName());
        customerResponse.setLastName(customer.getLastName());
        customerResponse.setIsVIP(customer.getIsVIP());
        customerResponse.setIsPYME(customer.getIsPYME());
        return customerResponse;
    }
}
