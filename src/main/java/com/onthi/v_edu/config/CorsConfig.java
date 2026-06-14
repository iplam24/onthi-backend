package com.onthi.v_edu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // Liệt kê chính xác các domain frontend của bạn
        config.setAllowedOriginPatterns(java.util.List.of(
                "https://hoc24h.tech",
                "https://admin.hoc24h.tech",
                "https://onthi.vuxuanlam.me",
                "https://tinnhan.vuxuanlam.me",
                "http://localhost:*"
        ));

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}