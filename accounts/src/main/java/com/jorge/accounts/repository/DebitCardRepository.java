package com.jorge.accounts.repository;

import com.jorge.accounts.model.DebitCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface DebitCardRepository extends ReactiveMongoRepository<DebitCard, String> {
    Mono<DebitCard> findByDebitCardNumber(String debitCardNumber);
    Mono<Void> deleteByDebitCardNumber(String debitCardNumber);
}
