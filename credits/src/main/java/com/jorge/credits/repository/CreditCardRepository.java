package com.jorge.credits.repository;

import com.jorge.credits.model.BalanceResponse;
import com.jorge.credits.model.CreditCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CreditCardRepository extends ReactiveMongoRepository<CreditCard, String> {
    Flux<CreditCard> findByCardHolderId(String cardHolderId);

    Mono<CreditCard> findByCreditCardNumber(String creditCardNumber);
}
