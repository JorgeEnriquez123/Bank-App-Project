package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditMapper;
import com.jorge.credits.model.BalanceResponse;
import com.jorge.credits.model.Credit;
import com.jorge.credits.model.CreditRequest;
import com.jorge.credits.model.CreditResponse;
import com.jorge.credits.repository.CreditRepository;
import com.jorge.credits.service.CreditService;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.model.CustomerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditServiceImpl implements CreditService {
    private final CustomerClient customerClient;
    private final CreditMapper creditMapper;
    private final CreditRepository creditRepository;

    @Override
    public Flux<CreditResponse> getAllCredits() {
        log.info("Fetching all credits");
        return creditRepository.findAll()
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Flux<CreditResponse> getCreditsByCustomerDni(String customerDni) {
        log.info("Fetching credits by customer DNI: {}", customerDni);
        return creditRepository.findByCustomerDni(customerDni)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card from Customer by Dni: " + customerDni + " not found")))
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<BalanceResponse> getCreditCardAvailableBalanceByCreditCardNumber(String creditCardNumber) {
        log.info("Fetching available balance for credit card number: {}", creditCardNumber);
        return creditRepository.findByCreditCardNumber(creditCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card with card number: " + creditCardNumber + " not found")))
                .map(creditCard -> {
                    BalanceResponse balanceResponse = new BalanceResponse();
                    balanceResponse.setCreditCardNumber(creditCard.getCreditCardNumber());
                    balanceResponse.setAvailableBalance(creditCard.getAvailableBalance());
                    balanceResponse.setOutstandingBalance(creditCard.getOutstandingBalance());
                    return balanceResponse;
                });
    }

    @Override
    public Mono<CreditResponse> createCredit(CreditRequest creditRequest) {
        log.info("Creating a new credit for customer DNI: {}", creditRequest.getCustomerDni());
        Credit credit = creditMapper.mapToCredit(creditRequest);
        credit.setCreatedAt(LocalDateTime.now());

        return customerClient.getCustomerByDni(creditRequest.getCustomerDni())
                .flatMap(customerResponse -> {
                    //If the credit type is CREDIT_CARD we don't do any checks
                    if (credit.getCreditType() == Credit.CreditType.PERSONAL_CREDIT_CARD ||
                        credit.getCreditType() == Credit.CreditType.BUSINESS_CREDIT_CARD
                    ) {
                        return creditRepository.save(credit);
                    }
                    if (customerResponse.getCustomerType() == CustomerResponse.CustomerType.PERSONAL) {
                        return creditRepository.findByCustomerDniAndCreditTypeIn(
                                customerResponse.getDni(), List.of(Credit.CreditType.PERSONAL, Credit.CreditType.BUSINESS))
                                .hasElements()
                                .flatMap(hasCredit -> {
                                    if (hasCredit) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Customer with dni: " + customerResponse.getDni() + " has one credit already"));
                                    } else {
                                        return creditRepository.save(credit); // Save only if no existing credit
                                    }
                                });
                    } else {
                        return creditRepository.save(credit);
                    }
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with dni: " + creditRequest.getCustomerDni() + " not found")))
                .map(creditMapper::mapToCreditResponse);
    }
    @Override
    public Mono<Void> deleteCreditById(String id) {
        log.info("Deleting credit with id: {}", id);
        return creditRepository.deleteById(id);
    }

    @Override
    public Mono<CreditResponse> updateCreditById(String id, CreditRequest creditRequest) {
        log.info("Updating credit with id: {}", id);
        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit with id: " + id + " not found")))
                .flatMap(existingCredit -> {
                    Credit updatedCredit = creditMapper.mapToCredit(creditRequest);
                    updatedCredit.setId(existingCredit.getId());
                    updatedCredit.setCreatedAt(existingCredit.getCreatedAt());
                    return creditRepository.save(updatedCredit);
                })
                .map(creditMapper::mapToCreditResponse);
    }
}
