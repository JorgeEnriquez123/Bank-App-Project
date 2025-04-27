package com.jorge.bootcoin.mapper;

import com.jorge.bootcoin.model.BootCoinExchangePetition;
import com.jorge.bootcoin.tempdto.BootCoinExchangePetitionRequest;
import com.jorge.bootcoin.tempdto.BootCoinExchangePetitionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BootCoinExchangePetitionMapper {
    public BootCoinExchangePetition mapToBootCoinExchangePetition(BootCoinExchangePetitionRequest request) {
        BootCoinExchangePetition petition = new BootCoinExchangePetition();
        petition.setBootCoinAmount(request.getBootCoinAmount());
        petition.setPaymentType(BootCoinExchangePetition.PaymentType.valueOf(request.getPaymentType().name()));
        petition.setPaymentMethodId(request.getPaymentMethodId());
        petition.setSellerBootCoinWalletId(request.getSellerBootCoinWalletId());
        // Defined Values
        petition.setCreatedAt(LocalDateTime.now());
        petition.setStatus(BootCoinExchangePetition.Status.PENDING);
        return petition;
    }

    public BootCoinExchangePetitionResponse mapToBootCoinExchangePetitionResponse(BootCoinExchangePetition petition) {
        BootCoinExchangePetitionResponse response = new BootCoinExchangePetitionResponse();
        response.setId(petition.getId());
        response.setBootCoinAmount(petition.getBootCoinAmount());
        response.setPaymentType(BootCoinExchangePetitionResponse.PaymentType.valueOf(petition.getPaymentType().name()));
        response.setPaymentMethodId(petition.getPaymentMethodId());
        response.setBuyerBootCoinWalletId(petition.getBuyerBootCoinWalletId());
        response.setSellerBootCoinWalletId(petition.getSellerBootCoinWalletId());
        response.setCreatedAt(petition.getCreatedAt());
        response.setStatus(BootCoinExchangePetitionResponse.Status.valueOf(petition.getStatus().name()));
        return response;
    }
}
