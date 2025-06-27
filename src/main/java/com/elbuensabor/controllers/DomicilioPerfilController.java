package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.DomicilioRequestDTO;
import com.elbuensabor.dto.response.DomicilioResponseDTO;
import com.elbuensabor.services.IDomicilioPerfilService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para gestión de domicilios desde el perfil del usuario
 * Todas las operaciones están restringidas al usuario autenticado
 *
 * Rutas: /api/perfil/domicilios/*
 */
@RestController
@RequestMapping("/api/perfil/domicilios")
public class DomicilioPerfilController {

    private static final Logger logger = LoggerFactory.getLogger(DomicilioPerfilController.class);
    private final IDomicilioPerfilService domicilioPerfilService;

    @Autowired
    public DomicilioPerfilController(IDomicilioPerfilService domicilioPerfilService) {
        this.domicilioPerfilService = domicilioPerfilService;
    }

    /**
     * GET /api/perfil/domicilios
     * Obtiene todos los domicilios del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<List<DomicilioResponseDTO>> getMisDomicilios(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} requesting their domicilios", jwt.getSubject());

        List<DomicilioResponseDTO> domicilios = domicilioPerfilService.getMisDomicilios(jwt.getSubject());

        logger.debug("Returning {} domicilios for user {}", domicilios.size(), jwt.getSubject());
        return ResponseEntity.ok(domicilios);
    }

    /**
     * GET /api/perfil/domicilios/principal
     * Obtiene el domicilio principal del usuario autenticado
     */
    @GetMapping("/principal")
    public ResponseEntity<DomicilioResponseDTO> getMiDomicilioPrincipal(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} requesting their principal domicilio", jwt.getSubject());

        DomicilioResponseDTO domicilioPrincipal = domicilioPerfilService.getMiDomicilioPrincipal(jwt.getSubject());

        if (domicilioPrincipal != null) {
            return ResponseEntity.ok(domicilioPrincipal);
        } else {
            logger.debug("User {} has no principal domicilio", jwt.getSubject());
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * GET /api/perfil/domicilios/{id}
     * Obtiene un domicilio específico del usuario autenticado
     */
    @GetMapping("/{id}")
    public ResponseEntity<DomicilioResponseDTO> getMiDomicilio(@PathVariable Long id,
                                                               @AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} requesting domicilio {}", jwt.getSubject(), id);

        DomicilioResponseDTO domicilio = domicilioPerfilService.getMiDomicilio(jwt.getSubject(), id);
        return ResponseEntity.ok(domicilio);
    }

    /**
     * POST /api/perfil/domicilios
     * Crea un nuevo domicilio para el usuario autenticado
     */
    @PostMapping
    public ResponseEntity<DomicilioResponseDTO> crearMiDomicilio(@Valid @RequestBody DomicilioRequestDTO domicilioDTO,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} creating new domicilio", jwt.getSubject());

        DomicilioResponseDTO nuevoDomicilio = domicilioPerfilService.crearMiDomicilio(jwt.getSubject(), domicilioDTO);

        logger.debug("Created domicilio {} for user {}", nuevoDomicilio.getIdDomicilio(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDomicilio);
    }

    /**
     * PUT /api/perfil/domicilios/{id}
     * Actualiza un domicilio del usuario autenticado
     */
    @PutMapping("/{id}")
    public ResponseEntity<DomicilioResponseDTO> actualizarMiDomicilio(@PathVariable Long id,
                                                                      @Valid @RequestBody DomicilioRequestDTO domicilioDTO,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} updating domicilio {}", jwt.getSubject(), id);

        DomicilioResponseDTO domicilioActualizado = domicilioPerfilService.actualizarMiDomicilio(
                jwt.getSubject(), id, domicilioDTO);

        logger.debug("Updated domicilio {} for user {}", id, jwt.getSubject());
        return ResponseEntity.ok(domicilioActualizado);
    }

    /**
     * DELETE /api/perfil/domicilios/{id}
     * Elimina un domicilio del usuario autenticado
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMiDomicilio(@PathVariable Long id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} deleting domicilio {}", jwt.getSubject(), id);

        domicilioPerfilService.eliminarMiDomicilio(jwt.getSubject(), id);

        logger.debug("Deleted domicilio {} for user {}", id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/perfil/domicilios/{id}/principal
     * Marca un domicilio específico como principal
     */
    @PatchMapping("/{id}/principal")
    public ResponseEntity<DomicilioResponseDTO> marcarComoPrincipal(@PathVariable Long id,
                                                                    @AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} marking domicilio {} as principal", jwt.getSubject(), id);

        DomicilioResponseDTO domicilioPrincipal = domicilioPerfilService.marcarComoPrincipal(jwt.getSubject(), id);

        logger.debug("Marked domicilio {} as principal for user {}", id, jwt.getSubject());
        return ResponseEntity.ok(domicilioPrincipal);
    }

    /**
     * GET /api/perfil/domicilios/estadisticas
     * Obtiene estadísticas de domicilios del usuario (cantidad, etc.)
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticasDomicilios(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("User {} requesting domicilios statistics", jwt.getSubject());

        long cantidad = domicilioPerfilService.contarMisDomicilios(jwt.getSubject());
        DomicilioResponseDTO principal = domicilioPerfilService.getMiDomicilioPrincipal(jwt.getSubject());

        Map<String, Object> estadisticas = Map.of(
                "cantidadTotal", cantidad,
                "tienePrincipal", principal != null,
                "domicilioPrincipal", principal != null ? principal : Map.of()
        );

        return ResponseEntity.ok(estadisticas);
    }
}