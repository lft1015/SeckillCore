package com.reditickets.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关认证配置属性
 * <p>
 * 绑定 application.yml 中 gateway.auth 前缀的配置，
 * 包括 JWT 认证白名单路径列表
 * </p>
 *
 * @author gugu
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /** 白名单路径列表，匹配的路径跳过 JWT 认证 */
    private List<String> whitelist;
}