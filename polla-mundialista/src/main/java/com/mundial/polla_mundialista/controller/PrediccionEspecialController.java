package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Equipo;
import com.mundial.polla_mundialista.entity.Jugador;
import com.mundial.polla_mundialista.entity.PrediccionEspecial;
import com.mundial.polla_mundialista.entity.Usuario;
import com.mundial.polla_mundialista.repository.EquipoRepository;
import com.mundial.polla_mundialista.repository.JugadorRepository;
import com.mundial.polla_mundialista.repository.PrediccionEspecialRepository;
import com.mundial.polla_mundialista.repository.UsuarioRepository;
import com.mundial.polla_mundialista.util.AppConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especiales")
@CrossOrigin(origins = "*")
public class PrediccionEspecialController {

    private final PrediccionEspecialRepository repo;
    private final UsuarioRepository usuarioRepo;
    private final EquipoRepository equipoRepo;
    private final JugadorRepository jugadorRepo;

    public PrediccionEspecialController(PrediccionEspecialRepository repo,
                                        UsuarioRepository usuarioRepo,
                                        EquipoRepository equipoRepo,
                                        JugadorRepository jugadorRepo) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
        this.equipoRepo = equipoRepo;
        this.jugadorRepo = jugadorRepo;
    }

    // --- PÚBLICO: LISTAR JUGADORES ---
    @GetMapping("/jugadores")
    public List<Jugador> listarJugadores() {
        return jugadorRepo.findAll();
    }

    // --- USUARIO: VER MI PREDICCIÓN (DTO SEGURO) ---
    @GetMapping("/usuario/{usuarioId}")
    public PrediccionEspecialResponseDTO obtenerPorUsuario(@PathVariable Long usuarioId) {
        PrediccionEspecial entidad = repo.findByUsuarioId(usuarioId).orElse(null);

        if (entidad == null) return null;

        // Convertimos la Entidad a DTO para limpiar tokens y datos basura
        return new PrediccionEspecialResponseDTO(entidad);
    }

    // --- ENDPOINTS DE GUARDADO ---
    @PostMapping("/campeon")
    public PrediccionEspecialResponseDTO guardarCampeon(@Valid @RequestBody PrediccionCampeonDTO dto) {
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Equipo campeon = equipoRepo.findById(dto.getEquipoCampeonId())
                .orElseThrow(() -> new RuntimeException("Equipo campeón no encontrado"));

        PrediccionEspecial especial = repo.findByUsuarioId(dto.getUsuarioId())
                .orElse(new PrediccionEspecial());

        especial.setUsuario(usuario);
        especial.setEquipoCampeon(campeon);

        PrediccionEspecial guardado = repo.save(especial);
        return new PrediccionEspecialResponseDTO(guardado);
    }

    @PostMapping("/goleador")
    public PrediccionEspecialResponseDTO guardarGoleador(@Valid @RequestBody PrediccionGoleadorDTO dto) {
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Jugador goleador = jugadorRepo.findById(dto.getJugadorGoleadorId())
                .orElseThrow(() -> new RuntimeException("Jugador goleador no encontrado"));

        PrediccionEspecial especial = repo.findByUsuarioId(dto.getUsuarioId())
                .orElse(new PrediccionEspecial());

        especial.setUsuario(usuario);
        especial.setJugadorGoleador(goleador);

        PrediccionEspecial guardado = repo.save(especial);
        return new PrediccionEspecialResponseDTO(guardado);
    }

    // ==========================================
    // DTOs DE RESPUESTA (CLEAN ARCHITECTURE)
    // ==========================================
    @Data
    static class PrediccionEspecialResponseDTO {
        private Long id;
        private UsuarioResumenDTO usuario;
        private EquipoSimpleDTO equipoCampeon;
        private JugadorSimpleDTO jugadorGoleador;
        private EquipoSimpleDTO equipoPalo; // ¡Aquí debe salir tu Gallo Tapado!

        public PrediccionEspecialResponseDTO(PrediccionEspecial p) {
            this.id = p.getId();
            if (p.getUsuario() != null) {
                this.usuario = new UsuarioResumenDTO(p.getUsuario().getId(), p.getUsuario().getUsername());
            }
            if (p.getEquipoCampeon() != null) {
                this.equipoCampeon = new EquipoSimpleDTO(p.getEquipoCampeon());
            }
            if (p.getJugadorGoleador() != null) {
                this.jugadorGoleador = new JugadorSimpleDTO(p.getJugadorGoleador());
            }
            if (p.getEquipoPalo() != null) {
                this.equipoPalo = new EquipoSimpleDTO(p.getEquipoPalo());
            }
        }
    }

    @Data
    static class UsuarioResumenDTO {
        private Long id;
        private String username;
        public UsuarioResumenDTO(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    @Data
    static class EquipoSimpleDTO {
        private Long id;
        private String nombre;
        private String urlEscudo;
        public EquipoSimpleDTO(Equipo e) {
            this.id = e.getId();
            this.nombre = e.getNombre();
            this.urlEscudo = e.getUrlEscudo();
        }
    }

    @Data
    static class JugadorSimpleDTO {
        private Long id;
        private String nombre;
        public JugadorSimpleDTO(Jugador j) {
            this.id = j.getId();
            this.nombre = j.getNombre();
        }
    }

    // ==========================================
    // DTOs DE PETICIÓN
    // ==========================================
    @Data
    static class PrediccionCampeonDTO {
        @NotNull private Long usuarioId;
        @NotNull private Long equipoCampeonId;
    }

    @Data
    static class PrediccionGoleadorDTO {
        @NotNull private Long usuarioId;
        @NotNull private Long jugadorGoleadorId;
    }
}