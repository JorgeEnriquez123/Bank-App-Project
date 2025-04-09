package com.jorge.credits.repository;

import com.jorge.credits.model.Credit;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Collection;

@Repository
public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {
    Flux<Credit> findByCreditHolderIdAndCreditTypeIn(String creditHolderId, Collection<Credit.CreditType> creditTypes);
    Flux<Credit> findByCreditHolderId(String creditHolderId);
}
