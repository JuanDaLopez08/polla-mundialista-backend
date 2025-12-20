package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.HistorialDTO;
import com.mundial.polla_mundialista.dto.PrediccionDTO;
import com.mundial.polla_mundialista.entity.*;
import com.mundial.polla_mundialista.repository.*;
import com.mundial.polla_mundialista.util.AppConstants;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*; // Importante para Map, List, etc.
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predicciones")
@CrossOrigin(origins = "*")
public class PrediccionController {

    private final PrediccionRepository prediccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;
    private final PrediccionClasificadoRepository prediccionClasificadoRepository;

    public PrediccionController(PrediccionRepository prediccionRepository,
                                UsuarioRepository usuarioRepository,
                                PartidoRepository partidoRepository,
                                PrediccionClasificadoRepository prediccionClasificadoRepository) {
        this.prediccionRepository = prediccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.partidoRepository = partidoRepository;
        this.prediccionClasificadoRepository = prediccionClasificadoRepository;
    }

    // --- 1. HISTORIAL AGRUPADO ---
    @GetMapping("/historial")
    public List<HistorialDTO> obtenerHistorialCompleto() {
        List<Prediccion> todas = prediccionRepository.findAll();
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

    // --- 3. CREAR PREDICCIÓN ---
    @PostMapping
    public Prediccion crearOActualizarPrediccion(@Valid @RequestBody PrediccionDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_USUARIO_NO_ENCONTRADO));
        Partido partido = partidoRepository.findById(dto.getPartidoId())
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_PARTIDO_NO_ENCONTRADO));

        Fase fase = partido.getFase();
        if (fase.getFechaLimite() != null && LocalDateTime.now().isAfter(fase.getFechaLimite())) {
            throw new RuntimeException("TIEMPO EXPIRADO: La fase '" + fase.getNombre() + "' cerró sus apuestas.");
        }
        if (LocalDateTime.now().isAfter(partido.getFechaPartido())) {
            throw new RuntimeException("¡El partido ya comenzó! Apuestas cerradas.");
        }

        Optional<Prediccion> existente = prediccionRepository.findAll().stream()
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()) && p.getPartido().getId().equals(partido.getId()))
                .findFirst();

        if (existente.isPresent()) {
            throw new RuntimeException("YA PREDICHO: No se permiten cambios en pronósticos ya guardados.");
        }

        Prediccion prediccion = new Prediccion();
        prediccion.setUsuario(usuario);
        prediccion.setPartido(partido);
        prediccion.setGolesLocalPredicho(dto.getGolesLocal());
        prediccion.setGolesVisitantePredicho(dto.getGolesVisitante());
        prediccion.setFechaRegistro(LocalDateTime.now());

        return prediccionRepository.save(prediccion);
    }

    // --- 4. CLASIFICADOS (SOLUCIÓN AL ERROR DE TYPE DEFINITION) ---
    // Devolvemos List<Map> en lugar de List<Entity> para evitar problemas de Hibernate Proxy
    @GetMapping("/clasificados/usuario/{usuarioId}")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodosClasificadosUsuario(@PathVariable Long usuarioId) {

        // 1. Obtenemos la data cruda de la BD
        List<PrediccionClasificado> lista = prediccionClasificadoRepository.findByUsuarioId(usuarioId);

        // 2. Transformamos a un Mapa simple (JSON Limpio)
        List<Map<String, Object>> respuesta = lista.stream().map(pc -> {
            Map<String, Object> item = new HashMap<>();
            item.put("prediccionId", pc.getId());

            // Extraemos datos del equipo con seguridad (null check)
            if (pc.getEquipo() != null) {
                item.put("equipoId", pc.getEquipo().getId());
                item.put("nombreEquipo", pc.getEquipo().getNombre());
                item.put("urlEscudo", pc.getEquipo().getUrlEscudo());

                // Si necesitas el grupo
                if (pc.getEquipo().getGrupo() != null) {
                    item.put("grupo", pc.getEquipo().getGrupo().getNombre());
                }
            }
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }
}