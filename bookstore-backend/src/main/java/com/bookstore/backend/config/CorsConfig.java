package com.bookstore.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // Allow any origin pattern
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("*"));
        // Allow any header
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        // Allow any method
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
        // Allow credentials
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}
