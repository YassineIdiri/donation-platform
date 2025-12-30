package com.yassine.smartexpensetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Front Angular
        config.setAllowedOrigins(List.of("http://localhost:4200"));

        // IMPORTANT: méthodes, y compris OPTIONS (préflight)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // IMPORTANT: Authorization pour Bearer JWT
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Optionnel: si tu veux lire certains headers côté front
        config.setExposedHeaders(List.of("Authorization"));

        // JWT Bearer => pas besoin de cookies
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
