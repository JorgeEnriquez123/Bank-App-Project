package com.jorge.customers.service.impl;

import com.jorge.customers.mapper.CustomerMapper;
import com.jorge.customers.model.Customer;
import com.jorge.customers.model.CustomerResponse;
import com.jorge.customers.model.ProductSummaryResponse;
import com.jorge.customers.repository.CustomerRepository;
import com.jorge.customers.webclient.client.AccountClient;
import com.jorge.customers.webclient.client.CreditClient;
import com.jorge.customers.webclient.model.AccountResponse;
import com.jorge.customers.webclient.model.CreditCardResponse;
import com.jorge.customers.webclient.model.CreditResponse;
import com.jorge.customers.webclient.model.DebitCardResponse;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceImplTest {
    @Mock
    private CustomerRepository customerRepository;
    @Spy
    private CustomerMapper customerMapper;
    @Mock
    private AccountClient accountClient;
    @Mock
    private CreditClient creditClient;
    @InjectMocks
    private CustomerServiceImpl customerServiceImpl;

    private String customerId;
    private Customer customer;

    @BeforeEach
    void setUp(){
        customerId = UUID.randomUUID().toString();
        customer = Customer.builder()
                .id(customerId)
                .customerType(Customer.CustomerType.PERSONAL)
                .firstName("John")
                .lastName("Doe")
                .isVIP(true)
                .isPYME(false)
                .build();
    }

    @Test
    void whenGetCustomerById_WithExistingId_ThenReturnCustomerResponse(){
        when(customerRepository.findById(customerId)).thenReturn(Mono.just(customer));

        Mono<CustomerResponse> customerResponseMono = customerServiceImpl.getCustomerById(customerId);

        StepVerifier.create(customerResponseMono)
                .assertNext(customerResponse -> {
                    assertEquals(customerId, customerResponse.getId());
                    assertEquals("John", customerResponse.getFirstName());
                    assertEquals("Doe", customerResponse.getLastName());
                    assertEquals(CustomerResponse.CustomerTypeEnum.PERSONAL, customerResponse.getCustomerType());
                    assertEquals(true, customerResponse.getIsVIP());
                    assertEquals(false, customerResponse.getIsPYME());
                })
                .verifyComplete();
    }

    @Test
    void whenGetProductSummaryByCustomerId_WithExistingId_ThenReturnProductSummary() {
        when(customerRepository.findById(customerId)).thenReturn(Mono.just(customer));
        when(accountClient.getAccountsByCustomerId(customerId)).thenReturn(Flux.just(createAccountResponse()));
        when(accountClient.getDebitCardsByCardHolderId(customerId)).thenReturn(Flux.just(createDebitCardResponse()));
        when(creditClient.getCreditCardsByCardHolderId(customerId)).thenReturn(Flux.just(createCreditCardResponse()));
        when(creditClient.getCreditsByCreditHolderId(customerId)).thenReturn(Flux.just(createCreditResponse()));

        Mono<ProductSummaryResponse> productSummaryResponseMono = customerServiceImpl.getProductSummaryByCustomerId(customerId);

        StepVerifier.create(productSummaryResponseMono)
                .assertNext(productSummary -> {
                    assertEquals(customerId, productSummary.getCustomerId());
                    assertEquals(ProductSummaryResponse.CustomerTypeEnum.valueOf(Customer.CustomerType.PERSONAL.name()), productSummary.getCustomerType());
                    assertEquals("John", productSummary.getFirstName());
                    assertEquals("Doe", productSummary.getLastName());
                    assertEquals(Boolean.TRUE, productSummary.getIsVIP());
                    assertEquals(Boolean.FALSE, productSummary.getIsPYME());
                    assertEquals(4, productSummary.getProducts().size());
                })
                .verifyComplete();
    }


    private AccountResponse createAccountResponse() {
        AccountResponse response = new AccountResponse();
        response.setId(UUID.randomUUID().toString());
        response.setAccountType(AccountResponse.AccountType.SAVINGS);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private DebitCardResponse createDebitCardResponse() {
        DebitCardResponse response = new DebitCardResponse();
        response.setId(UUID.randomUUID().toString());
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private CreditCardResponse createCreditCardResponse() {
        CreditCardResponse response = new CreditCardResponse();
        response.setId(UUID.randomUUID().toString());
        response.setType(CreditCardResponse.CreditCardType.PERSONAL_CREDIT_CARD);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    private CreditResponse createCreditResponse() {
        CreditResponse response = new CreditResponse();
        response.setId(UUID.randomUUID().toString());
        response.setCreditType(CreditResponse.CreditType.PERSONAL);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
