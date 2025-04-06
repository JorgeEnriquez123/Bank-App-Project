package com.jorge.accounts.service;

import com.jorge.accounts.model.FixedTermAccountRequest;
import com.jorge.accounts.model.FixedTermAccountResponse;
import reactor.core.publisher.Mono;

public interface FixedTermAccountService {
    Mono<FixedTermAccountResponse> createFixedTermAccount(FixedTermAccountRequest fixedTermAccountRequest);
    Mono<FixedTermAccountResponse> updateFixedTermAccountByAccountNumber(String accountNumber, FixedTermAccountRequest fixedTermAccountRequest);
}
