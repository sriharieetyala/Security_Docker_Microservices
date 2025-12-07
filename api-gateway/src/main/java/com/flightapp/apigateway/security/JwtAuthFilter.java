package com.flightapp.apigateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        log.info(" Incoming: {} {}", method, path);

        // 1) Public endpoints — always allow
        if (isPublic(path, method)) {
            log.info(" Public endpoint allowed");
            return chain.filter(exchange);
        }

        // 2) Require Authorization header for secured endpoints
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn(" Missing or invalid Authorization header");
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        Claims claims;
        try {
            jwtUtil.validateToken(token);
            claims = jwtUtil.extractAllClaims(token);
        } catch (Exception e) {
            log.warn(" JWT invalid: {}", e.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String username = claims.getSubject();
        String role = jwtUtil.extractRole(token);
        log.info(" Authenticated: {} [{}]", username, role);

        // 3) Admin-only endpoints
        if (isAdminOnly(path, method)) {
            if (!"ADMIN".equals(role)) {
                log.warn(" Forbidden - ADMIN only");
                return onError(exchange, HttpStatus.FORBIDDEN);
            }
        }

        // 4) Add user info to downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Name", username)
                .header("X-User-Role", role != null ? role : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (path.startsWith("/auth/")) {
            return true;
        }
        if (method == HttpMethod.GET && path.startsWith("/flight-service/flights")) {
            return true;
        }
        if (method == HttpMethod.POST && path.startsWith("/flight-service/flights/search")) {
            return true;
        }
        return false;
    }

    private boolean isAdminOnly(String path, HttpMethod method) {
        return (path.equals("/flight-service/flights")
                && method == HttpMethod.POST);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Filter early
    }
}
