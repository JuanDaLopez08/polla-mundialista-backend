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

    // --- USUARIO: VER MI PREDICCIÓN ---
    @GetMapping("/usuario/{usuarioId}")
    public PrediccionEspecial obtenerPorUsuario(@PathVariable Long usuarioId) {
        // Retorna lo que tenga (puede tener solo campeón, solo goleador, o ambos)
        return repo.findByUsuarioId(usuarioId).orElse(null);
    }

    // --- ENDPOINT 1: PREDECIR SOLO CAMPEÓN ---
    @PostMapping("/campeon")
    public PrediccionEspecial guardarCampeon(@Valid @RequestBody PrediccionCampeonDTO dto) {
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Equipo campeon = equipoRepo.findById(dto.getEquipoCampeonId())
                .orElseThrow(() -> new RuntimeException("Equipo campeón no encontrado"));

        // Buscamos si ya tiene registro para actualizarlo, sino creamos uno nuevo
        PrediccionEspecial especial = repo.findByUsuarioId(dto.getUsuarioId())
                .orElse(new PrediccionEspecial());

        especial.setUsuario(usuario);
        especial.setEquipoCampeon(campeon);
        // NO tocamos el goleador (se mantiene null o el que ya estaba)

        return repo.save(especial);
    }

    // --- ENDPOINT 2: PREDECIR SOLO GOLEADOR ---
    @PostMapping("/goleador")
    public PrediccionEspecial guardarGoleador(@Valid @RequestBody PrediccionGoleadorDTO dto) {
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Jugador goleador = jugadorRepo.findById(dto.getJugadorGoleadorId())
                .orElseThrow(() -> new RuntimeException("Jugador goleador no encontrado"));

        // Buscamos si ya tiene registro
        PrediccionEspecial especial = repo.findByUsuarioId(dto.getUsuarioId())
                .orElse(new PrediccionEspecial());

        especial.setUsuario(usuario);
        especial.setJugadorGoleador(goleador);
        // NO tocamos el campeón (se mantiene null o el que ya estaba)

        return repo.save(especial);
    }

    // ==========================================
    // DTOs INDEPENDIENTES
    // ==========================================
    @Data
    static class PrediccionCampeonDTO {
        @NotNull
        private Long usuarioId;
        @NotNull
        private Long equipoCampeonId;
    }

    @Data
    static class PrediccionGoleadorDTO {
        @NotNull
        private Long usuarioId;
        @NotNull
        private Long jugadorGoleadorId;
    }
}