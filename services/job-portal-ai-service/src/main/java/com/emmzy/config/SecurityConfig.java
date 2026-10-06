package com.emmzy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * Main Spring Security configuration.
     *
     * The AI service is a REST API consumed by the React frontend
     * and by other services in the job portal architecture.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * Enable CORS using the CorsConfigurationSource bean below.
                 */
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                /*
                 * CSRF is not needed for this stateless REST API.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * Allow browser preflight requests.
                 *
                 * The frontend sends OPTIONS requests before certain
                 * cross-origin requests, especially when Authorization
                 * and custom headers are present.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /*
                         * AI endpoints.
                         *
                         * Authentication/authorization can be tightened
                         * later once the AI service's JWT validation is
                         * fully connected.
                         */
                        .requestMatchers("/api/ai/**").permitAll()

                        /*
                         * Allow Spring Boot's basic error endpoint so that
                         * errors can be returned normally during development.
                         */
                        .requestMatchers("/error").permitAll()

                        /*
                         * Everything else requires authentication.
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * We are not using Spring Security's generated login page
                 * or HTTP Basic authentication for the frontend API.
                 */
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }


    /**
     * CORS configuration for the React frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        /*
         * React/Vite development server.
         */
        configuration.setAllowedOrigins(
                List.of("http://localhost:3000")
        );

        /*
         * Methods currently used by the frontend and REST API.
         */
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * Headers sent by apiClient.js.
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-User-Email",
                        "X-User-Id"
                )
        );

        /*
         * The frontend does not currently rely on browser cookies for
         * authentication, so credentials are not required here.
         */
        configuration.setAllowCredentials(false);

        /*
         * Cache the preflight response for one hour during development.
         */
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
