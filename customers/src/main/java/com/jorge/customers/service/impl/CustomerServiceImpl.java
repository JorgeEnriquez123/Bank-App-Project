package com.jorge.customers.service.impl;

import com.jorge.customers.mapper.CustomerMapper;
import com.jorge.customers.model.*;
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
    public Mono<ProductSummaryResponse> getProductSummaryByCustomerId(String customerId) {
        log.info("Fetching product summary for customer with id: {}", customerId);

        log.info("Fetching customer with id: {}", customerId);
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer with id: " + customerId + " not found")))
                .flatMap(customer -> getAllProductsFromCustomer(customer)
                .collectList()
                        .map(products -> {
                            ProductSummaryResponse productSummaryResponse = new ProductSummaryResponse();
                            productSummaryResponse.setCustomerId(customer.getId());
                            productSummaryResponse.setCustomerType(ProductSummaryResponse.CustomerTypeEnum
                                    .valueOf(customer.getCustomerType().name()));
                            productSummaryResponse.setFirstName(customer.getFirstName());
                            productSummaryResponse.setLastName(customer.getLastName());
                            productSummaryResponse.setIsVIP(customer.getIsVIP());
                            productSummaryResponse.setIsPYME(customer.getIsPYME());
                            productSummaryResponse.setProducts(products);
                            log.info("Product summary for customer with id: {}: {}", customerId, productSummaryResponse);
                            return productSummaryResponse;
                        }));
    }

    public Customer updateCustomerFromRequest(Customer existingCustomer, CustomerRequest customerRequest) {
        log.debug("Updating existing customer: {} with request: {}", existingCustomer, customerRequest);
        Customer updatedCustomer = customerMapper.mapToCustomer(customerRequest);
        updatedCustomer.setId(existingCustomer.getId());
        return updatedCustomer;
    }

    public Flux<ProductsAvailable> getAllProductsFromCustomer(Customer customer) {
        String customerId = customer.getId();

        Flux<ProductsAvailable> accountsFlux = accountClient.getAccountsByCustomerId(customerId)
                .map(accountResponse -> {
                    ProductsAvailable accountSummary = new ProductsAvailable();
                    accountSummary.setProductType(accountResponse.getAccountType().name() + "_ACCOUNT");
                    accountSummary.setProductId(accountResponse.getId());
                    accountSummary.setCreatedAt(accountResponse.getCreatedAt());
                    return accountSummary;
                });

        Flux<ProductsAvailable> debitCardsFlux = accountClient.getDebitCardsByCardHolderId(customerId)
                .map(debitCard -> {
                    ProductsAvailable debitCardSummary = new ProductsAvailable();
                    debitCardSummary.setProductType("DEBIT_CARD");
                    debitCardSummary.setProductId(debitCard.getId());
                    debitCardSummary.setCreatedAt(debitCard.getCreatedAt());
                    log.info("Adding debit card to product summary: {}", debitCardSummary);
                    return debitCardSummary;
                });

        Flux<ProductsAvailable> creditCardsFlux = creditClient.getCreditCardsByCardHolderId(customerId)
                .map(creditCard -> {
                    ProductsAvailable creditCardSummary = new ProductsAvailable();
                    creditCardSummary.setProductType(creditCard.getType().name());
                    creditCardSummary.setProductId(creditCard.getId());
                    creditCardSummary.setCreatedAt(creditCard.getCreatedAt());
                    log.info("Adding credit card to product summary: {}", creditCardSummary);
                    return creditCardSummary;
                });

        Flux<ProductsAvailable> creditsFlux = creditClient.getCreditsByCreditHolderId(customerId)
                .map(creditResponse -> {
                    ProductsAvailable creditSummary = new ProductsAvailable();
                    creditSummary.setProductType(creditResponse.getCreditType().name() + "_CREDIT");
                    creditSummary.setProductId(creditResponse.getId());
                    creditSummary.setCreatedAt(creditResponse.getCreatedAt());
                    log.info("Adding credit to product summary: {}", creditSummary);
                    return creditSummary;
                });

        return Flux.merge(accountsFlux, debitCardsFlux, creditCardsFlux, creditsFlux);
    }
}
