package com.miraquest.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code miraquest.jwt.secret} (mín. 32 bytes; defina JWT_SECRET em produção) e validade do token em horas. */
@ConfigurationProperties(prefix = "miraquest.jwt")
public record JwtProperties(String secret, long expiracaoHoras) {

}
