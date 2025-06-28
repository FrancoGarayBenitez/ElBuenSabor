package com.elbuensabor.config;

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
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

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
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new Auth0JwtAuthenticationConverter()))
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(
                                "/api/auth0/register",
                                "/api/categorias/**",
                                "/api/articulos-insumo/**",
                                "/api/unidades-medida/**",
                                "/api/articulos-manufacturados/**",
                                "/payment/**",
                                "/webhooks/mercadopago",
                                "/api/compras-insumo/**"

                                ).permitAll()

                        // CORS preflight
                        .requestMatchers("OPTIONS", "/**").permitAll()

                        // Endpoints autenticados
                        .requestMatchers(
                                "/api/clientes/**",
                                "/api/pedidos/**",
                                "/api/auth0/login",
                                "/api/auth0/me",
                                "/api/auth0/validate",
                                "/api/auth0/profile",
                                "/api/auth0/complete-profile",
                                "/api/auth0/refresh-roles"
                        ).authenticated()

                        // Endpoints de admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Resto requiere autenticación
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .build();
    }
}