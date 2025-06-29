package com.elbuensabor.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Servicio para sincronizar roles con Auth0
 */
@Service
public class Auth0RoleSyncService {

    private static final Logger logger = LoggerFactory.getLogger(Auth0RoleSyncService.class);

    @Value("${auth0.domain:dev-ik2kub20ymu4sfpr.us.auth0.com}")
    private String auth0Domain;

    @Value("${auth0.management.client-id:}")
    private String managementClientId;

    @Value("${auth0.management.client-secret:}")
    private String managementClientSecret;

    @Value("${auth0.management.audience:}")
    private String managementAudience;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Obtiene un access token para la Management API de Auth0
     */
    private String getManagementToken() {
        try {
            String url = String.format("https://%s/oauth/token", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of(
                    "client_id", managementClientId,
                    "client_secret", managementClientSecret,
                    "audience", managementAudience,
                    "grant_type", "client_credentials"
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            throw new RuntimeException("No se pudo obtener token de Management API");

        } catch (Exception e) {
            logger.error("Error obteniendo token de Management API: {}", e.getMessage());
            throw new RuntimeException("Error conectando con Auth0", e);
        }
    }

    /**
     * Asigna un rol a un usuario en Auth0
     */
    public void assignRoleToUser(String auth0UserId, String roleName) {
        try {
            // Primero obtener el ID del rol en Auth0
            String roleId = getRoleIdByName(roleName);
            if (roleId == null) {
                logger.warn("Rol {} no encontrado en Auth0, creándolo...", roleName);
                roleId = createRole(roleName);
            }

            // Asignar rol al usuario
            String token = getManagementToken();
            String url = String.format("https://%s/api/v2/users/%s/roles", auth0Domain, auth0UserId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, Object> body = Map.of("roles", List.of(roleId));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("✅ Rol {} asignado exitosamente a usuario {} en Auth0", roleName, auth0UserId);
            } else {
                logger.warn("⚠️ Respuesta inesperada de Auth0: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("❌ Error asignando rol {} a usuario {} en Auth0: {}", roleName, auth0UserId, e.getMessage());
            // No lanzar excepción para no bloquear el flujo principal
        }
    }

    /**
     * Remueve todos los roles de un usuario y asigna el nuevo
     */
    public void syncUserRole(String auth0UserId, String newRole) {
        try {
            // Primero remover todos los roles existentes
            removeAllUserRoles(auth0UserId);

            // Luego asignar el nuevo rol
            assignRoleToUser(auth0UserId, newRole);

            logger.info("✅ Rol sincronizado en Auth0 para usuario {}: {}", auth0UserId, newRole);

        } catch (Exception e) {
            logger.error("❌ Error sincronizando rol en Auth0: {}", e.getMessage());
        }
    }

    /**
     * Remueve todos los roles de un usuario
     */
    private void removeAllUserRoles(String auth0UserId) {
        try {
            String token = getManagementToken();

            // Primero obtener roles actuales del usuario
            String getUserRolesUrl = String.format("https://%s/api/v2/users/%s/roles", auth0Domain, auth0UserId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<List> rolesResponse = restTemplate.exchange(getUserRolesUrl, HttpMethod.GET, getRequest, List.class);

            if (rolesResponse.getBody() != null && !rolesResponse.getBody().isEmpty()) {
                List<Map<String, Object>> currentRoles = (List<Map<String, Object>>) rolesResponse.getBody();
                List<String> roleIdsToRemove = currentRoles.stream()
                        .map(role -> (String) role.get("id"))
                        .toList();

                // Remover roles existentes
                String removeRolesUrl = String.format("https://%s/api/v2/users/%s/roles", auth0Domain, auth0UserId);

                HttpHeaders removeHeaders = new HttpHeaders();
                removeHeaders.setContentType(MediaType.APPLICATION_JSON);
                removeHeaders.setBearerAuth(token);

                Map<String, Object> removeBody = Map.of("roles", roleIdsToRemove);
                HttpEntity<Map<String, Object>> removeRequest = new HttpEntity<>(removeBody, removeHeaders);

                restTemplate.exchange(removeRolesUrl, HttpMethod.DELETE, removeRequest, String.class);

                logger.debug("Roles removidos de usuario {} en Auth0", auth0UserId);
            }

        } catch (Exception e) {
            logger.error("Error removiendo roles de usuario en Auth0: {}", e.getMessage());
        }
    }

    /**
     * Obtiene el ID de un rol por su nombre
     */
    private String getRoleIdByName(String roleName) {
        try {
            String token = getManagementToken();
            String url = String.format("https://%s/api/v2/roles?name_filter=%s", auth0Domain, roleName);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                List<Map<String, Object>> roles = (List<Map<String, Object>>) response.getBody();
                Map<String, Object> role = roles.get(0);
                return (String) role.get("id");
            }

            return null;

        } catch (Exception e) {
            logger.error("Error obteniendo ID del rol {}: {}", roleName, e.getMessage());
            return null;
        }
    }

    /**
     * Crea un nuevo rol en Auth0
     */
    private String createRole(String roleName) {
        try {
            String token = getManagementToken();
            String url = String.format("https://%s/api/v2/roles", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, String> body = Map.of(
                    "name", roleName,
                    "description", "Rol " + roleName + " creado desde la aplicación"
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                String roleId = (String) response.getBody().get("id");
                logger.info("✅ Rol {} creado en Auth0 con ID: {}", roleName, roleId);
                return roleId;
            }

            throw new RuntimeException("No se pudo crear el rol en Auth0");

        } catch (Exception e) {
            logger.error("Error creando rol {} en Auth0: {}", roleName, e.getMessage());
            throw new RuntimeException("Error creando rol en Auth0", e);
        }
    }
}