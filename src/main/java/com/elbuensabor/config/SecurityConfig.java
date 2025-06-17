package com.elbuensabor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// 1. ANOTACIÓN AÑADIDA
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <-- 1. HABILITA EL USO DE @PreAuthorize EN TUS CONTROLADORES
public class SecurityConfig {

    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    private final CorsConfig corsConfig;

    public SecurityConfig(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Has movido la protección a nivel de método, podrías dejar todo como autenticado por defecto
                        // o mantener tus endpoints públicos explícitamente. Ambas formas son válidas.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(
                                "/api/categorias/**",
                                "/api/articulos-insumo/**",
                                "/api/unidades-medida/**",
                                "/api/articulos-manufacturados/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                // 3. LE DECIMOS A SPRING QUE USE NUESTRO CONVERSOR DE ROLES
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

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