package com.elbuensabor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private HybridBearerTokenResolver hybridBearerTokenResolver;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // HABILITAR CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Deshabilitar CSRF para APIs REST
                .csrf(csrf -> csrf.disable())

                // Configurar gestión de sesiones
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Agregar nuestro filtro JWT personalizado PRIMERO
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Configurar OAuth2 Resource Server para Auth0 con nuestro resolver personalizado
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(hybridBearerTokenResolver) // Usar nuestro resolver
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new Auth0JwtAuthenticationConverter())
                        )
                )

                // Configurar autorización de endpoints
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (no requieren autenticación)
                        .requestMatchers("/api/clientes/register").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/validate").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth0/**").authenticated()
                        .requestMatchers("/api/clientes").permitAll() // SOLO PARA TESTING
                        .requestMatchers("/api/clientes/**").permitAll() // SOLO PARA TESTING
                        .requestMatchers("/api/categorias/**").permitAll()
                        .requestMatchers("/api/articulos-insumo/**").permitAll()
                        .requestMatchers("/api/unidades-medida/**").permitAll()
                        .requestMatchers("/api/articulos-manufacturados/**").permitAll()

                        // Endpoints de MercadoPago
                        .requestMatchers("/payment/**").permitAll()
                        .requestMatchers("/webhooks/mercadopago").permitAll()

                        // Permitir OPTIONS requests (preflight)
                        .requestMatchers("OPTIONS", "/**").permitAll()

                        // Todos los demás endpoints requieren autenticación
                        .anyRequest().authenticated()
                )

                // Deshabilitar autenticación básica y form login
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}