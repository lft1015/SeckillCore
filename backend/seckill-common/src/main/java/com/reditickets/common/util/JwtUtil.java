package com.reditickets.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 提供 JWT Token 的生成、解析、校验功能。
 * 使用 HMAC-SHA256 签名算法，密钥和过期时间从配置文件读取
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    /**
     * 获取签名密钥
     * <p>
     * 使用 HMAC-SHA256 算法，密钥长度至少需要 256 bits（32 字节）
     * </p>
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * <p>
     * 将 userId 和 username 作为 claims 写入 Token 体，
     * 并设置签发时间和过期时间
     * </p>
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成 JWT 刷新令牌
     * <p>
     * 刷新令牌有效期 7 天，仅用于续期访问令牌，不包含业务数据
     * </p>
     *
     * @param userId 用户ID
     * @return JWT 刷新令牌字符串
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token 并返回 Claims
     *
     * @param token JWT Token 字符串
     * @return Token 中的 Claims
     * @throws ExpiredJwtException       Token 已过期
     * @throws MalformedJwtException     Token 格式错误
     * @throws SignatureException        Token 签名无效
     * @throws UnsupportedJwtException   Token 不被支持
     * @throws IllegalArgumentException Token 为空或非法
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token 字符串
     * @return 用户ID，解析失败返回 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.warn("解析 Token 获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token 字符串
     * @return 用户名，解析失败返回 null
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.warn("解析 Token 获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 Token 是否有效
     * <p>
     * 返回 true 表示 Token 签名正确且未过期
     * </p>
     *
     * @param token JWT Token 字符串
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Token 签名无效: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token 格式错误: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Token 不被支持: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token 为空或非法: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     *
     * @param token JWT Token 字符串
     * @return 剩余毫秒数，Token 无效返回 -1
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return -1;
        }
    }
}