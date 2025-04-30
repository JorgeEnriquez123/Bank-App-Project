package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.SavingsAccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.SavingsAccount;
import com.jorge.accounts.model.SavingsAccountRequest;
import com.jorge.accounts.model.SavingsAccountResponse;
import com.jorge.accounts.repository.SavingsAccountRepository;
import com.jorge.accounts.utils.AccountUtils;
import com.jorge.accounts.utils.CustomerValidation;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.response.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SavingsAccountServiceImplTest {
    @Mock
    private CustomerClient customerClient;
    @Mock
    private AccountUtils accountUtils; // We'll use a real instance of AccountUtils with mocked TransactionClient later
    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    private SavingsAccountMapper savingsAccountMapper; // Mockito will not create this instance initially
    @Mock
    private CustomerValidation customerValidation;
    @Mock
    private TransactionClient transactionClient; // Mock the dependency for AccountUtils

    @InjectMocks
    private SavingsAccountServiceImpl savingsAccountServiceImpl;

    private String customerId;
    private SavingsAccountRequest savingsAccountRequest;
    private CustomerResponse personalCustomer;
    private CustomerResponse vipCustomer;
    private SavingsAccount savingsAccount;

    @BeforeEach
    void setUp() {
        customerId = "customer123";

        savingsAccountRequest = new SavingsAccountRequest();
        savingsAccountRequest.setCustomerId(customerId);
        savingsAccountRequest.setBalance(BigDecimal.valueOf(100.0));
        savingsAccountRequest.setMovementsThisMonth(0);
        savingsAccountRequest.setMaxMovementsFeeFreeThisMonth(10);
        savingsAccountRequest.setIsCommissionFeeActive(false);
        savingsAccountRequest.setMovementCommissionFee(BigDecimal.ZERO);
        savingsAccountRequest.setMonthlyMovementsLimit(20);

        personalCustomer = new CustomerResponse();
        personalCustomer.setId(customerId);
        personalCustomer.setCustomerType(CustomerResponse.CustomerType.PERSONAL);
        personalCustomer.setIsVIP(false);
        personalCustomer.setIsPYME(false);

        vipCustomer = new CustomerResponse();
        vipCustomer.setId(customerId);
        vipCustomer.setCustomerType(CustomerResponse.CustomerType.PERSONAL); // VIP can also be Personal
        vipCustomer.setIsVIP(true);
        vipCustomer.setIsPYME(false);

        savingsAccount = new SavingsAccount();
        savingsAccount.setId(UUID.randomUUID().toString());
        savingsAccount.setAccountNumber("12345678901234");
        savingsAccount.setBalance(BigDecimal.valueOf(100.0));
        savingsAccount.setCustomerId(customerId);
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setCreatedAt(LocalDateTime.now());
        savingsAccount.setMovementsThisMonth(0);
        savingsAccount.setMaxMovementsFeeFreeThisMonth(10);
        savingsAccount.setIsCommissionFeeActive(false);
        savingsAccount.setMovementCommissionFee(BigDecimal.ZERO);
        savingsAccount.setMonthlyMovementsLimit(20);

        // **Crucial Change:** Create a real instance of SavingsAccountMapper and its dependency
        // Provide the mocked AccountUtils to the real SavingsAccountMapper constructor
        AccountUtils realAccountUtils = new AccountUtils(transactionClient); // Create a real AccountUtils with the mocked TransactionClient
        savingsAccountMapper = new SavingsAccountMapper(realAccountUtils); // Create the real SavingsAccountMapper instance

        // Inject the real (now spied) SavingsAccountMapper into the service
        // Note: @InjectMocks will now inject this manually created instance if the field already exists
        // If we didn't manually create it, @InjectMocks would fail due to the lack of a no-arg constructor.
        savingsAccountServiceImpl = new SavingsAccountServiceImpl(customerClient, accountUtils, savingsAccountRepository, savingsAccountMapper, customerValidation);
    }

    @Test
    void whenCreateSavingsAccount_WithBusinessCustomer_ThenThrowBadRequestException() {
        CustomerResponse businessCustomer = new CustomerResponse();
        businessCustomer.setId(customerId);
        businessCustomer.setCustomerType(CustomerResponse.CustomerType.BUSINESS);
        businessCustomer.setIsVIP(false);
        businessCustomer.setIsPYME(false); // Business can be non-PYME

        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(businessCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(businessCustomer)).thenReturn(Mono.just(businessCustomer));
        when(customerValidation.businessCustomerValidation(Account.AccountType.SAVINGS)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business customer can't create a SAVINGS account")));

        StepVerifier.create(savingsAccountServiceImpl.createSavingsAccount(savingsAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Business customer can't create a SAVINGS account"))
                .verify();
    }

    @Test
    void whenCreateSavingsAccount_WithNonExistingCustomer_ThenThrowNotFoundException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.empty());

        StepVerifier.create(savingsAccountServiceImpl.createSavingsAccount(savingsAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " not found"))
                .verify();
    }

    @Test
    void whenCreateSavingsAccount_WithCustomerOverdueDebt_ThenThrowBadRequestException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer with dni: " + customerId + " has overdue debts")));

        StepVerifier.create(savingsAccountServiceImpl.createSavingsAccount(savingsAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " has overdue debts"))
                .verify();
    }

    @Test
    void whenCreateSavingsAccount_WithPersonalCustomerExistingSavingsAccount_ThenThrowConflictException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.personalCustomerValidation(personalCustomer, Account.AccountType.SAVINGS)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Customer with dni: " + customerId + " already has a SAVINGS account")));

        StepVerifier.create(savingsAccountServiceImpl.createSavingsAccount(savingsAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.CONFLICT &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " already has a SAVINGS account"))
                .verify();
    }

    @Test
    void whenCreateSavingsAccount_WithVIPCustomerWithoutCreditCard_ThenThrowBadRequestException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(vipCustomer));
        when(customerValidation.validateCreditCardExists(vipCustomer)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer does not own a credit card. VIP and PYME need a credit card to create an account")));

        StepVerifier.create(savingsAccountServiceImpl.createSavingsAccount(savingsAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer does not own a credit card. VIP and PYME need a credit card to create an account"))
                .verify();
    }


    @Test
    void whenUpdateSavingsAccountByAccountNumber_WithExistingAccount_ThenReturnUpdatedResponse() {
        String accountNumber = "12345678901234";
        SavingsAccount existingAccount = new SavingsAccount();
        existingAccount.setId(UUID.randomUUID().toString());
        existingAccount.setAccountNumber(accountNumber);
        existingAccount.setCustomerId(customerId);
        existingAccount.setBalance(BigDecimal.valueOf(500.0));
        existingAccount.setCreatedAt(LocalDateTime.now().minusDays(5));
        existingAccount.setMonthlyMovementsLimit(10);
        existingAccount.setAccountType(Account.AccountType.SAVINGS);
        existingAccount.setMovementsThisMonth(5);
        existingAccount.setMaxMovementsFeeFreeThisMonth(10);
        existingAccount.setIsCommissionFeeActive(false);
        existingAccount.setMovementCommissionFee(BigDecimal.ZERO);


        SavingsAccountRequest updateRequest = new SavingsAccountRequest();
        updateRequest.setBalance(BigDecimal.valueOf(700.0));
        updateRequest.setMonthlyMovementsLimit(25);
        updateRequest.setMovementsThisMonth(7); // Example of updating another field
        updateRequest.setMaxMovementsFeeFreeThisMonth(12);
        updateRequest.setIsCommissionFeeActive(true);
        updateRequest.setMovementCommissionFee(BigDecimal.valueOf(1.0));

        SavingsAccount accountAfterUpdateLogic = new SavingsAccount();
        accountAfterUpdateLogic.setId(existingAccount.getId());
        accountAfterUpdateLogic.setAccountNumber(existingAccount.getAccountNumber());
        accountAfterUpdateLogic.setCustomerId(existingAccount.getCustomerId());
        accountAfterUpdateLogic.setCreatedAt(existingAccount.getCreatedAt());
        accountAfterUpdateLogic.setAccountType(existingAccount.getAccountType()); // Keep existing account type
        accountAfterUpdateLogic.setBalance(updateRequest.getBalance()); // Updated balance
        accountAfterUpdateLogic.setMonthlyMovementsLimit(updateRequest.getMonthlyMovementsLimit()); // Updated monthly limit
        accountAfterUpdateLogic.setMovementsThisMonth(updateRequest.getMovementsThisMonth()); // Updated movements
        accountAfterUpdateLogic.setMaxMovementsFeeFreeThisMonth(updateRequest.getMaxMovementsFeeFreeThisMonth()); // Updated max movements fee free
        accountAfterUpdateLogic.setIsCommissionFeeActive(updateRequest.getIsCommissionFeeActive()); // Updated commission active
        accountAfterUpdateLogic.setMovementCommissionFee(updateRequest.getMovementCommissionFee()); // Updated commission fee


        when(savingsAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(existingAccount));
        when(savingsAccountRepository.save(any(SavingsAccount.class))).thenReturn(Mono.just(accountAfterUpdateLogic));

        StepVerifier.create(savingsAccountServiceImpl.updateSavingsAccountByAccountNumber(accountNumber, updateRequest))
                .expectNextMatches(response -> response != null &&
                        response.getId().equals(existingAccount.getId()) &&
                        response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(700.0)) == 0 &&
                        response.getMonthlyMovementsLimit() == 25 &&
                        response.getAccountType() == SavingsAccountResponse.AccountTypeEnum.SAVINGS &&
                        response.getMovementsThisMonth() == 7 &&
                        response.getMaxMovementsFeeFreeThisMonth() == 12 &&
                        response.getIsCommissionFeeActive() == true &&
                        response.getMovementCommissionFee().compareTo(BigDecimal.valueOf(1.0)) == 0)
                .verifyComplete();
    }

    @Test
    void whenUpdateSavingsAccountByAccountNumber_WithNonExistingAccount_ThenThrowNotFoundException() {
        String accountNumber = "nonexistent";
        SavingsAccountRequest updateRequest = new SavingsAccountRequest();

        when(savingsAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.empty());

        StepVerifier.create(savingsAccountServiceImpl.updateSavingsAccountByAccountNumber(accountNumber, updateRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Savings Account with account number: " + accountNumber + " not found"))
                .verify();
    }
}
