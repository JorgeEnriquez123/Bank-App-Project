package com.jorge.credits.repository;

import com.jorge.credits.model.Credit;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Repository
public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {
    Flux<Credit> findByCustomerDni(String customerDni);
    Mono<Credit> findByCreditCardNumber(String creditCardNumber);

    Flux<Credit> findByCustomerDniAndCreditTypeIn(String customerDni, Collection<Credit.CreditType> creditTypes);
}
