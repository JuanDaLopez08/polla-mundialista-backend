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

    // CORRECCIÓN: Devolvemos un DTO (PrediccionResumenDTO) en lugar de la Entidad
    @GetMapping("/usuario/{usuarioId}/grupo/{grupoId}")
    public List<PrediccionResumenDTO> obtenerPorGrupo(@PathVariable Long usuarioId, @PathVariable Long grupoId) {
        List<PrediccionClasificado> predicciones = prediccionRepo.findByUsuarioIdAndGrupoId(usuarioId, grupoId);

        // Convertimos la entidad sucia (con proxies) a un DTO limpio
        return predicciones.stream()
                .map(p -> new PrediccionResumenDTO(
                        p.getId(),
                        p.getEquipo().getId(),
                        p.getEquipo().getNombre(),
                        p.getEquipo().getUrlEscudo()
                ))
                .collect(Collectors.toList());
    }

    // Guardar los clasificados
    @PostMapping
    @Transactional
    public String guardarPrediccion(@Valid @RequestBody ClasificadosDTO dto) {

        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Grupo grupo = grupoRepo.findById(dto.getGrupoId())
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // 1. Borrar predicciones anteriores
        List<PrediccionClasificado> anteriores = prediccionRepo.findByUsuarioIdAndGrupoId(usuario.getId(), grupo.getId());
        prediccionRepo.deleteAll(anteriores);

        // 2. Guardar las nuevas
        for (Long equipoId : dto.getEquiposIds()) {
            Equipo equipo = equipoRepo.findById(equipoId)
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + equipoId));

            if (equipo.getGrupo() == null || !equipo.getGrupo().getId().equals(grupo.getId())) {
                throw new RuntimeException("El equipo " + equipo.getNombre() + " no pertenece al Grupo " + grupo.getNombre());
            }

            PrediccionClasificado prediccion = new PrediccionClasificado();
            prediccion.setUsuario(usuario);
            prediccion.setGrupo(grupo);
            prediccion.setEquipo(equipo);
            prediccion.setFechaRegistro(LocalDateTime.now());

            prediccionRepo.save(prediccion);
        }

        return "Predicción de clasificados guardada exitosamente.";
    }

    // ==========================================
    // DTO INTERNO (Para evitar el error ByteBuddyInterceptor)
    // ==========================================
    @Data
    @AllArgsConstructor
    static class PrediccionResumenDTO {
        private Long prediccionId;
        private Long equipoId;
        private String nombreEquipo;
        private String urlEscudo;
    }
}