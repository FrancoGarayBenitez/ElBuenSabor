package com.elbuensabor.config;

import com.elbuensabor.entities.Usuario;
import com.elbuensabor.repository.IUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Interceptor para validar que solo usuarios activos puedan acceder
 */
@Component
public class ActiveUserInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ActiveUserInterceptor.class);

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Solo validar en endpoints protegidos (excluir públicos)
        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            return true;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String auth0Id = jwt.getSubject();

                // Buscar usuario en base de datos
                Optional<Usuario> usuarioOpt = usuarioRepository.findByAuth0Id(auth0Id);

                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();

                    if (!usuario.isActivo()) {
                        logger.warn("❌ Usuario desactivado intentando acceder: {} - {}", auth0Id, usuario.getEmail());

                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("""
                            {
                                "error": "Usuario desactivado",
                                "message": "Tu cuenta ha sido desactivada. Contacta al administrador.",
                                "code": "USER_DEACTIVATED",
                                "timestamp": "%s"
                            }
                            """.formatted(java.time.Instant.now().toString()));

                        return false; // Bloquear acceso
                    }

                    logger.debug("✅ Usuario activo validado: {}", usuario.getEmail());
                } else {
                    logger.debug("Usuario no encontrado en BD, permitir acceso (posible usuario nuevo)");
                }
            }

        } catch (Exception e) {
            logger.error("Error validando usuario activo: {}", e.getMessage());
            // En caso de error, permitir acceso para no bloquear la aplicación
        }

        return true; // Permitir acceso
    }

    /**
     * Verifica si el endpoint es público (no requiere validación de usuario activo)
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth0/register") ||
                requestURI.startsWith("/api/categorias") ||
                requestURI.startsWith("/api/articulos-insumo") ||
                requestURI.startsWith("/api/unidades-medida") ||
                requestURI.startsWith("/api/articulos-manufacturados") ||
                requestURI.startsWith("/payment") ||
                requestURI.startsWith("/webhooks") ||
                requestURI.startsWith("/img") ||
                requestURI.startsWith("/static") ||
                requestURI.startsWith("/api/images") ||
                requestURI.contains("/debug"); // Endpoints de debug
    }
}