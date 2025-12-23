package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Equipo;
import com.mundial.polla_mundialista.entity.Jugador;
import com.mundial.polla_mundialista.repository.EquipoRepository;
import com.mundial.polla_mundialista.repository.JugadorRepository;
import com.mundial.polla_mundialista.service.TorneoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort; // ✅ IMPORTANTE: Agregado para ordenar
import org.springframework.web.bind.annotation.*;

import java.util.List; // ✅ IMPORTANTE

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final EquipoRepository equipoRepo;
    private final JugadorRepository jugadorRepo;
    private final TorneoService torneoService;

    public AdminController(EquipoRepository equipoRepo, JugadorRepository jugadorRepo,
                           TorneoService torneoService) {
        this.equipoRepo = equipoRepo;
        this.jugadorRepo = jugadorRepo;
        this.torneoService = torneoService;
    }

    // ==========================================
    // 1. GESTIÓN DE EQUIPOS Y JUGADORES
    // ==========================================

    // ✅ NUEVO: Endpoint para listar equipos ordenados alfabéticamente
    @GetMapping("/equipos")
    public List<Equipo> obtenerTodosLosEquipos() {
        return equipoRepo.findAll(Sort.by(Sort.Direction.ASC, "nombre"));
    }

    // ✅ MODIFICADO: Ahora soporta cambiar 'esCandidatoPalo'
    @PutMapping("/actualizar-equipo/{id}")
    public Equipo actualizarEquipo(@PathVariable Long id, @RequestBody EquipoDTO dto) {
        // Quitamos @Valid del parámetro para permitir actualizaciones parciales (sin enviar nombre obligatoriamente)
        Equipo equipo = equipoRepo.findById(id).orElseThrow(() -> new RuntimeException("Equipo no encontrado"));

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            equipo.setNombre(dto.getNombre());
        }
        if (dto.getUrlEscudo() != null) {
            equipo.setUrlEscudo(dto.getUrlEscudo());
        }
        // Lógica nueva para el Palo
        if (dto.getEsCandidatoPalo() != null) {
            equipo.setEsCandidatoPalo(dto.getEsCandidatoPalo());
        }

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
    // 2. GESTIÓN DEL TORNEO (Con Penales)
    // ==========================================

    @PutMapping("/partidos/{id}/registrar-resultado")
    public String registrarResultado(@PathVariable Long id, @Valid @RequestBody ResultadoDTO dto) {
        return torneoService.registrarResultadoPartido(
                id,
                dto.getGolesLocal(),
                dto.getGolesVisitante(),
                dto.getGolesPenalesLocal(),
                dto.getGolesPenalesVisitante()
        );
    }

    @PutMapping("/partidos/{id}/corregir-llave")
    public String corregirLlave(@PathVariable Long id, @Valid @RequestBody RivalesDTO dto) {
        return torneoService.corregirLlaveFaseFinal(id, dto.getEquipoLocalId(), dto.getEquipoVisitanteId());
    }

    @PostMapping("/sorteo/palos")
    public String sortearPalos() {
        return torneoService.sortearPalos();
    }

    // ==========================================
    // 3. PREMIOS MANUALES
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

    // ✅ MODIFICADO: Agregamos esCandidatoPalo
    @Data static class EquipoDTO {
        private String nombre; // Ya no es @NotBlank obligatorio para permitir updates parciales
        private String urlEscudo;
        private Boolean esCandidatoPalo;
    }

    @Data static class JugadorDTO {
        // @NotBlank(message = "El nombre es obligatorio") // Opcional si quieres validación estricta
        private String nombre;
        private Long equipoId;
    }

    @Data static class ResultadoDTO {
        @NotNull(message = "Local obligatorio") @Min(value = 0, message = "No negativos")
        private Integer golesLocal;

        @NotNull(message = "Visitante obligatorio") @Min(value = 0, message = "No negativos")
        private Integer golesVisitante;

        private Integer golesPenalesLocal;
        private Integer golesPenalesVisitante;
    }

    @Data static class RivalesDTO {
        @NotNull(message = "Local obligatorio") private Long equipoLocalId;
        @NotNull(message = "Visitante obligatorio") private Long equipoVisitanteId;
    }
}