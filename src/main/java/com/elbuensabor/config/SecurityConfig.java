package com.elbuensabor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Agregamos esto
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("=== CREATING SECURITY FILTER CHAIN ===");

        http
                // Habilitar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Deshabilitar CSRF para APIs REST
                .csrf(csrf -> csrf.disable())

                // Configurar gestión de sesiones como STATELESS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configurar OAuth2 Resource Server SOLO para Auth0
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("=== AUTH ENTRY POINT TRIGGERED ===");
                            System.out.println("Request: " + request.getRequestURI());
                            System.out.println("Method: " + request.getMethod());
                            System.out.println("Auth Exception: " + authException.getMessage());
                            response.sendError(401, "Unauthorized");
                        })
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new Auth0JwtAuthenticationConverter())
                        )
                )

                // Configurar autorización de endpoints
                .authorizeHttpRequests(auth -> {
                    System.out.println("=== CONFIGURING AUTHORIZATION ===");
                    auth
                            // Endpoints públicos (no requieren autenticación)
                            .requestMatchers("/api/auth0/register").permitAll()
                            .requestMatchers("/api/categorias/**").permitAll()
                            .requestMatchers("/api/articulos-insumo/**").permitAll()
                            .requestMatchers("/api/unidades-medida/**").permitAll()
                            .requestMatchers("/api/articulos-manufacturados/**").permitAll()

                            // Endpoints de MercadoPago (públicos)
                            .requestMatchers("/payment/**").permitAll()
                            .requestMatchers("/webhooks/mercadopago").permitAll()

                            // Permitir OPTIONS requests (preflight CORS)
                            .requestMatchers("OPTIONS", "/**").permitAll()

                            // Endpoints que requieren autenticación
                            .requestMatchers("/api/clientes/**").authenticated()
                            .requestMatchers("/api/pedidos/**").authenticated()
                            .requestMatchers("/api/auth0/login").authenticated()
                            .requestMatchers("/api/auth0/me").authenticated()
                            .requestMatchers("/api/auth0/validate").authenticated()
                            .requestMatchers("/api/auth0/profile").authenticated()

                            // Endpoints de admin (requieren rol ADMIN)
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")

                            // Todos los demás endpoints requieren autenticación
                            .anyRequest().authenticated();
                })

                // Deshabilitar autenticación básica y form login
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        System.out.println("=== SECURITY FILTER CHAIN CREATED ===");
        return http.build();
    }
}