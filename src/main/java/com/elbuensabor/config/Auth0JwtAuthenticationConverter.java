package com.elbuensabor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convertidor de JWT de Auth0 a Authentication de Spring Security
 */
@Component
public class Auth0JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(Auth0JwtAuthenticationConverter.class);

    // Namespace configurado en Auth0 Action - debe coincidir exactamente
    private static final String NAMESPACE = "https://APIElBuenSabor";
    private static final String ROLES_CLAIM = NAMESPACE + "/roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            logger.debug("Converting Auth0 JWT for user: {}", jwt.getSubject());

            // Extraer authorities (roles) del JWT
            Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

            logger.debug("Extracted authorities for user {}: {}", jwt.getSubject(), authorities);

            // Crear y retornar token de autenticación
            return new JwtAuthenticationToken(jwt, authorities);

        } catch (Exception e) {
            logger.error("Error converting Auth0 JWT for user {}: {}", jwt.getSubject(), e.getMessage());

            // En caso de error, asignar rol de cliente por defecto
            Collection<SimpleGrantedAuthority> defaultAuthorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENTE"));

            logger.warn("Using default CLIENTE role due to error for user: {}", jwt.getSubject());
            return new JwtAuthenticationToken(jwt, defaultAuthorities);
        }
    }

    /**
     * Extrae los roles del JWT y los convierte a Spring Security authorities
     */
    private Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        try {
            // Obtener el claim de roles del JWT
            Object rolesObject = jwt.getClaim(ROLES_CLAIM);

            if (rolesObject == null) {
                logger.debug("No roles claim found for user: {}, using default CLIENTE role", jwt.getSubject());
                return getDefaultAuthorities();
            }

            // Verificar que sea una lista
            if (!(rolesObject instanceof List)) {
                logger.warn("Roles claim is not a list for user: {}, found: {}", jwt.getSubject(), rolesObject.getClass().getSimpleName());

                // Intentar convertir a String si no es lista
                if (rolesObject instanceof String) {
                    String roleString = (String) rolesObject;
                    String authority = roleString.trim().toUpperCase();
                    authority = authority.startsWith("ROLE_") ? authority : "ROLE_" + authority;
                    return Collections.singletonList(new SimpleGrantedAuthority(authority));
                }

                return getDefaultAuthorities();
            }

            List<?> roles = (List<?>) rolesObject;

            if (roles.isEmpty()) {
                logger.debug("Empty roles list for user: {}, using default CLIENTE role", jwt.getSubject());
                return getDefaultAuthorities();
            }

            // Convertir roles a authorities
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> {
                        String roleString = role.toString().trim().toUpperCase();

                        // Asegurar que tenga el prefijo ROLE_
                        String authority = roleString.startsWith("ROLE_") ?
                                roleString : "ROLE_" + roleString;

                        return new SimpleGrantedAuthority(authority);
                    })
                    .collect(Collectors.toList());

            logger.debug("Successfully extracted {} authorities from roles: {}", authorities.size(), roles);
            return authorities;

        } catch (Exception e) {
            logger.error("Error extracting authorities from JWT: {}", e.getMessage());
            return getDefaultAuthorities();
        }
    }

    /**
     * Retorna las authorities por defecto cuando no se pueden extraer roles
     */
    private Collection<SimpleGrantedAuthority> getDefaultAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }
}