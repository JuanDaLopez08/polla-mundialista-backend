package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.PosicionDTO;
import com.mundial.polla_mundialista.entity.ResultadoOficial; // ✅ IMPORTANTE: No olvides importar la entidad
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

    // Endpoint: Verificar si el torneo finalizó
    @GetMapping("/es-finalizado")
    public ResponseEntity<Boolean> verificarEstadoTorneo() {
        boolean terminado = torneoService.esTorneoFinalizado();
        return ResponseEntity.ok(terminado);
    }

    // ✅ NUEVO ENDPOINT: Obtener los resultados oficiales (Campeón y Goleador)
    // Este es el que llama tu AdminDefiniciones.jsx al cargar la página
    @GetMapping("/resultados-oficiales")
    public ResponseEntity<ResultadoOficial> obtenerResultadosOficiales() {
        ResultadoOficial resultados = torneoService.obtenerResultadosOficiales();

        // Si aún no se ha guardado nada en la BD, devolvemos 204 No Content
        if (resultados == null) {
            return ResponseEntity.noContent().build();
        }

        // Si existe, devolvemos el objeto con el Campeón y Goleador
        return ResponseEntity.ok(resultados);
    }
}