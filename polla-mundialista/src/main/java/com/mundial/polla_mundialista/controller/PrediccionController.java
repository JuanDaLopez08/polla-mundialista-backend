package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.HistorialDTO;
import com.mundial.polla_mundialista.dto.PrediccionDTO;
import com.mundial.polla_mundialista.entity.Fase;
import com.mundial.polla_mundialista.entity.Partido;
import com.mundial.polla_mundialista.entity.Prediccion;
import com.mundial.polla_mundialista.entity.Usuario;
import com.mundial.polla_mundialista.repository.PartidoRepository;
import com.mundial.polla_mundialista.repository.PrediccionRepository;
import com.mundial.polla_mundialista.repository.UsuarioRepository;
import com.mundial.polla_mundialista.util.AppConstants;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predicciones")
@CrossOrigin(origins = "*")
public class PrediccionController {

    private final PrediccionRepository prediccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;

    public PrediccionController(PrediccionRepository prediccionRepository, UsuarioRepository usuarioRepository, PartidoRepository partidoRepository) {
        this.prediccionRepository = prediccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.partidoRepository = partidoRepository;
    }

    // --- 1. HISTORIAL AGRUPADO (Para la pestaña Histórico del Front) ---
    @GetMapping("/historial")
    public List<HistorialDTO> obtenerHistorialCompleto() {
        List<Prediccion> todas = prediccionRepository.findAll();

        // Agrupamos por Usuario
        Map<Usuario, List<Prediccion>> agrupadas = todas.stream()
                .collect(Collectors.groupingBy(Prediccion::getUsuario));

        List<HistorialDTO> historial = new ArrayList<>();

        agrupadas.forEach((usuario, predicciones) -> {
            HistorialDTO dto = new HistorialDTO();
            dto.setUsername(usuario.getUsername());
            dto.setPuntosTotales(usuario.getPuntosTotales());

            List<HistorialDTO.PrediccionResumenDTO> detalles = predicciones.stream().map(p -> {
                HistorialDTO.PrediccionResumenDTO resumen = new HistorialDTO.PrediccionResumenDTO();
                resumen.setPartidoId(p.getPartido().getId());

                // Validamos nulls por si son partidos de fases finales aún no definidos
                if (p.getPartido().getEquipoLocal() != null) {
                    resumen.setEquipoLocal(p.getPartido().getEquipoLocal().getNombre());
                    resumen.setUrlEscudoLocal(p.getPartido().getEquipoLocal().getUrlEscudo());
                } else {
                    resumen.setEquipoLocal("Por definir");
                }

                if (p.getPartido().getEquipoVisitante() != null) {
                    resumen.setEquipoVisitante(p.getPartido().getEquipoVisitante().getNombre());
                    resumen.setUrlEscudoVisitante(p.getPartido().getEquipoVisitante().getUrlEscudo());
                } else {
                    resumen.setEquipoVisitante("Por definir");
                }

                resumen.setGolesLocalPredicho(p.getGolesLocalPredicho());
                resumen.setGolesVisitantePredicho(p.getGolesVisitantePredicho());
                resumen.setPuntosGanados(p.getPuntosGanados());
                resumen.setEstadoPartido(p.getPartido().getEstado());
                return resumen;
            }).collect(Collectors.toList());

            dto.setPredicciones(detalles);
            historial.add(dto);
        });

        // Ordenamos por puntos (Mayor a menor)
        historial.sort((a, b) -> {
            int pA = a.getPuntosTotales() == null ? 0 : a.getPuntosTotales();
            int pB = b.getPuntosTotales() == null ? 0 : b.getPuntosTotales();
            return Integer.compare(pB, pA);
        });

        return historial;
    }

    // --- 2. MIS PREDICCIONES ---
    @GetMapping("/usuario/{usuarioId}")
    public List<Prediccion> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return prediccionRepository.findByUsuarioId(usuarioId);
    }

    // --- 3. CREAR O EDITAR PREDICCIÓN (CON REGLA DE FASE) ---
    @PostMapping
    public Prediccion crearOActualizarPrediccion(@Valid @RequestBody PrediccionDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));

        Partido partido = partidoRepository.findById(dto.getPartidoId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_PARTIDO_NO_ENCONTRADO));

        // VALIDACIÓN DE NEGOCIO: FECHA LÍMITE DE LA FASE
        Fase fase = partido.getFase();
        if (fase.getFechaLimite() != null && LocalDateTime.now().isAfter(fase.getFechaLimite())) {
            throw new RuntimeException("TIEMPO EXPIRADO: La fase '" + fase.getNombre() + "' cerró sus apuestas el " + fase.getFechaLimite());
        }

        // VALIDACIÓN EXTRA: El partido individual ya comenzó
        if (LocalDateTime.now().isAfter(partido.getFechaPartido())) {
            throw new RuntimeException("¡El partido ya comenzó! Apuestas cerradas.");
        }

        Optional<Prediccion> existente = prediccionRepository.findAll().stream()
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()) && p.getPartido().getId().equals(partido.getId()))
                .findFirst();

        Prediccion prediccion;

        if (existente.isPresent()) {
            prediccion = existente.get();
            // Actualizamos valores
            prediccion.setGolesLocalPredicho(dto.getGolesLocal());
            prediccion.setGolesVisitantePredicho(dto.getGolesVisitante());
            prediccion.setFechaRegistro(LocalDateTime.now()); // Actualizamos auditoría
        } else {
            // Creamos nueva
            prediccion = new Prediccion();
            prediccion.setUsuario(usuario);
            prediccion.setPartido(partido);
            prediccion.setGolesLocalPredicho(dto.getGolesLocal());
            prediccion.setGolesVisitantePredicho(dto.getGolesVisitante());
            prediccion.setFechaRegistro(LocalDateTime.now());
        }

        return prediccionRepository.save(prediccion);
    }
}