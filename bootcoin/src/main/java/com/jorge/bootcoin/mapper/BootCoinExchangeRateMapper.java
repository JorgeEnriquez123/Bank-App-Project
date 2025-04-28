package com.jorge.bootcoin.mapper;

import com.jorge.bootcoin.model.BootCoinExchangeRate;
import com.jorge.bootcoin.model.BootCoinExchangeRateRequest;
import com.jorge.bootcoin.model.BootCoinExchangeRateResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BootCoinExchangeRateMapper {
    public BootCoinExchangeRate mapToBootCoinExchangeRate(BootCoinExchangeRateRequest request) {
        BootCoinExchangeRate exchangeRate = new BootCoinExchangeRate();
        exchangeRate.setBuyRate(request.getBuyRate());
        exchangeRate.setSellRate(request.getSellRate());
        exchangeRate.setEffectiveDate(request.getEffectiveDate());
        exchangeRate.setCreatedAt(LocalDateTime.now());
        return exchangeRate;
    }

    public BootCoinExchangeRateResponse mapToBootCoinExchangeRateResponse(BootCoinExchangeRate exchangeRate) {
        BootCoinExchangeRateResponse response = new BootCoinExchangeRateResponse();
        response.setId(exchangeRate.getId());
        response.setBuyRate(exchangeRate.getBuyRate());
        response.setSellRate(exchangeRate.getSellRate());
        response.setEffectiveDate(exchangeRate.getEffectiveDate());
        response.setCreatedAt(exchangeRate.getCreatedAt());
        return response;
    }
}