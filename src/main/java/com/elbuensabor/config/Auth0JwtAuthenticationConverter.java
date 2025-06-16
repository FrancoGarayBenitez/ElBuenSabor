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

@Component
public class Auth0JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(Auth0JwtAuthenticationConverter.class);
    private static final String NAMESPACE = "https://APIElBuenSabor";
    private static final String ROLES_CLAIM = NAMESPACE + "/roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            logger.info("=== AUTH0 JWT PROCESSING ===");
            logger.info("JWT Subject: {}", jwt.getSubject());
            logger.info("JWT Issuer: {}", jwt.getIssuer());
            logger.info("JWT Claims: {}", jwt.getClaims());

            Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);
            logger.info("Extracted authorities: {}", authorities);

            return new JwtAuthenticationToken(jwt, authorities);
        } catch (Exception e) {
            logger.error("Error processing Auth0 JWT: ", e);
            throw e;
        }
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        try {
            logger.info("Looking for roles in claim: {}", ROLES_CLAIM);

            // Extraer roles del custom claim que definiste en Auth0
            Object rolesObj = jwt.getClaim(ROLES_CLAIM);
            logger.info("Roles object: {} (type: {})", rolesObj,
                    rolesObj != null ? rolesObj.getClass().getSimpleName() : "null");

            if (rolesObj instanceof List) {
                List<?> roles = (List<?>) rolesObj;
                logger.info("Found roles list: {}", roles);

                return roles.stream()
                        .map(role -> {
                            String roleStr = role.toString().toUpperCase();
                            String finalRole = roleStr.startsWith("ROLE_") ?
                                    roleStr : "ROLE_" + roleStr;
                            logger.info("Mapping role: {} -> {}", role, finalRole);
                            return new SimpleGrantedAuthority(finalRole);
                        })
                        .collect(Collectors.toList());
            }

            // Si no hay roles específicos, asignar rol de cliente por defecto
            logger.warn("No roles found, assigning default ROLE_CLIENTE");
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENTE"));

        } catch (Exception e) {
            logger.error("Error extracting authorities: ", e);
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        }
    }
}