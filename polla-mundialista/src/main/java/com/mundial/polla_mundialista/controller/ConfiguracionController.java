package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Configuracion;
import com.mundial.polla_mundialista.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionRepository configuracionRepository;

    @GetMapping("/reglas")
    public ResponseEntity<Map<String, String>> obtenerReglasPuntos() {
        List<Configuracion> configs = configuracionRepository.findAll();
        Map<String, String> mapaReglas = new HashMap<>();

        for (Configuracion c : configs) {
            mapaReglas.put(c.getClave(), c.getValor());
        }

        return ResponseEntity.ok(mapaReglas);
    }
}