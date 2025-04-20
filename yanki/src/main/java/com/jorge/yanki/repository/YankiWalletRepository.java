package com.jorge.yanki.repository;

import com.jorge.yanki.model.YankiWallet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YankiWalletRepository extends MongoRepository<YankiWallet, String> {
    Optional<YankiWallet> findByPhoneNumber(String phoneNumber);
}
