package com.jorge.gateway.security;

import com.jorge.gateway.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor // Lombok para inyectar dependencias via constructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;
    private final WebClient.Builder webClientBuilder; // Inyecta WebClient.Builder

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        // 1. Validar el token JWT (firma, expiración)
        if (!jwtUtil.validateToken(authToken)) {
            return Mono.error(new BadCredentialsException("Invalid or expired JWT token"));
        }

        // 2. Extraer dni (subject)
        String dni;
        try {
            dni = jwtUtil.getDniFromToken(authToken);
        } catch (Exception e) {
            return Mono.error(new BadCredentialsException("Failed to extract dni from token"));
        }

        if (dni == null || dni.isEmpty()) {
            return Mono.error(new BadCredentialsException("Username not found in token"));
        }

        // 3. Llamar al Customer Service para verificar la existencia del usuario
        return checkCustomerExists(dni)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        // 4. Si existe, crear Authentication object autenticado
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                dni,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // Rol básico
                        );
                        return Mono.just(auth).cast(Authentication.class);
                    } else {
                        // 5. Si no existe, rechazar
                        return Mono.error(new BadCredentialsException("User not found: " + dni));
                    }
                })
                .onErrorMap(WebClientResponseException.class, ex ->
                        new BadCredentialsException("Error contacting customer service", ex)
                )
                .onErrorResume(e -> Mono.error(new BadCredentialsException("Authentication failed", e)));
    }

    private Mono<Boolean> checkCustomerExists(String dni) {
        WebClient client = webClientBuilder.baseUrl("http://localhost:8080").build();
        return client.get()
                .uri("/customers/dni/" + dni)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(WebClientResponseException.NotFound.class, false)
                .onErrorResume(e -> !(e instanceof WebClientResponseException.NotFound), Mono::error);
    }
}
