package com.jorge.customers.service.impl;

import com.jorge.customers.mapper.CustomerMapper;
import com.jorge.customers.model.Customer;
import com.jorge.customers.model.CustomerRequest;
import com.jorge.customers.model.CustomerResponse;
import com.jorge.customers.model.CustomerType;
import com.jorge.customers.repository.CustomerRepository;
import com.jorge.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Flux<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers.");
        return customerRepository.findAll()
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<CustomerResponse> getCustomerByDni(String dni) {
        log.info("Fetching customer by DNI: {}", dni);
        return customerRepository.findByDni(dni)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente con dni: " + dni + " no encontrado")))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<CustomerResponse> createCustomer(CustomerRequest customerRequest) {
        log.info("Creating customer: {}", customerRequest);
        return customerRepository.save(customerMapper.mapToCustomer(customerRequest))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<CustomerResponse> updateCustomerByDni(String dni, CustomerRequest customerRequest) {
        log.info("Updating customer with DNI: {} with data: {}", dni, customerRequest);
        return customerRepository.findByDni(dni)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente con dni: " + dni + " no encontrado")))
                .flatMap(existingCustomer ->
                        customerRepository.save(updateCustomerFromRequest(existingCustomer, customerRequest)))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<Void> deleteCustomerByDni(String dni) {
        log.info("Deleting customer with DNI: {}", dni);
        return customerRepository.deleteByDni((dni));
    }

    public Customer updateCustomerFromRequest(Customer existingCustomer, CustomerRequest customerRequest) {
        log.debug("Updating existing customer: {} with request: {}", existingCustomer, customerRequest);
        existingCustomer.setCustomerType(CustomerType.valueOf(customerRequest.getCustomerType().name()));
        existingCustomer.setEmail(customerRequest.getEmail());
        existingCustomer.setPhoneNumber(customerRequest.getPhoneNumber());
        existingCustomer.setAddress(customerRequest.getAddress());
        existingCustomer.setDni(customerRequest.getDni());
        existingCustomer.setFirstName(customerRequest.getFirstName());
        existingCustomer.setLastName(customerRequest.getLastName());
        log.debug("Updated customer: {}", existingCustomer);
        return existingCustomer;
    }
}
