package com.jorge.customers.expose;

import com.jorge.customers.api.CustomersApiDelegate;
import com.jorge.customers.model.CustomerRequest;
import com.jorge.customers.model.CustomerResponse;
import com.jorge.customers.model.LoginResponse;
import com.jorge.customers.model.ProductSummaryResponse;
import com.jorge.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomerApiDelegateImpl implements CustomersApiDelegate{
    private final CustomerService customerService;

    @Override
    public Mono<CustomerResponse> getCustomerByDni(String dni, ServerWebExchange exchange) {
        return customerService.getCustomerByDni(dni);
    }

    @Override
    public Mono<CustomerResponse> createCustomer(Mono<CustomerRequest> customerRequest, ServerWebExchange exchange) {
        return customerRequest.flatMap(customerService::createCustomer);
    }

    @Override
    public Mono<Void> deleteCustomerById(String id, ServerWebExchange exchange) {
        return customerService.deleteCustomerById(id);
    }

    @Override
    public Flux<CustomerResponse> getAllCustomers(ServerWebExchange exchange) {
        return customerService.getAllCustomers();
    }

    @Override
    public Mono<CustomerResponse> getCustomerById(String id, ServerWebExchange exchange) {
        return customerService.getCustomerById(id);
    }

    @Override
    public Mono<CustomerResponse> updateCustomerById(String id, Mono<CustomerRequest> customerRequest, ServerWebExchange exchange) {
        return customerRequest
                .flatMap(customerReq -> customerService.updateCustomerById(id, customerReq));
    }

    @Override
    public Mono<ProductSummaryResponse> getCustomerProductSummaryById(String id, ServerWebExchange exchange) {
        return customerService.getProductSummaryByCustomerId(id);
    }

    @Override
    public Mono<LoginResponse> loginCustomer(String dni, ServerWebExchange exchange) {
        return customerService.login(dni);
    }
}
