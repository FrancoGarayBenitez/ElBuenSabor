package com.elbuensabor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                            jwt.jwtAuthenticationConverter(new Auth0JwtAuthenticationConverter());
                            // 🔍 DEBUG: Agregar logging para JWT processing
                            logger.debug("🔍 Configuring JWT authentication converter");
                        })
                )
                .authorizeHttpRequests(auth -> {
                    logger.debug("🔍 Configuring authorization rules");
                    auth
                            .requestMatchers(
                                    "/api/auth0/register",
                                    "/api/categorias/**",
                                    "/api/articulos-insumo/**",
                                    "/api/unidades-medida/**",
                                    "/api/articulos-manufacturados/**",
                                    "/payment/**",
                                    "/webhooks/mercadopago",
                                "/api/compras-insumo/**"
,
                                    "/img/**",
                                    "/static/**",
                                    "/api/images/**" //
                                    ).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(
                                    "/api/clientes/**",
                                    "/api/auth/**",
                                    "/api/pedidos/**",
                                    "/api/auth0/login",
                                    "/api/auth0/me",
                                    "/api/auth0/validate",
                                    "/api/auth0/profile",
                                    "/api/auth0/complete-profile",
                                    "/api/auth0/refresh-roles"
                            ).authenticated()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .build();
    }
}