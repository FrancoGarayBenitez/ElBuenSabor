package com.elbuensabor.controllers;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;


    // ImageController.java
    @RestController
    @RequestMapping("/api/imagenes")

    public class ImageController {

        @Value("${app.upload.dir:src/main/resources/static/img/}")
        private String uploadDir;

        @Value("${app.base.url:http://localhost:8080}")
        private String baseUrl;

        @PostMapping("/upload")
        public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
            try {
                // Validar que el archivo no esté vacío
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "El archivo está vacío"));
                }

                // Validar tipo de archivo
                String contentType = file.getContentType();
                if (!isValidImageType(contentType)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Tipo de archivo no válido. Solo se permiten: JPG, PNG, GIF, WEBP"));
                }

                // Validar tamaño (máximo 5MB)
                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "El archivo es demasiado grande. Máximo 5MB"));
                }

                // Generar nombre único para el archivo
                String originalFilename = file.getOriginalFilename();
                String fileExtension = getFileExtension(originalFilename);
                String uniqueFilename = generateUniqueFilename(fileExtension);

                // Crear directorio si no existe
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Guardar archivo
                Path filePath = uploadPath.resolve(uniqueFilename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Generar URL
                String imageUrl = baseUrl + "/img/" + uniqueFilename;

                // Crear respuesta con información de la imagen
                Map<String, Object> response = Map.of(
                        "success", true,
                        "filename", uniqueFilename,
                        "url", imageUrl,
                        "originalName", originalFilename,
                        "size", file.getSize(),
                        "contentType", contentType
                );

                return ResponseEntity.ok(response);

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al guardar el archivo: " + e.getMessage()));
            }
        }

        @DeleteMapping("/delete/{filename}")
        public ResponseEntity<?> deleteImage(@PathVariable String filename) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(filename);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Imagen eliminada correctamente"));
                } else {
                    return ResponseEntity.notFound().build();
                }
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al eliminar el archivo: " + e.getMessage()));
            }
        }

        @GetMapping("/validate/{filename}")
        public ResponseEntity<?> validateImage(@PathVariable String filename) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(filename);

                if (Files.exists(filePath)) {
                    String url = baseUrl + "/img/" + filename;
                    return ResponseEntity.ok(Map.of(
                            "exists", true,
                            "url", url,
                            "filename", filename
                    ));
                } else {
                    return ResponseEntity.ok(Map.of("exists", false));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error al validar la imagen: " + e.getMessage()));
            }
        }

        // ==================== MÉTODOS AUXILIARES ====================

        private boolean isValidImageType(String contentType) {
            return contentType != null && (
                    contentType.equals("image/jpeg") ||
                            contentType.equals("image/jpg") ||
                            contentType.equals("image/png") ||
                            contentType.equals("image/gif") ||
                            contentType.equals("image/webp")
            );
        }

        private String getFileExtension(String filename) {
            if (filename == null || filename.lastIndexOf('.') == -1) {
                return ".jpg"; // Default extension
            }
            return filename.substring(filename.lastIndexOf('.'));
        }

        private String generateUniqueFilename(String extension) {
            return System.currentTimeMillis() + "_" +
                    java.util.UUID.randomUUID().toString().substring(0, 8) +
                    extension;
        }
    }

