package com.onthi.v_edu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, Authorization headers, etc.)
        config.setAllowCredentials(true);

        // Explicitly allow the frontend domains that will call this API.
        // Replace / add any other front-end origins you use (including admin domain).
        List<String> allowedOrigins = new ArrayList<>();
        allowedOrigins.add("https://onthi.vuxuanlam.me");
        allowedOrigins.add("https://admin.vuxuanlam.me");
        allowedOrigins.add("https://api.vuxuanlam.me");
        // Local dev (optional)
        allowedOrigins.add("http://localhost:3000");

        // Use allowed origin patterns to allow exact hosts. Do NOT use "*" together with credentials.
        config.setAllowedOriginPatterns(allowedOrigins);

        // Allow all headers and methods
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Register CorsFilter with highest precedence so CORS headers are added even on error responses
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);

        List<String> allowedOrigins = new ArrayList<>();
        allowedOrigins.add("https://onthi.vuxuanlam.me");
        allowedOrigins.add("https://admin.vuxuanlam.me");
        allowedOrigins.add("https://api.vuxuanlam.me");
        allowedOrigins.add("http://localhost:3000");
        config.setAllowedOriginPatterns(allowedOrigins);

        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);

        source.registerCorsConfiguration("/**", config);

        CorsFilter corsFilter = new CorsFilter(source);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
