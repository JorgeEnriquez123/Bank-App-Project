package com.jorge.customers.service;

import com.jorge.customers.model.CustomerRequest;
import com.jorge.customers.model.CustomerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Flux<CustomerResponse> getAllCustomers();
    Mono<CustomerResponse> getCustomerByDni(String dni);
    Mono<CustomerResponse> createCustomer(CustomerRequest customer);
    Mono<CustomerResponse> updateCustomerByDni(String dni, CustomerRequest customer);
    Mono<Void> deleteCustomerByDni(String dni);
}
