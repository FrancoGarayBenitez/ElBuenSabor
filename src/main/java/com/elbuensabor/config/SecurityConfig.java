package com.elbuensabor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Agregamos esto
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    private final CorsConfig corsConfig;
    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    public SecurityConfig(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

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

    // Tu bean jwtDecoder() no necesita cambios
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    // 2. BEAN AÑADIDO PARA "TRADUCIR" LOS ROLES DEL TOKEN DE AUTH0
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Creamos un conversor para las "autoridades" (roles) basadas en el token JWT.
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Le decimos que busque los roles en nuestro claim personalizado.
        // ¡DEBE SER EXACTAMENTE EL MISMO NAMESPACE QUE USASTE EN LA ACTION DE AUTH0!
        grantedAuthoritiesConverter.setAuthoritiesClaimName("https://elbuensabor.com/roles");

        // Le decimos que añada el prefijo "ROLE_" a cada rol, que es el estándar de Spring Security.
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        // Creamos el conversor principal de autenticación JWT y le asignamos nuestro conversor de roles.
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    // Validador de audiencia personalizado
    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;

        public AudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (jwt.getAudience().contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_audience", "The required audience is missing", null));
        }
    }
}