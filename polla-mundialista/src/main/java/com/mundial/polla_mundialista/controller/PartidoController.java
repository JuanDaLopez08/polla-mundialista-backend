package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Partido;
import com.mundial.polla_mundialista.repository.PartidoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partidos")
@CrossOrigin(origins = "*")
public class PartidoController {

    private final PartidoRepository partidoRepository;

    public PartidoController(PartidoRepository partidoRepository) {
        this.partidoRepository = partidoRepository;
    }

    // 1. Obtener TODOS (Ordenados por fecha)
    @GetMapping
    public List<Partido> obtenerTodos() {
        return partidoRepository.findAllByOrderByFechaPartidoAsc();
    }

    // 2. Obtener por ID de Fase (Se mantiene por si acaso)
    @GetMapping("/fase/{faseId}")
    public List<Partido> obtenerPorFase(@PathVariable Long faseId) {
        return partidoRepository.findByFaseIdOrderByFechaPartidoAsc(faseId);
    }

    // 3. NUEVO: Obtener por NOMBRE de Fase
    // Ejemplo: GET http://localhost:8080/api/partidos/fase-nombre/Grupos
    // Ejemplo: GET http://localhost:8080/api/partidos/fase-nombre/Final
    @GetMapping("/fase-nombre/{nombreFase}")
    public List<Partido> obtenerPorNombreFase(@PathVariable String nombreFase) {
        return partidoRepository.findByFaseNombreIgnoreCaseOrderByFechaPartidoAsc(nombreFase);
    }
}