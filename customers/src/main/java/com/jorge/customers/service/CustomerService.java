package com.jorge.customers.service;

import com.jorge.customers.model.CustomerRequest;
import com.jorge.customers.model.CustomerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Flux<CustomerResponse> getAllCustomers();
    Mono<CustomerResponse> getCustomerById(String dni);
    Mono<CustomerResponse> createCustomer(CustomerRequest customer);
    Mono<CustomerResponse> updateCustomerById(String dni, CustomerRequest customer);
    Mono<Void> deleteCustomerById(String dni);

    Mono<CustomerResponse> getCustomerByDni(String dni);
}
