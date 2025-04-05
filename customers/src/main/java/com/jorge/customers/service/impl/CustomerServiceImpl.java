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
    public Mono<CustomerResponse> getCustomerById(String id) {
        log.info("Fetching customer by id: {}", id);
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente con id: " + id + " no encontrado")))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<CustomerResponse> createCustomer(CustomerRequest customerRequest) {
        log.info("Creating customer: {}", customerRequest);
        return customerRepository.save(customerMapper.mapToCustomer(customerRequest))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<CustomerResponse> updateCustomerById(String id, CustomerRequest customerRequest) {
        log.info("Updating customer with id: {} with data: {}", id, customerRequest);
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente con id: " + id + " no encontrado")))
                .flatMap(existingCustomer ->
                        customerRepository.save(updateCustomerFromRequest(existingCustomer, customerRequest)))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<Void> deleteCustomerById(String id) {
        log.info("Deleting customer with id: {}", id);
        return customerRepository.deleteById((id));
    }

    public Customer updateCustomerFromRequest(Customer existingCustomer, CustomerRequest customerRequest) {
        log.debug("Updating existing customer: {} with request: {}", existingCustomer, customerRequest);
        Customer updatedCustomer = customerMapper.mapToCustomer(customerRequest);
        updatedCustomer.setId(existingCustomer.getId());
        return updatedCustomer;
    }
}
