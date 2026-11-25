package com.stockpurchaseorder.backend.auth.config;

import com.stockpurchaseorder.backend.auth.service.AccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.util.Base64;

@Configuration
public class AccessTokenConfig {
    @Bean
    AccessTokenService accessTokenService(@Value("${stockflow.auth.jwt-secret-base64}") String secret) {
        return new AccessTokenService(Base64.getDecoder().decode(secret), Clock.systemUTC());
    }
}
