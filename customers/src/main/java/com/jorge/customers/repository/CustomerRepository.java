package com.jorge.customers.repository;

import com.jorge.customers.model.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveMongoRepository<Customer, String> {
    Mono<Customer> findByDni(String dni);

    Mono<Void> deleteByDni(String dni);
}
