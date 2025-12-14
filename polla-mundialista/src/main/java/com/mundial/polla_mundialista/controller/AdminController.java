package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Equipo;
import com.mundial.polla_mundialista.entity.Jugador;
import com.mundial.polla_mundialista.repository.EquipoRepository;
import com.mundial.polla_mundialista.repository.JugadorRepository;
import com.mundial.polla_mundialista.service.TorneoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final EquipoRepository equipoRepo;
    private final JugadorRepository jugadorRepo;
    private final TorneoService torneoService;

    // Solo inyectamos lo necesario para CRUD simple y el Servicio Maestro
    public AdminController(EquipoRepository equipoRepo, JugadorRepository jugadorRepo,
                           TorneoService torneoService) {
        this.equipoRepo = equipoRepo;
        this.jugadorRepo = jugadorRepo;
        this.torneoService = torneoService;
    }

    // ==========================================
    // 1. GESTIÓN DE EQUIPOS Y JUGADORES (CRUD Simple)
    // ==========================================

    @PutMapping("/actualizar-equipo/{id}")
    public Equipo actualizarEquipo(@PathVariable Long id, @Valid @RequestBody EquipoDTO dto) {
        Equipo equipo = equipoRepo.findById(id).orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
        equipo.setNombre(dto.getNombre());
        equipo.setUrlEscudo(dto.getUrlEscudo());
        return equipoRepo.save(equipo);
    }

    @PutMapping("/actualizar-jugador/{id}")
    public Jugador actualizarJugador(@PathVariable Long id, @Valid @RequestBody JugadorDTO dto) {
        Jugador jugador = jugadorRepo.findById(id).orElseThrow(() -> new RuntimeException("Jugador no encontrado"));
        jugador.setNombre(dto.getNombre());
        return jugadorRepo.save(jugador);
    }

    @PostMapping("/crear-jugador")
    public Jugador crearJugador(@Valid @RequestBody JugadorDTO dto) {
        Jugador jugador = new Jugador();
        jugador.setNombre(dto.getNombre());
        if (dto.getEquipoId() != null) {
            Equipo equipoReal = equipoRepo.findById(dto.getEquipoId())
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
            jugador.setEquipo(equipoReal);
        }
        return jugadorRepo.save(jugador);
    }

    // ==========================================
    // 2. GESTIÓN DEL TORNEO (Delegado 100% al Servicio)
    // ==========================================

    @PutMapping("/partidos/{id}/registrar-resultado")
    public String registrarResultado(@PathVariable Long id, @Valid @RequestBody ResultadoDTO dto) {
        // Llama al servicio para guardar resultado, calcular puntos y avanzar ronda si aplica
        return torneoService.registrarResultadoPartido(id, dto.getGolesLocal(), dto.getGolesVisitante());
    }

    @PutMapping("/partidos/{id}/corregir-llave")
    public String corregirLlave(@PathVariable Long id, @Valid @RequestBody RivalesDTO dto) {
        // Llama al servicio para forzar los equipos de un partido y compensar puntos de palo si es necesario
        return torneoService.corregirLlaveFaseFinal(id, dto.getEquipoLocalId(), dto.getEquipoVisitanteId());
    }

    @PostMapping("/sorteo/palos")
    public String sortearPalos() {
        return torneoService.sortearPalos();
    }

    // ==========================================
    // 3. PREMIOS MANUALES (Delegado 100% al Servicio)
    // ==========================================

    @PostMapping("/definir-campeon/{equipoId}")
    public String definirCampeon(@PathVariable Long equipoId) {
        return torneoService.procesarPuntosCampeon(equipoId);
    }

    @PostMapping("/definir-goleador/{jugadorId}")
    public String definirGoleador(@PathVariable Long jugadorId) {
        return torneoService.procesarPuntosGoleador(jugadorId);
    }

    // ==========================================
    // DTOs INTERNOS
    // ==========================================
    @Data static class EquipoDTO {
        @NotBlank(message = "El nombre es obligatorio")
        private String nombre;
        private String urlEscudo;
    }

    @Data static class JugadorDTO {
        @NotBlank(message = "El nombre es obligatorio")
        private String nombre;
        private Long equipoId;
    }

    @Data static class ResultadoDTO {
        @NotNull(message = "Local obligatorio") @Min(value = 0, message = "No negativos")
        private Integer golesLocal;

        @NotNull(message = "Visitante obligatorio") @Min(value = 0, message = "No negativos")
        private Integer golesVisitante;
    }

    @Data static class RivalesDTO {
        @NotNull(message = "Local obligatorio") private Long equipoLocalId;
        @NotNull(message = "Visitante obligatorio") private Long equipoVisitanteId;
    }
}