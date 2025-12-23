package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.PosicionDTO;
import com.mundial.polla_mundialista.service.TorneoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/torneo")
@CrossOrigin(origins = "*")
public class TorneoController {

    private final TorneoService torneoService;

    public TorneoController(TorneoService torneoService) {
        this.torneoService = torneoService;
    }

    // Endpoint: Obtener Tabla de Posiciones agrupada por Grupo
    @GetMapping("/posiciones")
    public Map<String, List<PosicionDTO>> obtenerTablaPosiciones() {
        return torneoService.calcularTablaPosicionesPorGrupo();
    }

    // ✅ NUEVO ENDPOINT: Verificar si el torneo finalizó
    // Retorna true si ya hay Campeón y Goleador registrados en la BD.
    @GetMapping("/es-finalizado")
    public ResponseEntity<Boolean> verificarEstadoTorneo() {
        boolean terminado = torneoService.esTorneoFinalizado();
        return ResponseEntity.ok(terminado);
    }
}