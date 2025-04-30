package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.CheckingAccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.CheckingAccount;
import com.jorge.accounts.model.CheckingAccountRequest;
import com.jorge.accounts.model.CheckingAccountResponse;
import com.jorge.accounts.repository.CheckingAccountRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CheckingAccountServiceImplTest {
    @Mock
    private CustomerClient customerClient;
    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    private CheckingAccountMapper checkingAccountMapper; // Mockito will not create this instance initially
    @Mock
    private CustomerValidation customerValidation;
    @Mock
    private AccountUtils accountUtils; // We'll use a real instance of AccountUtils with mocked TransactionClient later
    @Mock
    private TransactionClient transactionClient; // Mock the dependency for AccountUtils

    @InjectMocks
    private CheckingAccountServiceImpl checkingAccountServiceImpl;

    private String customerId;
    private CheckingAccountRequest checkingAccountRequest;
    private CustomerResponse personalCustomer;
    private CustomerResponse businessCustomer;
    private CustomerResponse pymeCustomer;
    private CheckingAccount checkingAccount;

    @BeforeEach
    void setUp() {
        customerId = "customer789";

        checkingAccountRequest = new CheckingAccountRequest();
        checkingAccountRequest.setCustomerId(customerId);
        checkingAccountRequest.setBalance(BigDecimal.valueOf(2000.0));
        checkingAccountRequest.setMovementsThisMonth(0);
        checkingAccountRequest.setMaxMovementsFeeFreeThisMonth(Integer.MAX_VALUE); // Checking accounts often have high limits
        checkingAccountRequest.setIsCommissionFeeActive(false);
        checkingAccountRequest.setMovementCommissionFee(BigDecimal.ZERO);
        checkingAccountRequest.setMaintenanceFee(BigDecimal.valueOf(10.0)); // Default maintenance fee
        checkingAccountRequest.setHolders(List.of("holder1", "holder2"));
        checkingAccountRequest.setAuthorizedSigners(List.of("signer1"));

        personalCustomer = new CustomerResponse();
        personalCustomer.setId(customerId);
        personalCustomer.setCustomerType(CustomerResponse.CustomerType.PERSONAL);
        personalCustomer.setIsVIP(false);
        personalCustomer.setIsPYME(false);

        businessCustomer = new CustomerResponse();
        businessCustomer.setId(customerId);
        businessCustomer.setCustomerType(CustomerResponse.CustomerType.BUSINESS);
        businessCustomer.setIsVIP(false);
        businessCustomer.setIsPYME(false);

        pymeCustomer = new CustomerResponse();
        pymeCustomer.setId(customerId);
        pymeCustomer.setCustomerType(CustomerResponse.CustomerType.BUSINESS); // PYME is a type of BUSINESS
        pymeCustomer.setIsVIP(false);
        pymeCustomer.setIsPYME(true);


        checkingAccount = new CheckingAccount();
        checkingAccount.setId(UUID.randomUUID().toString());
        checkingAccount.setAccountNumber("11223344556677");
        checkingAccount.setBalance(BigDecimal.valueOf(2000.0));
        checkingAccount.setCustomerId(customerId);
        checkingAccount.setAccountType(Account.AccountType.CHECKING);
        checkingAccount.setCreatedAt(LocalDateTime.now());
        checkingAccount.setMovementsThisMonth(0);
        checkingAccount.setMaxMovementsFeeFreeThisMonth(Integer.MAX_VALUE);
        checkingAccount.setIsCommissionFeeActive(false);
        checkingAccount.setMovementCommissionFee(BigDecimal.ZERO);
        checkingAccount.setMaintenanceFee(BigDecimal.valueOf(10.0));
        checkingAccount.setHolders(List.of("holder1", "holder2"));
        checkingAccount.setAuthorizedSigners(List.of("signer1"));


        checkingAccountMapper = new CheckingAccountMapper(accountUtils);

        checkingAccountServiceImpl = new CheckingAccountServiceImpl(customerClient, checkingAccountRepository, checkingAccountMapper, customerValidation, accountUtils);
    }

    @Test
    void whenCreateCheckingAccount_WithPersonalCustomer_ThenReturnCheckingAccountResponse() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.personalCustomerValidation(personalCustomer, Account.AccountType.CHECKING)).thenReturn(Mono.empty());
        when(accountUtils.generateAccountNumber()).thenReturn("11223344556677"); // Mock generateAccountNumber
        when(checkingAccountRepository.save(any(CheckingAccount.class))).thenReturn(Mono.just(checkingAccount));
        when(accountUtils.handleInitialDeposit(any(CheckingAccount.class), any(BigDecimal.class))).thenReturn(Mono.just(checkingAccount));


        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectNextMatches(response -> response != null &&
                        response.getCustomerId().equals(customerId) &&
                        response.getAccountNumber().equals("11223344556677") &&
                        response.getBalance().compareTo(BigDecimal.valueOf(2000.0)) == 0 &&
                        response.getAccountType() == CheckingAccountResponse.AccountTypeEnum.CHECKING &&
                        response.getMaintenanceFee().compareTo(BigDecimal.valueOf(10.0)) == 0) // Assert default maintenance fee
                .verifyComplete();
    }

    @Test
    void whenCreateCheckingAccount_WithBusinessCustomer_ThenReturnCheckingAccountResponse() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(businessCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(businessCustomer)).thenReturn(Mono.just(businessCustomer));
        when(customerValidation.businessCustomerValidation(Account.AccountType.CHECKING)).thenReturn(Mono.empty());
        when(accountUtils.generateAccountNumber()).thenReturn("11223344556677"); // Mock generateAccountNumber
        when(checkingAccountRepository.save(any(CheckingAccount.class))).thenReturn(Mono.just(checkingAccount));
        when(accountUtils.handleInitialDeposit(any(CheckingAccount.class), any(BigDecimal.class))).thenReturn(Mono.just(checkingAccount));

        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectNextMatches(response -> response != null &&
                        response.getCustomerId().equals(customerId) &&
                        response.getAccountNumber().equals("11223344556677") &&
                        response.getBalance().compareTo(BigDecimal.valueOf(2000.0)) == 0 &&
                        response.getAccountType() == CheckingAccountResponse.AccountTypeEnum.CHECKING &&
                        response.getMaintenanceFee().compareTo(BigDecimal.valueOf(10.0)) == 0) // Assert default maintenance fee
                .verifyComplete();
    }

    @Test
    void whenCreateCheckingAccount_WithPYMECustomer_ThenReturnCheckingAccountResponseWithZeroMaintenanceFee() {
        // Need to create a CheckingAccount object that reflects the PYME discount
        CheckingAccount pymeCheckingAccount = new CheckingAccount();
        pymeCheckingAccount.setId(UUID.randomUUID().toString());
        pymeCheckingAccount.setAccountNumber("11223344556677");
        pymeCheckingAccount.setBalance(BigDecimal.valueOf(2000.0));
        pymeCheckingAccount.setCustomerId(customerId);
        pymeCheckingAccount.setAccountType(Account.AccountType.CHECKING);
        pymeCheckingAccount.setCreatedAt(LocalDateTime.now());
        pymeCheckingAccount.setMovementsThisMonth(0);
        pymeCheckingAccount.setMaxMovementsFeeFreeThisMonth(Integer.MAX_VALUE);
        pymeCheckingAccount.setIsCommissionFeeActive(false);
        pymeCheckingAccount.setMovementCommissionFee(BigDecimal.ZERO);
        pymeCheckingAccount.setMaintenanceFee(BigDecimal.ZERO); // Maintenance fee set to zero for PYME
        pymeCheckingAccount.setHolders(List.of("holder1", "holder2"));
        pymeCheckingAccount.setAuthorizedSigners(List.of("signer1"));


        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(pymeCustomer));
        when(customerValidation.validateCreditCardExists(pymeCustomer)).thenReturn(Mono.just(pymeCustomer)); // PYME validation
        when(customerValidation.businessCustomerValidation(Account.AccountType.CHECKING)).thenReturn(Mono.empty());
        when(accountUtils.generateAccountNumber()).thenReturn("11223344556677"); // Mock generateAccountNumber
        // When the service saves the account, return the PYME specific checking account
        when(checkingAccountRepository.save(any(CheckingAccount.class))).thenReturn(Mono.just(pymeCheckingAccount));
        when(accountUtils.handleInitialDeposit(any(CheckingAccount.class), any(BigDecimal.class))).thenReturn(Mono.just(pymeCheckingAccount));

        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectNextMatches(response -> response != null &&
                        response.getCustomerId().equals(customerId) &&
                        response.getAccountNumber().equals("11223344556677") &&
                        response.getBalance().compareTo(BigDecimal.valueOf(2000.0)) == 0 &&
                        response.getAccountType() == CheckingAccountResponse.AccountTypeEnum.CHECKING &&
                        response.getMaintenanceFee().compareTo(BigDecimal.ZERO) == 0) // Assert maintenance fee is zero
                .verifyComplete();
    }


    @Test
    void whenCreateCheckingAccount_WithNonExistingCustomer_ThenThrowNotFoundException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.empty());

        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " not found"))
                .verify();
    }

    @Test
    void whenCreateCheckingAccount_WithCustomerOverdueDebt_ThenThrowBadRequestException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer with dni: " + customerId + " has overdue debts")));

        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " has overdue debts"))
                .verify();
    }

    @Test
    void whenCreateCheckingAccount_WithPersonalCustomerExistingCheckingAccount_ThenThrowConflictException() {
        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.validateIfCustomerHasOverDueDebt(personalCustomer)).thenReturn(Mono.just(personalCustomer));
        when(customerValidation.personalCustomerValidation(personalCustomer, Account.AccountType.CHECKING)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Customer with dni: " + customerId + " already has a CHECKING account")));

        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.CONFLICT &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer with dni: " + customerId + " already has a CHECKING account"))
                .verify();
    }

    @Test
    void whenCreateCheckingAccount_WithVIPCustomerWithoutCreditCard_ThenThrowBadRequestException() {
        CustomerResponse vipCustomerWithoutCard = new CustomerResponse();
        vipCustomerWithoutCard.setId(customerId);
        vipCustomerWithoutCard.setCustomerType(CustomerResponse.CustomerType.PERSONAL);
        vipCustomerWithoutCard.setIsVIP(true);
        vipCustomerWithoutCard.setIsPYME(false);

        when(customerClient.getCustomerById(customerId)).thenReturn(Mono.just(vipCustomerWithoutCard));
        when(customerValidation.validateCreditCardExists(vipCustomerWithoutCard)).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer does not own a credit card. VIP and PYME need a credit card to create an account")));

        StepVerifier.create(checkingAccountServiceImpl.createCheckingAccount(checkingAccountRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                        ((ResponseStatusException) throwable).getReason().equals("Customer does not own a credit card. VIP and PYME need a credit card to create an account"))
                .verify();
    }

    @Test
    void whenUpdateCheckingAccountByAccountNumber_WithExistingAccount_ThenReturnUpdatedResponse() {
        String accountNumber = "11223344556677";
        CheckingAccount existingAccount = new CheckingAccount();
        existingAccount.setId(UUID.randomUUID().toString());
        existingAccount.setAccountNumber(accountNumber);
        existingAccount.setCustomerId(customerId);
        existingAccount.setBalance(BigDecimal.valueOf(2000.0));
        existingAccount.setCreatedAt(LocalDateTime.now().minusDays(5));
        existingAccount.setAccountType(Account.AccountType.CHECKING);
        existingAccount.setMovementsThisMonth(0);
        existingAccount.setMaxMovementsFeeFreeThisMonth(Integer.MAX_VALUE);
        existingAccount.setIsCommissionFeeActive(false);
        existingAccount.setMovementCommissionFee(BigDecimal.ZERO);
        existingAccount.setMaintenanceFee(BigDecimal.valueOf(10.0));
        existingAccount.setHolders(new ArrayList<>(List.of("holder1", "holder2"))); // Use mutable list for updates
        existingAccount.setAuthorizedSigners(new ArrayList<>(List.of("signer1"))); // Use mutable list for updates


        CheckingAccountRequest updateRequest = new CheckingAccountRequest();
        updateRequest.setBalance(BigDecimal.valueOf(2500.0));
        updateRequest.setMovementsThisMonth(3);
        updateRequest.setMaxMovementsFeeFreeThisMonth(50);
        updateRequest.setIsCommissionFeeActive(true);
        updateRequest.setMovementCommissionFee(BigDecimal.valueOf(0.25));
        updateRequest.setMaintenanceFee(BigDecimal.valueOf(12.0));
        updateRequest.setHolders(List.of("holder1", "holder3"));
        updateRequest.setAuthorizedSigners(List.of("signer1", "signer2"));


        // Simulate the result of updateCheckingAccountFromRequest
        CheckingAccount accountAfterUpdateLogic = new CheckingAccount();
        accountAfterUpdateLogic.setId(existingAccount.getId());
        accountAfterUpdateLogic.setAccountNumber(existingAccount.getAccountNumber());
        accountAfterUpdateLogic.setCustomerId(existingAccount.getCustomerId());
        accountAfterUpdateLogic.setCreatedAt(existingAccount.getCreatedAt());
        accountAfterUpdateLogic.setAccountType(existingAccount.getAccountType()); // Keep existing account type
        accountAfterUpdateLogic.setBalance(updateRequest.getBalance()); // Updated balance
        accountAfterUpdateLogic.setMovementsThisMonth(updateRequest.getMovementsThisMonth());
        accountAfterUpdateLogic.setMaxMovementsFeeFreeThisMonth(updateRequest.getMaxMovementsFeeFreeThisMonth());
        accountAfterUpdateLogic.setIsCommissionFeeActive(updateRequest.getIsCommissionFeeActive());
        accountAfterUpdateLogic.setMovementCommissionFee(updateRequest.getMovementCommissionFee());
        accountAfterUpdateLogic.setMaintenanceFee(updateRequest.getMaintenanceFee());
        accountAfterUpdateLogic.setHolders(updateRequest.getHolders());
        accountAfterUpdateLogic.setAuthorizedSigners(updateRequest.getAuthorizedSigners());


        when(checkingAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.just(existingAccount));
        // Mock the save operation to return the simulated updated account
        when(checkingAccountRepository.save(any(CheckingAccount.class))).thenReturn(Mono.just(accountAfterUpdateLogic));

        StepVerifier.create(checkingAccountServiceImpl.updateCheckingAccountByAccountNumber(accountNumber, updateRequest))
                .expectNextMatches(response -> response != null &&
                        response.getId().equals(existingAccount.getId()) &&
                        response.getAccountNumber().equals(accountNumber) &&
                        response.getBalance().compareTo(BigDecimal.valueOf(2500.0)) == 0 &&
                        response.getMovementsThisMonth() == 3 &&
                        response.getMaxMovementsFeeFreeThisMonth() == 50 &&
                        response.getIsCommissionFeeActive() == true &&
                        response.getMovementCommissionFee().compareTo(BigDecimal.valueOf(0.25)) == 0 &&
                        response.getMaintenanceFee().compareTo(BigDecimal.valueOf(12.0)) == 0 &&
                        response.getHolders().containsAll(List.of("holder1", "holder3")) &&
                        response.getHolders().size() == 2 &&
                        response.getAuthorizedSigners().containsAll(List.of("signer1", "signer2")) &&
                        response.getAuthorizedSigners().size() == 2)
                .verifyComplete();
    }

    @Test
    void whenUpdateCheckingAccountByAccountNumber_WithNonExistingAccount_ThenThrowNotFoundException() {
        String accountNumber = "nonexistent";
        CheckingAccountRequest updateRequest = new CheckingAccountRequest();

        when(checkingAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Mono.empty());

        StepVerifier.create(checkingAccountServiceImpl.updateCheckingAccountByAccountNumber(accountNumber, updateRequest))
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        ((ResponseStatusException) throwable).getReason().equals("Checking Account with account number: " + accountNumber + " not found"))
                .verify();
    }
}
