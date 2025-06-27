package com.elbuensabor.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        // Devolvemos un JSON para ser consistentes
        return Map.of("respuesta", "pong");
    }
}