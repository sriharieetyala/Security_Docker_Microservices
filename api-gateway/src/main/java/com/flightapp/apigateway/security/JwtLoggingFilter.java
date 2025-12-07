package com.flightapp.apigateway.security;



import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoggingFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info(" Request to: {}", path);

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                jwtUtil.validateToken(token);
                Claims claims = jwtUtil.extractAllClaims(token);

                log.info(" Authenticated user: {}, role: {}",
                        claims.getSubject(),
                        claims.get("role"));

            } catch (Exception ex) {
                log.warn(" Invalid JWT: {}", ex.getMessage());
            }
        } else {
            log.info(" No Auth header found (public or unauthenticated request)");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // run early
    }
}
