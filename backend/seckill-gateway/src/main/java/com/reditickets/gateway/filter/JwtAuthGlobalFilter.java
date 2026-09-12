package com.reditickets.gateway.filter;

import com.reditickets.gateway.config.GatewayAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 全局认证过滤器
 * <p>
 * 在请求到达后端微服务之前进行 JWT Token 校验，白名单路径放行，
 * 认证失败返回 401，认证成功将 userId 写入 X-User-Id 请求头透传
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LEN = BEARER_PREFIX.length();
    private static final String HEADER_USER_ID = "X-User-Id";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final GatewayAuthProperties authProperties;
    private final SecretKey signingKey;

    public JwtAuthGlobalFilter(GatewayAuthProperties authProperties,
                               @Value("${jwt.secret}") String secret) {
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return writeUnauthorized(exchange, "未提供认证信息");
        }

        String token = authHeader.substring(BEARER_PREFIX_LEN);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                return writeUnauthorized(exchange, "Token 中缺少用户信息");
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(HEADER_USER_ID, String.valueOf(userId))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            return writeUnauthorized(exchange, "Token 已过期");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return writeUnauthorized(exchange, "Token 格式错误");
        } catch (SignatureException e) {
            return writeUnauthorized(exchange, "Token 签名无效");
        } catch (Exception e) {
            log.error("[网关JWT] 认证异常: path={}", path, e);
            return writeUnauthorized(exchange, "认证失败");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        if (authProperties.getWhitelist() == null) {
            return false;
        }
        for (String pattern : authProperties.getWhitelist()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}