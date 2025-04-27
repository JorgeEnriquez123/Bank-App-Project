package com.jorge.bootcoin.repository;

import com.jorge.bootcoin.model.BootCoinExchangeRate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface BootCoinExchangeRateRepository extends ReactiveMongoRepository<BootCoinExchangeRate, String> {
    Mono<BootCoinExchangeRate> findTopByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(LocalDateTime effectiveDate);
}
