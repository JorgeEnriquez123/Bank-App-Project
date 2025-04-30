package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.FixedTermAccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.FixedTermAccount;
import com.jorge.accounts.model.FixedTermAccountRequest;
import com.jorge.accounts.model.FixedTermAccountResponse;
import com.jorge.accounts.repository.FixedTermAccountRepository;
import com.jorge.accounts.utils.AccountUtils;
import com.jorge.accounts.utils.CustomerValidation;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.response.CustomerResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FixedTermAccountServiceImplTest {
    @Mock
    private CustomerClient customerClient;
    @Mock
    private AccountUtils accountUtils; // We'll use a real instance of AccountUtils with mocked TransactionClient later
    @Mock
    private FixedTermAccountRepository fixedTermAccountRepository;

    private FixedTermAccountMapper fixedTermAccountMapper; // Mockito will not create this instance initially
    @Mock
    private CustomerValidation customerValidation;
    @Mock
    private TransactionClient transactionClient; // Mock the dependency for AccountUtils

    @InjectMocks
    private FixedTermAccountServiceImpl fixedTermAccountServiceImpl;

    private String customerId;
    private FixedTermAccountRequest fixedTermAccountRequest;
    private CustomerResponse personalCustomer;
    private CustomerResponse vipCustomer;
    private FixedTermAccount fixedTermAccount;

    @BeforeEach
    void setUp() {
        customerId = "customer456";

        fixedTermAccountRequest = new FixedTermAccountRequest();
        fixedTermAccountRequest.setCustomerId(customerId);
        fixedTermAccountRequest.setBalance(BigDecimal.valueOf(5000.0));
        fixedTermAccountRequest.setMovementsThisMonth(0);
        fixedTermAccountRequest.setMaxMoveementsFeeFreeThisMonth(1);
        fixedTermAccountRequest.setIsCommissionFeeActive(false);
        fixedTermAccountRequest.setMovementCommissionFee(BigDecimal.ZERO);
        fixedTermAccountRequest.setAllowedWithdrawal(LocalDate.now().plusMonths(6));

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

        fixedTermAccount = new FixedTermAccount();
        fixedTermAccount.setId(UUID.randomUUID().toString());
        fixedTermAccount.setAccountNumber("98765432109876");
        fixedTermAccount.setBalance(BigDecimal.valueOf(5000.0));
        fixedTermAccount.setCustomerId(customerId);
        fixedTermAccount.setAccountType(Account.AccountType.FIXED_TERM);
        fixedTermAccount.setCreatedAt(LocalDateTime.now());
        fixedTermAccount.setMovementsThisMonth(0);
        fixedTermAccount.setMaxMovementsFeeFreeThisMonth(1);
        fixedTermAccount.setIsCommissionFeeActive(false);
        fixedTermAccount.setMovementCommissionFee(BigDecimal.ZERO);
        fixedTermAccount.setAllowedWithdrawal(LocalDate.now().plusMonths(6));

        // Create the real FixedTermAccountMapper instance with the mocked AccountUtils
        fixedTermAccountMapper = new FixedTermAccountMapper(accountUtils); // Pass the @Mocked accountUtils here

        // Inject the dependencies into the service implementation
        // @InjectMocks would usually handle this if the fields were annotated.
        // Since we are manually creating the mapper, we need to ensure all dependencies are provided.
        // Also, ensure the *mocked* accountUtils is injected into the service.
        fixedTermAccountServiceImpl = new FixedTermAccountServiceImpl(customerClient, accountUtils, fixedTermAccountRepository, fixedTermAccountMapper, customerValidation);}

    @Test
    void whenCreateFixedTermAccount_WithPersonalCustomer_ThenReturnFixedTermAccountResponse() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.personalCustomerValidation(personalCustomer, Account.AccountType.FIXED_TERM)).thenReturn(Mono.empty());
        when(accountUtils.generateAccountNumber()).thenReturn("98765432109876"); // Mock generateAccountNumber
        when(fixedTermAccountRepository.save(any(FixedTermAccount.class))).thenReturn(Mono.just(fixedTermAccount));
        when(accountUtils.handleInitialDeposit(any(FixedTermAccount.class), any(BigDecimal.class))).thenReturn(Mono.just(fixedTermAccount));

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectNextMatches(response -> response != null &&
                        response.getCustomerId().equals(customerId) &&
                        response.getAccountNumber().equals("98765432109876") &&
                        response.getBalance().compareTo(BigDecimal.valueOf(5000.0)) == 0 &&
                        response.getAccountType() == FixedTermAccountResponse.AccountTypeEnum.FIXED_TERM) // Add some basic assertions
                .verifyComplete();
    }

    @Test
    void whenCreateFixedTermAccount_WithVIPCustomer_ThenReturnFixedTermAccountResponse() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(vipCustomer));
        when(customerValidation.validateCreditCardExists(vipCustomer)).thenReturn(Mono.just(vipCustomer));
        when(customerValidation.personalCustomerValidation(vipCustomer, Account.AccountType.FIXED_TERM)).thenReturn(Mono.empty());
        when(accountUtils.generateAccountNumber()).thenReturn("98765432109876"); // Mock generateAccountNumber
        when(fixedTermAccountRepository.save(any(FixedTermAccount.class))).thenReturn(Mono.just(fixedTermAccount));
        when(accountUtils.handleInitialDeposit(any(FixedTermAccount.class), any(BigDecimal.class))).thenReturn(Mono.just(fixedTermAccount));
        // No need to mock mapToFixedTermAccountResponse if you expect the real one to be called

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectNextMatches(response -> response != null)
                .verifyComplete();
    }

    @Test
    void whenCreateFixedTermAccount_WithBusinessCustomer_ThenThrowBadRequestException() {
        CustomerResponse businessCustomer = new CustomerResponse();
        businessCustomer.setId(customerId);
        businessCustomer.setCustomerType(CustomerResponse.CustomerType.BUSINESS);
        businessCustomer.setIsVIP(false);
        businessCustomer.setIsPYME(false);

        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(businessCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(businessCustomer)).thenReturn(Mono.just(businessCustomer));
        when(customerValidation.businessCustomerValidation(Account.AccountType.FIXED_TERM)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business customer can't create a FIXED_TERM account")));

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Business customer can't create a FIXED_TERM account"))
                .verify();
    }

    @Test
    void whenCreateFixedTermAccount_WithNonExistingCustomer_ThenThrowNotFoundException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.empty());

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " not found"))
                .verify();
    }

    @Test
    void whenCreateFixedTermAccount_WithCustomerOverdueDebt_ThenThrowBadRequestException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer with dni: " + customerId + " has overdue debts")));

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " has overdue debts"))
                .verify();
    }

    @Test
    void whenCreateFixedTermAccount_WithPersonalCustomerExistingFixedTermAccount_ThenThrowConflictException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.personalCustomerValidation(personalCustomer, Account.AccountType.FIXED_TERM)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Customer with dni: " + customerId + " already has a FIXED_TERM account")));

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.CONFLICT &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " already has a FIXED_TERM account"))
                .verify();
    }

    @Test
    void whenCreateFixedTermAccount_WithVIPCustomerWithoutCreditCard_ThenThrowBadRequestException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(vipCustomer));
        when(customerValidation.validateCreditCardExists(vipCustomer)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer does not own a credit card. VIP and PYME need a credit card to create an account")));

        StepVerifier.create(fixedTermAccountServiceImpl.createFixedTermAccount(fixedTermAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer does not own a credit card. VIP and PYME need a credit card to create an account"))
                .verify();
    }

    @Test
    void whenUpdateFixedTermAccountByAccountNumber_WithExistingAccount_ThenReturnUpdatedResponse() {
        String accountNumber = "98765432109876";
        FixedTermAccount existingAccount = new FixedTermAccount();
        existingAccount.setId(UUID.randomUUID().toString());
        existingAccount.setAccountNumber(accountNumber);
        existingAccount.setCustomerId(customerId);
        existingAccount.setBalance(BigDecimal.valueOf(5000.0));
        existingAccount.setCreatedAt(LocalDateTime.now().minusDays(10));
        existingAccount.setAccountType(Account.AccountType.FIXED_TERM);
        existingAccount.setMovementsThisMonth(0);
        existingAccount.setMaxMovementsFeeFreeThisMonth(1);
        existingAccount.setIsCommissionFeeActive(false);
        existingAccount.setMovementCommissionFee(BigDecimal.ZERO);
        existingAccount.setAllowedWithdrawal(LocalDate.now().plusMonths(6));

        FixedTermAccountRequest updateRequest = new FixedTermAccountRequest();
        updateRequest.setBalance(BigDecimal.valueOf(6000.0));
        updateRequest.setMovementsThisMonth(1);
        updateRequest.setMaxMoveementsFeeFreeThisMonth(2);
        updateRequest.setIsCommissionFeeActive(true);
        updateRequest.setMovementCommissionFee(BigDecimal.valueOf(0.75));
        updateRequest.setAllowedWithdrawal(LocalDate.now().plusMonths(12));


        FixedTermAccount accountAfterUpdateLogic = new FixedTermAccount();
        accountAfterUpdateLogic.setId(existingAccount.getId());
        accountAfterUpdateLogic.setAccountNumber(existingAccount.getAccountNumber());
        accountAfterUpdateLogic.setCustomerId(existingAccount.getCustomerId());
        accountAfterUpdateLogic.setCreatedAt(existingAccount.getCreatedAt());
        accountAfterUpdateLogic.setAccountType(existingAccount.getAccountType());
        accountAfterUpdateLogic.setBalance(updateRequest.getBalance());
        accountAfterUpdateLogic.setMovementsThisMonth(updateRequest.getMovementsThisMonth());
        accountAfterUpdateLogic.setMaxMovementsFeeFreeThisMonth(updateRequest.getMaxMoveementsFeeFreeThisMonth());
        accountAfterUpdateLogic.setIsCommissionFeeActive(updateRequest.getIsCommissionFeeActive());
        accountAfterUpdateLogic.setMovementCommissionFee(updateRequest.getMovementCommissionFee());
        accountAfterUpdateLogic.setAllowedWithdrawal(updateRequest.getAllowedWithdrawal());

        when(fixedTermAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(existingAccount));
        when(fixedTermAccountRepository.save(any(FixedTermAccount.class))).thenReturn(Mono.just(accountAfterUpdateLogic));

        StepVerifier.create(fixedTermAccountServiceImpl.updateFixedTermAccountByAccountNumber(accountNumber, updateRequest))
                .expectNextMatches(response -> response != null &&
                        response.getId().equals(existingAccount.getId()) &&
                        response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(6000.0)) == 0 &&
                        response.getMovementsThisMonth() == 1 &&
                        response.getMaxMovementsFeeFreeThisMonth() == 2 &&
                        response.getIsCommissionFeeActive() == true &&
                        response.getMovementCommissionFee().compareTo(BigDecimal.valueOf(0.75)) == 0 &&
                        response.getAllowedWithdrawal().isEqual(LocalDate.now().plusMonths(12)))
                .verifyComplete();
    }

    @Test
    void whenUpdateFixedTermAccountByAccountNumber_WithNonExistingAccount_ThenThrowNotFoundException() {
        String accountNumber = "nonexistent";
        FixedTermAccountRequest updateRequest = new FixedTermAccountRequest();

        when(fixedTermAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.empty());

        StepVerifier.create(fixedTermAccountServiceImpl.updateFixedTermAccountByAccountNumber(accountNumber, updateRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Fixed Term Account with account number: " + accountNumber + " not found"))
                .verify();
    }
}
