package com.jorge.customers.service.impl;

import com.jorge.customers.mapper.CustomerMapper;
import com.jorge.customers.model.Customer;
import com.jorge.customers.model.CustomerRequest;
import com.jorge.customers.model.CustomerResponse;
import com.jorge.customers.model.ProductSummaryResponse;
import com.jorge.customers.repository.CustomerRepository;
import com.jorge.customers.service.CustomerService;
import com.jorge.customers.webclient.client.AccountClient;
import com.jorge.customers.webclient.client.CreditClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AccountClient accountClient;
    private final CreditClient creditClient;

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
                        "Customer with id: " + id + " not found")))
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
                        "Customer with id: " + id + " not found")))
                .flatMap(existingCustomer ->
                        customerRepository.save(updateCustomerFromRequest(existingCustomer, customerRequest)))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Mono<Void> deleteCustomerById(String id) {
        log.info("Deleting customer with id: {}", id);
        return customerRepository.deleteById((id));
    }

    @Override
    public Mono<CustomerResponse> getCustomerByDni(String dni) {
        log.info("Fetching customer by DNI: {}", dni);
        return customerRepository.findByDni(dni)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with dni: " + dni + " not found")))
                .map(customerMapper::mapToCustomerResponse);
    }

    @Override
    public Flux<ProductSummaryResponse> getProductSummaryByCustomerId(String customerId) {
        log.info("Fetching product summary for customer with id: {}", customerId);
        List<ProductSummaryResponse> productSummaryResponse = new ArrayList<>();
        log.info("Fetching accounts for customer with id: {}", customerId);
        return accountClient.getAccountsByCustomerId(customerId)
                .flatMap(accountResponse -> {
                    ProductSummaryResponse accountSummary = new ProductSummaryResponse();
                    accountSummary.setProductType(accountResponse.getAccountType().name() + "_ACCOUNT");
                    accountSummary.setProductId(accountResponse.getId());
                    accountSummary.setCreatedAt(accountResponse.getCreatedAt());
                    productSummaryResponse.add(accountSummary);
                    return Mono.just(accountResponse);
                })
                .flatMap(accountResponse -> {
                    log.info("Fetching debit cards for customer with id: {}", customerId);
                    return accountClient.getDebitCardsByCardHolderId(customerId);
                })
                .flatMap(debitCard -> {
                    ProductSummaryResponse debitCardSummary = new ProductSummaryResponse();
                    debitCardSummary.setProductType("DEBIT_CARD");
                    debitCardSummary.setProductId(debitCard.getId());
                    debitCardSummary.setCreatedAt(debitCard.getCreatedAt());
                    log.info("Adding debit card to product summary: {}", debitCardSummary);
                    productSummaryResponse.add(debitCardSummary);
                    return Mono.just(debitCard);
                })
                .flatMap(debitCard -> {
                    log.info("Fetching credit cards for customer with id: {}", customerId);
                    return creditClient.getCreditCardsByCardHolderId(customerId);
                })
                .flatMap(creditCard -> {
                    ProductSummaryResponse creditCardSummary = new ProductSummaryResponse();
                    creditCardSummary.setProductType(creditCard.getType().name());
                    creditCardSummary.setProductId(creditCard.getId());
                    creditCardSummary.setCreatedAt(creditCard.getCreatedAt());
                    log.info("Adding credit card to product summary: {}", creditCardSummary);
                    productSummaryResponse.add(creditCardSummary);
                    return Mono.just(creditCard);
                })
                .flatMap(credit -> {
                    log.info("Fetching credits for customer with id: {}", customerId);
                    return creditClient.getCreditsByCreditHolderId(customerId);
                })
                .flatMap(creditResponse -> {
                    ProductSummaryResponse creditSummary = new ProductSummaryResponse();
                    creditSummary.setProductType(creditResponse.getCreditType().name() + "_CREDIT");
                    creditSummary.setProductId(creditResponse.getId());
                    creditSummary.setCreatedAt(creditResponse.getCreatedAt());
                    log.info("Adding credit to product summary: {}", creditSummary);
                    productSummaryResponse.add(creditSummary);
                    return Mono.just(creditResponse);
                })
                .flatMap(credit -> {
                    log.info("Product summary for customer with id: {}: {}", customerId, productSummaryResponse);
                    return Flux.fromIterable(productSummaryResponse);
                });
    }

    public Customer updateCustomerFromRequest(Customer existingCustomer, CustomerRequest customerRequest) {
        log.debug("Updating existing customer: {} with request: {}", existingCustomer, customerRequest);
        Customer updatedCustomer = customerMapper.mapToCustomer(customerRequest);
        updatedCustomer.setId(existingCustomer.getId());
        return updatedCustomer;
    }
}
