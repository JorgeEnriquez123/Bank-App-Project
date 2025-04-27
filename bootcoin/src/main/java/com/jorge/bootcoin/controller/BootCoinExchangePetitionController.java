package com.jorge.bootcoin.controller;


import com.jorge.bootcoin.dto.kafka.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.service.BootCoinExchangePetitionService;
import com.jorge.bootcoin.tempdto.BootCoinExchangePetitionRequest;
import com.jorge.bootcoin.tempdto.BootCoinExchangePetitionResponse;
import com.jorge.bootcoin.tempdto.BootCoinSellerPaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bootcoin-exchange-petitions")
@RequiredArgsConstructor
public class BootCoinExchangePetitionController {
    private final BootCoinExchangePetitionService bootCoinExchangePetitionService;

    @GetMapping
    public Flux<BootCoinExchangePetitionResponse> getAllPetitions() {
        return bootCoinExchangePetitionService.getAllPetitions();
    }

    @GetMapping("/{id}")
    public Mono<BootCoinExchangePetitionResponse> getPetitionById(@PathVariable String id) {
        return bootCoinExchangePetitionService.getPetitionById(id);
    }

    @PostMapping("/buyer-wallet/{id}")
    public Mono<ResponseEntity<BootCoinExchangePetitionResponse>> createPetition(@PathVariable String id,
                                                                                 @RequestBody BootCoinExchangePetitionRequest request) {
        return bootCoinExchangePetitionService.createPetition(id, request)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @PutMapping("/{id}")
    public Mono<BootCoinExchangePetitionResponse> updatePetition(@PathVariable String id,
                                                                 @RequestBody BootCoinExchangePetitionRequest request) {
        return bootCoinExchangePetitionService.updatePetition(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deletePetition(@PathVariable String id) {
        return bootCoinExchangePetitionService.deletePetition(id);
    }

    @PostMapping("/{id}/accept")
    public Mono<SuccessfulEventOperationResponse> acceptBootCoinExchangePetition(@PathVariable String id,
                                                                                 @RequestBody BootCoinSellerPaymentMethod sellerPaymentMethod) {
        return bootCoinExchangePetitionService.acceptBootCoinExchangePetition(id, sellerPaymentMethod);
    }
}
