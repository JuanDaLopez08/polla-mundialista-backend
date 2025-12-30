package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Configuracion;
import com.mundial.polla_mundialista.repository.ConfiguracionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionRepository configuracionRepository;

    // ==========================================
    // 1. OBTENER TODO (Lectura)
    // ==========================================
    // Trae la lista completa de configuraciones de puntos ordenada alfabéticamente
    @GetMapping("/obtener-configuracion-de-puntos")
    public List<Configuracion> obtenerConfiguracionDePuntos() {
        return configuracionRepository.findAll(Sort.by(Sort.Direction.ASC, "clave"));
    }

    // ==========================================
    // 2. EDITAR UNO (Escritura)
    // ==========================================
    // Edita el valor de una regla específica
    @PutMapping("/editar-valor-de-puntos")
    public ResponseEntity<?> editarValorDePuntos(@Valid @RequestBody EditarConfigDTO dto) {

        // Usamos el método 'findByClave' del repositorio
        Optional<Configuracion> configOpt = configuracionRepository.findByClave(dto.getClave());

        if (configOpt.isPresent()) {
            Configuracion config = configOpt.get();
            config.setValor(dto.getValor());
            configuracionRepository.save(config);
            return ResponseEntity.ok(config);
        } else {
            return ResponseEntity.badRequest().body("Error: No existe la regla con clave: " + dto.getClave());
        }
    }

    // ==========================================
    // DTO INTERNO
    // ==========================================
    @Data
    static class EditarConfigDTO {
        @NotBlank(message = "La clave es obligatoria (Ej: PUNTOS_CAMPEON)")
        private String clave;

        @NotBlank(message = "El nuevo valor es obligatorio")
        private String valor;
    }
}