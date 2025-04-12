package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.mapper.DebitCardMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.repository.DebitCardRepository;
import com.jorge.accounts.service.DebitCardService;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.model.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class DebitCardServiceImpl implements DebitCardService {
    private final DebitCardRepository debitCardRepository;
    private final AccountRepository accountRepository;
    private final DebitCardMapper debitCardMapper;
    private final TransactionClient transactionClient;

    @Override
    public Flux<DebitCardResponse> getAllDebitCards() {
        return debitCardRepository.findAll()
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> getDebitCardById(String id) {
        return debitCardRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit Card with id: " + id + " not found")))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> getDebitCardByDebitCardNumber(String debitCardNumber) {
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Flux<DebitCardResponse> getDebitCardsByCardHolderId(String cardHolderId) {
        return debitCardRepository.findByCardHolderId(cardHolderId)
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> createDebitCard(DebitCardRequest debitCardRequest) {
        return debitCardRepository.save(debitCardMapper.mapToDebitCard(debitCardRequest))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<DebitCardResponse> updateDebitCardByDebitCardNumber(String debitCardNumber, DebitCardRequest debitCardRequest) {
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMap(existingDebitCard -> debitCardRepository.save(updateDebitCardFromRequest(existingDebitCard, debitCardRequest)))
                .map(debitCardMapper::mapToDebitCardResponse);
    }

    @Override
    public Mono<Void> deleteDebitCardByDebitCardNumber(String debitCardNumber) {
        return debitCardRepository.deleteByDebitCardNumber(debitCardNumber);
    }

    @Override
    public Mono<BalanceResponse> withdrawByDebitCardNumber(String debitCardNumber, WithdrawalRequest withdrawalRequest) {
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMap(debitCard -> accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with account number: " + debitCard.getMainLinkedAccountNumber() + " not found")))
                        .flatMap(mainAccount -> {
                            if (mainAccount.getBalance().compareTo(withdrawalRequest.getAmount()) > 0) {
                                return withdrawFromAccount(mainAccount, withdrawalRequest);
                            } else {
                                return Flux.fromIterable(debitCard.getLinkedAccountsNumber())
                                        .filter(accountNumber -> !accountNumber.equals(debitCard.getMainLinkedAccountNumber()))
                                        .flatMap(accountRepository::findByAccountNumber)
                                        .filter(account -> account.getBalance().compareTo(withdrawalRequest.getAmount()) >= 0)
                                        .next()
                                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit Card does not have enough balance in any of its linked accounts")))
                                        .flatMap(accountWithSufficientBalance -> withdrawFromAccount(accountWithSufficientBalance, withdrawalRequest));
                            }
                        })
                        .map(accountDebited -> {
                            BalanceResponse balanceResponse = new BalanceResponse();
                            balanceResponse.setAccountNumber(accountDebited.getAccountNumber());
                            balanceResponse.setAccountType(BalanceResponse.AccountTypeEnum.valueOf(accountDebited.getAccountType().name()));
                            balanceResponse.setBalance(accountDebited.getBalance());
                            return balanceResponse;
                        }));
    }

    @Override
    public Mono<BalanceResponse> getBalanceByDebitCardNumber(String debitCardNumber) {
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMap(debitCard -> accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with account number: " + debitCard.getMainLinkedAccountNumber() + " not found")))
                        .map(account -> {
                            BalanceResponse balanceResponse = new BalanceResponse();
                            balanceResponse.setAccountNumber(account.getAccountNumber());
                            balanceResponse.setAccountType(BalanceResponse.AccountTypeEnum.valueOf(account.getAccountType().name()));
                            balanceResponse.setBalance(account.getBalance());
                            return balanceResponse;
                        }));
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByDebitCardNumberLast10(String debitCardNumber) {
        return debitCardRepository.findByDebitCardNumber(debitCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit Card with debit card number: " + debitCardNumber + " not found")))
                .flatMapMany(debitCard -> accountRepository.findByAccountNumber(debitCard.getMainLinkedAccountNumber())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account with account number: " + debitCard.getMainLinkedAccountNumber() + " not found")))
                        .flatMapMany(account -> transactionClient.getTransactionsByAccountNumber(account.getAccountNumber()))
                        .sort(Comparator.comparing(TransactionResponse::getCreatedAt).reversed())
                        .take(10));
    }

    private Mono<Account> withdrawFromAccount(Account account, WithdrawalRequest withdrawalRequest) {
        BigDecimal withdrawalAmount = withdrawalRequest.getAmount();
        if(account.getIsCommissionFeeActive()) {
            withdrawalAmount = withdrawalAmount.add(account.getMovementCommissionFee());
        }

        if(account.getBalance().compareTo(withdrawalAmount) < 0) {
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
                            .thenReturn(savedAccount);
                });
    }

    private DebitCard updateDebitCardFromRequest(DebitCard existingCreditCard, DebitCardRequest debitCardRequest) {
        DebitCard updatedDebitCard = debitCardMapper.mapToDebitCard(debitCardRequest);
        updatedDebitCard.setId(existingCreditCard.getId());
        updatedDebitCard.setCreatedAt(existingCreditCard.getCreatedAt());
        return updatedDebitCard;
    }
}
