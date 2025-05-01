package com.jorge.gateway.config;

import com.jorge.gateway.security.JwtReactiveAuthenticationManager;
import com.jorge.gateway.security.web.BearerTokenServerAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtReactiveAuthenticationManager authenticationManager;
    private final BearerTokenServerAuthenticationConverter authenticationConverter;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        // 1. Crear el filtro de autenticación personalizado
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        http    // 2. Deshabilitar mecanismos innecesarios (CSRF, Form Login, HTTP Basic)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // 3. Configurar manejo de excepciones de autenticación/autorización
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // Entry point para el 401 Unauthorized
                        .authenticationEntryPoint((swe, e) -> {
                            swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return swe.getResponse().setComplete();
                        })
                        // Handler para 403 Forbidden
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )

                // 4. Reglas de autorización
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/customers/**").permitAll()
                                .anyExchange().authenticated())
                // 5. Añadir filtro JWT personalizado ANTES del filtro de autorización
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}
