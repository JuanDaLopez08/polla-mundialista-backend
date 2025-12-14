package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.PosicionDTO;
import com.mundial.polla_mundialista.service.TorneoService;
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
    // Retorna un JSON tipo: { "A": [Equipo1, Equipo2...], "B": [...] }
    @GetMapping("/posiciones")
    public Map<String, List<PosicionDTO>> obtenerTablaPosiciones() {
        return torneoService.calcularTablaPosicionesPorGrupo();
    }
}