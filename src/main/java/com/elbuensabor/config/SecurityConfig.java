package com.elbuensabor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
<<<<<<< HEAD
=======
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
>>>>>>> ramaLucho

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
<<<<<<< HEAD
=======

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
>>>>>>> ramaLucho
                .authorizeHttpRequests(auth -> auth
                        // ========== ENDPOINTS PÚBLICOS ==========

                        // Sistema Clásico - Completamente público
                        .requestMatchers("/api/clientes/register").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
<<<<<<< HEAD
                        .requestMatchers("/api/auth/validate-classic").permitAll()

                        // Otros endpoints públicos
=======
                        .requestMatchers("/api/auth/validate").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth0/**").authenticated()
                        .requestMatchers("/api/clientes").permitAll() // SOLO PARA TESTING
                        .requestMatchers("/api/clientes/**").permitAll() // SOLO PARA TESTING
>>>>>>> ramaLucho
                        .requestMatchers("/api/categorias/**").permitAll()
                        .requestMatchers("/api/articulos-insumo/**").permitAll()
                        .requestMatchers("/api/unidades-medida/**").permitAll()
                        .requestMatchers("/api/articulos-manufacturados/**").permitAll()

<<<<<<< HEAD
                        // CRUD de clientes - público para testing
                        .requestMatchers("/api/clientes").permitAll()
                        .requestMatchers("/api/clientes/{id}").permitAll()
=======
                        // Endpoints de MercadoPago
                        .requestMatchers("/payment/**").permitAll()
                        .requestMatchers("/webhooks/mercadopago").permitAll()

                        // Permitir OPTIONS requests (preflight)
                        .requestMatchers("OPTIONS", "/**").permitAll()
>>>>>>> ramaLucho

                        // ========== ENDPOINTS QUE REQUIEREN AUTH0 ==========

                        // Sistema Auth0 - Requiere JWT válido
                        .requestMatchers("/api/auth/register").authenticated()  // Completar perfil
                        .requestMatchers("/api/auth/me").authenticated()       // Info usuario Auth0
                        .requestMatchers("/api/auth/validate").authenticated() // Validar Auth0
                        .requestMatchers("/api/clientes/me").authenticated()   // Perfil actual

                        // Todas las demás rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                // Solo aplicar OAuth2 a rutas específicas
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("permissions");
        converter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}