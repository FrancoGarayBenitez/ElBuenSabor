package com.elbuensabor.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class HybridBearerTokenResolver implements BearerTokenResolver {

    private static final Logger logger = LoggerFactory.getLogger(HybridBearerTokenResolver.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public String resolve(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            logger.info("=== HYBRID TOKEN RESOLVER ===");
            logger.info("Request URI: {}", request.getRequestURI());
            logger.info("Token length: {}", token.length());
            logger.info("Token start: {}", token.substring(0, Math.min(50, token.length())));

            // Si es un token local, no devolver nada para que OAuth2 no lo procese
            if (isLocalToken(token)) {
                logger.info("Detected LOCAL token - OAuth2 will ignore");
                return null; // Esto hace que OAuth2 Resource Server ignore este token
            }

            // Si no es local, devolver el token para que Auth0 lo procese
            logger.info("Detected AUTH0 token - OAuth2 will process");
            return token;
        }

        logger.info("No Bearer token found");
        return null;
    }

    private boolean isLocalToken(String token) {
        try {
            // Intentar extraer información con nuestra clave local
            String username = jwtUtil.extractUsername(token);
            boolean isExpired = jwtUtil.isTokenExpired(token);
            boolean isLocal = username != null && !isExpired;

            logger.info("Local token check - username: {}, expired: {}, isLocal: {}",
                    username, isExpired, isLocal);

            return isLocal;
        } catch (Exception e) {
            logger.info("Local token check failed: {} - Assuming Auth0 token", e.getMessage());
            return false;
        }
    }
}