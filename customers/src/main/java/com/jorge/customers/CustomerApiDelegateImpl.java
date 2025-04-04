package com.jorge.customers;

import com.jorge.customers.api.CustomersApiDelegate;
import com.jorge.customers.model.CustomerRequest;
import com.jorge.customers.model.CustomerResponse;
import com.jorge.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomerApiDelegateImpl implements CustomersApiDelegate {
    private final CustomerService customerService;

    @Override
    public Mono<CustomerResponse> createCustomer(Mono<CustomerRequest> customerRequest, ServerWebExchange exchange) {
        return customerRequest.flatMap(customerService::createCustomer);
    }

    @Override
    public Mono<Void> deleteCustomerByDni(String id, ServerWebExchange exchange) {
        return customerService.deleteCustomerByDni(id);
    }

    @Override
    public Flux<CustomerResponse> getAllCustomers(ServerWebExchange exchange) {
        return customerService.getAllCustomers();
    }

    @Override
    public Mono<CustomerResponse> getCustomerByDni(String dni, ServerWebExchange exchange) {
        return customerService.getCustomerByDni(dni);
    }

    @Override
    public Mono<CustomerResponse> updateCustomerByDni(String dni, Mono<CustomerRequest> customerRequest, ServerWebExchange exchange) {
        return customerRequest
                .flatMap(customerReq -> customerService.updateCustomerByDni(dni, customerReq));
    }
}
