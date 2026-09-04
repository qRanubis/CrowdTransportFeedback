package com.example.crowdtransportfeedback.security;
import org.springframework.boot.context.properties.ConfigurationProperties; import java.time.Duration;
@ConfigurationProperties("app.jwt") public record JwtProperties(String secret, Duration accessLifetime) {}
