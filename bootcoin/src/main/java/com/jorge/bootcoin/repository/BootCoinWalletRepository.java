package com.jorge.bootcoin.repository;

import com.jorge.bootcoin.model.BootCoinWallet;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BootCoinWalletRepository extends ReactiveMongoRepository<BootCoinWallet, String> {
}
