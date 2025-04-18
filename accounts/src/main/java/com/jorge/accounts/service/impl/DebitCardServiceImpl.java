package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.DebitCardMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.repository.DebitCardRepository;
import com.jorge.accounts.service.DebitCardService;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebitCardServiceImpl implements DebitCardService {
    private final DebitCardRepository debitCardRepository;
    private final AccountRepository accountRepository;
    private final DebitCardMapper debitCardMapper;
    private final TransactionClient transactionClient;

    @Override
    public Flux<DebitCardResponse> getAllDebitCards() {
        log.info("Fetching all debit cards");
        return debitCardRepository.findAll()
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> getDebitCardById(String id) {
        log.info("Fetching debit card with id: {}", id);
        return debitCardRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debit Card with id: " + id + " not found")))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> getDebitCardByDebitCardNumber(String debitCardNumber) {
        log.info("Fetching debit card with debit card number: {}", debitCardNumber);
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Flux<DebitCardResponse> getDebitCardsByCardHolderId(String cardHolderId) {
        log.info("Fetching debit cards with card holder id: {}", cardHolderId);
        return debitCardRepository.findByCardHolderId(cardHolderId)
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> createDebitCard(DebitCardRequest debitCardRequest) {
        log.info("Creating a new debit card for customer Id: {}", debitCardRequest.getCardHolderId());
        return debitCardRepository.save(debitCardMapper.mapToDebitCard(debitCardRequest))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> updateDebitCardByDebitCardNumber(String debitCardNumber, DebitCardRequest debitCardRequest) {
        log.info("Updating debit card with debit card number: {}", debitCardNumber);
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMap(existingDebitCard ->
                        debitCardRepository.save(updateDebitCardFromRequest(existingDebitCard, debitCardRequest)))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<Void> deleteDebitCardByDebitCardNumber(String debitCardNumber) {
        log.info("Deleting debit card with debit card number: {}", debitCardNumber);
        return debitCardRepository.deleteByDebitCardNumber(debitCardNumber);
    }

    @Override
    public Mono<BalanceResponse> withdrawByDebitCardNumber(String debitCardNumber, WithdrawalRequest withdrawalRequest) {
        log.info("Starting withdrawal of amount: {} from debit card with debit card number: {}", withdrawalRequest.getAmount(), debitCardNumber);
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMap(debitCard -> accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Account with account number: " + debitCard.getMainLinkedAccountNumber() + " not found")))
                        .flatMap(mainAccount -> {
                            if (mainAccount.getBalance().compareTo(withdrawalRequest.getAmount()) > 0) {
                                log.info("Main account balance is sufficient, proceeding with withdrawal");
                                return withdrawFromAccount(mainAccount, withdrawalRequest);
                            } else {
                                log.info("Main account balance is insufficient, checking linked accounts");
                                return Flux.fromIterable(debitCard.getLinkedAccountsNumber())
                                        .filter(accountNumber -> !accountNumber.equals(debitCard.getMainLinkedAccountNumber()))
                                        .flatMap(accountRepository::findByAccountNumber)
                                        .filter(account -> account.getBalance().compareTo(withdrawalRequest.getAmount()) >= 0)
                                        .next()
                                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Debit Card does not have enough balance in any of its linked accounts")))
                                        .flatMap(accountWithSufficientBalance ->
                                                withdrawFromAccount(accountWithSufficientBalance, withdrawalRequest));
                            }
                        })
                        .map(accountDebited -> {
                            log.info("Withdrawal successful, returning balance response");
                            BalanceResponse balanceResponse = new BalanceResponse();
                            balanceResponse.setAccountNumber(accountDebited.getAccountNumber());
                            balanceResponse.setAccountType(BalanceResponse.AccountTypeEnum.valueOf(accountDebited.getAccountType().name()));
                            balanceResponse.setBalance(accountDebited.getBalance());
                            return balanceResponse;
                        }));
    }

    @Override
    public Mono<BalanceResponse> getBalanceByDebitCardNumber(String debitCardNumber) {
        log.info("Getting balance by debit card number: {}", debitCardNumber);
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMap(debitCard -> accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Account with account number: " + debitCard.getMainLinkedAccountNumber() + " not found")))
                        .map(account -> {
                            log.info("Returning balance response for account number: {}", account.getAccountNumber());
                            BalanceResponse balanceResponse = new BalanceResponse();
                            balanceResponse.setAccountNumber(account.getAccountNumber());
                            balanceResponse.setAccountType(BalanceResponse.AccountTypeEnum.valueOf(account.getAccountType().name()));
                            balanceResponse.setBalance(account.getBalance());
                            return balanceResponse;
                        }));
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByDebitCardNumberLast10(String debitCardNumber) {
        log.info("Fetching last 10 transactions for debit card with debit card number: {}", debitCardNumber);
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMapMany(debitCard -> accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Account with account number: " + debitCard.getMainLinkedAccountNumber() + " not found")))
                        .flatMapMany(account -> transactionClient.getTransactionsByAccountNumber(account.getAccountNumber()))
                        .sort(Comparator.comparing(TransactionResponse::getCreatedAt).reversed())
                        .take(10));
    }

    private Mono<Account> withdrawFromAccount(Account account, WithdrawalRequest withdrawalRequest) {
        log.info("Withdrawing amount: {} from account with account number: {}", withdrawalRequest.getAmount(), account.getAccountNumber());
        BigDecimal withdrawalAmount = withdrawalRequest.getAmount();
        if(account.getIsCommissionFeeActive()) {
            log.info("Account has commission fee active, adding commission fee to withdrawal amount");
            withdrawalAmount = withdrawalAmount.add(account.getMovementCommissionFee());
        }

        if(account.getBalance().compareTo(withdrawalAmount) < 0) {
            log.warn("Account does not have enough balance");
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account does not have enough balance"));
        }

        account.setBalance(account.getBalance().subtract(withdrawalAmount));
        return accountRepository.save(account)
                .flatMap(savedAccount -> {
                    TransactionRequest transactionRequest = new TransactionRequest();
                    transactionRequest.setAccountNumber(savedAccount.getAccountNumber());
                    transactionRequest.setAmount(withdrawalRequest.getAmount());
                    transactionRequest.setTransactionType(TransactionRequest.TransactionType.WITHDRAWAL);
                    transactionRequest.setDescription("Withdrawal from debit card");
                    if (savedAccount.getIsCommissionFeeActive()) {
                        transactionRequest.setFee(savedAccount.getMovementCommissionFee());
                    } else {
                        transactionRequest.setFee(BigDecimal.ZERO);
                    }
                    return transactionClient.createTransaction(transactionRequest)
                            .doOnSuccess(transactionResponse ->
                                    log.info("Withdrawal transaction created successfully for account number: {}", savedAccount.getAccountNumber()))
                            .doOnError(throwable -> log.error("Error creating transaction: {}", throwable.getMessage()))
                            .thenReturn(savedAccount);
                });
    }

    private DebitCard updateDebitCardFromRequest(DebitCard existingCreditCard, DebitCardRequest debitCardRequest) {
        log.info("Updating debit card from request");
        DebitCard updatedDebitCard = debitCardMapper.mapToDebitCard(debitCardRequest);
        updatedDebitCard.setId(existingCreditCard.getId());
        updatedDebitCard.setCreatedAt(existingCreditCard.getCreatedAt());
        return updatedDebitCard;
    }
}
