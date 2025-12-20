package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.ClasificadosDTO;
import com.mundial.polla_mundialista.entity.*;
import com.mundial.polla_mundialista.repository.*;
import com.mundial.polla_mundialista.util.AppConstants;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predicciones/clasificados")
@CrossOrigin(origins = "*")
public class PrediccionClasificadoController {

    private final PrediccionClasificadoRepository prediccionRepo;
    private final UsuarioRepository usuarioRepo;
    private final GrupoRepository grupoRepo;
    private final EquipoRepository equipoRepo;

    public PrediccionClasificadoController(PrediccionClasificadoRepository prediccionRepo,
                                           UsuarioRepository usuarioRepo,
                                           GrupoRepository grupoRepo,
                                           EquipoRepository equipoRepo) {
        this.prediccionRepo = prediccionRepo;
        this.usuarioRepo = usuarioRepo;
        this.grupoRepo = grupoRepo;
        this.equipoRepo = equipoRepo;
    }

    @GetMapping("/usuario/{usuarioId}/grupo/{grupoId}")
    public List<PrediccionResumenDTO> obtenerPorGrupo(@PathVariable Long usuarioId, @PathVariable Long grupoId) {
        List<PrediccionClasificado> predicciones = prediccionRepo.findByUsuarioIdAndEquipoGrupoId(usuarioId, grupoId);

        return predicciones.stream()
                .map(p -> new PrediccionResumenDTO(
                        p.getId(),
                        p.getEquipo().getId(),
                        p.getEquipo().getNombre(),
                        p.getEquipo().getUrlEscudo()
                ))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 🛡️ GUARDAR BLINDADO (SIN SOBRESCRITURA)
    // ==========================================
    @PostMapping
    @Transactional
    public ResponseEntity<?> guardarPrediccion(@Valid @RequestBody ClasificadosDTO dto) {

        // 1. Validar Usuario y Grupo
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Grupo grupo = grupoRepo.findById(dto.getGrupoId())
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // 2. SEGURIDAD: Verificar si ya existen predicciones para este grupo
        List<PrediccionClasificado> anteriores = prediccionRepo.findByUsuarioIdAndEquipoGrupoId(usuario.getId(), grupo.getId());

        if (!anteriores.isEmpty()) {
            // 🛑 BLOQUEO DE ATAQUE: Si ya hay datos, rechazamos la petición.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("ERROR: Ya has realizado tu predicción para el Grupo " + grupo.getNombre() + ". No se permiten cambios.");
        }

        // 3. SEGURIDAD: Validar que envíen exactamente 2 equipos (Ni 1, ni 3)
        if (dto.getEquiposIds().size() != 2) {
            return ResponseEntity.badRequest()
                    .body("ERROR: Debes seleccionar exactamente 2 equipos para clasificar.");
        }

        // 4. Guardar las nuevas (Solo llegamos aquí si no existían previas)
        for (Long equipoId : dto.getEquiposIds()) {
            Equipo equipo = equipoRepo.findById(equipoId)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + equipoId));

            // Validar que el equipo pertenezca al grupo (Anti-Hack)
            if (equipo.getGrupo() == null || !equipo.getGrupo().getId().equals(grupo.getId())) {
                return ResponseEntity.badRequest()
                        .body("ERROR: El equipo " + equipo.getNombre() + " no pertenece al Grupo " + grupo.getNombre());
            }

            PrediccionClasificado prediccion = new PrediccionClasificado();
            prediccion.setUsuario(usuario);
            prediccion.setEquipo(equipo);
            prediccionRepo.save(prediccion);
        }

        return ResponseEntity.ok("Predicción de clasificados guardada exitosamente.");
    }

    @Data
    @AllArgsConstructor
    static class PrediccionResumenDTO {
        private Long prediccionId;
        private Long equipoId;
        private String nombreEquipo;
        private String urlEscudo;
    }
}