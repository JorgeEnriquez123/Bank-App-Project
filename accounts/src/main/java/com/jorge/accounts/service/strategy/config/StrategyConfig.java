package com.jorge.accounts.service.strategy.config;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.service.strategy.business.AccountMovementProcessStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class StrategyConfig {
    private final List<AccountMovementProcessStrategy> accountMovementStrategies;

    @Bean
    public Map<Account.AccountType, AccountMovementProcessStrategy> getAccountMovementStrategies() {
        Map<Account.AccountType, AccountMovementProcessStrategy> strategyMap = new EnumMap<>(Account.AccountType.class);
        accountMovementStrategies.forEach(accountMovementProcessStrategy ->
                strategyMap.put(accountMovementProcessStrategy.getAccountType(), accountMovementProcessStrategy));
        return strategyMap;
    }
}
