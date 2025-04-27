package com.jorge.bootcoin.repository;

import com.jorge.bootcoin.model.BootCoinExchangePetition;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BootCoinExchangePetitionRepository extends ReactiveMongoRepository<BootCoinExchangePetition, String> {
}
