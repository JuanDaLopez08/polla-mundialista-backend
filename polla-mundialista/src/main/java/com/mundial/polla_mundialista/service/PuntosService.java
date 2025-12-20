package com.mundial.polla_mundialista.service;

import com.mundial.polla_mundialista.dto.DesglosePuntosDTO;
import com.mundial.polla_mundialista.entity.*;
import com.mundial.polla_mundialista.repository.*;
import com.mundial.polla_mundialista.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PuntosService {

    private final UsuarioRepository usuarioRepo;
    private final PrediccionRepository prediccionRepo;
    private final PartidoRepository partidoRepo;
    private final PrediccionClasificadoRepository clasificadosRepo;
    private final PrediccionEspecialRepository especialRepo;
    private final ConfiguracionRepository configRepo;
    private final ResultadoOficialRepository resultadoRepo; // ✅ NUEVO: Para leer la verdad oficial

    // --- NOMBRES EXACTOS DE FASES (Según tu DataSeeder) ---
    private static final String FASE_16 = "Dieciseisavos de Final";
    private static final String FASE_8 = "Octavos de Final";
    private static final String FASE_4 = "Cuartos de Final";
    private static final String FASE_SEMIS = "Semifinales";
    private static final String FASE_FINAL = "Final";

    // --- CLAVES DE CONFIGURACIÓN (Puntos) ---
    private static final String KEY_EXACTO = "PUNTOS_EXACTO";
    private static final String KEY_GANADOR = "PUNTOS_GANADOR";
    private static final String KEY_INVERTIDO = "PUNTOS_MARCADOR_INVERTIDO";
    private static final String KEY_CLASIFICADO = "PUNTOS_CLASIFICADO";
    private static final String KEY_CAMPEON = "PUNTOS_CAMPEON";
    private static final String KEY_GOLEADOR = "PUNTOS_GOLEADOR";
    private static final String KEY_PALO = "PUNTOS_PALO";

    /**
     * Devuelve el valor de configuración de puntos (ej: 15, 10, 5).
     * Usado por TorneoService y AdminController.
     */
    public int obtenerPuntosPorTipo(String claveConfig) {
        return configRepo.findByClave(claveConfig)
                .map(c -> Integer.parseInt(c.getValor()))
                .orElse(0);
    }

    /**
     * Calcula puntos de un partido individual en tiempo real.
     */
    public int calcularPuntosPartido(Prediccion prediccion, Partido partidoReal) {
        if (partidoReal.getGolesLocalReal() == null || partidoReal.getGolesVisitanteReal() == null) {
            return 0;
        }
        Map<String, Integer> valores = cargarConfiguracionPuntos();
        return calcularLogicaPartido(prediccion, partidoReal, valores);
    }

    /**
     * RECALCULA TODO Y GUARDA (Arregla totales en BD).
     */
    @Transactional
    public void recalcularPuntosUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        DesglosePuntosDTO desglose = calcularDesglose(usuarioId, true); // true = guardar cambios

        usuario.setPuntosTotales(desglose.getTotalGeneral());
        usuarioRepo.save(usuario);
    }

    /**
     * SOLO CONSULTA (Para mostrar en el Frontend sin tocar la BD).
     */
    public DesglosePuntosDTO obtenerDesgloseUsuario(Long usuarioId) {
        return calcularDesglose(usuarioId, false); // false = solo lectura
    }

    // ==========================================
    // 🧠 MOTOR DE CÁLCULO CENTRAL
    // ==========================================
    private DesglosePuntosDTO calcularDesglose(Long usuarioId, boolean guardarCambios) {
        DesglosePuntosDTO reporte = new DesglosePuntosDTO();
        Map<String, Integer> val = cargarConfiguracionPuntos();

        // 1. PARTIDOS (Marcadores)
        List<Prediccion> predicciones = prediccionRepo.findByUsuarioId(usuarioId);
        int totalPartidos = 0;

        for (Prediccion pred : predicciones) {
            Partido partido = pred.getPartido();
            if ("FINALIZADO".equals(partido.getEstado()) && partido.getGolesLocalReal() != null) {
                int ganados = calcularLogicaPartido(pred, partido, val);

                // Clasificación para reporte
                if (ganados == val.get(KEY_EXACTO)) reporte.setPuntosPorMarcadorExacto(reporte.getPuntosPorMarcadorExacto() + ganados);
                else if (ganados == val.get(KEY_GANADOR)) reporte.setPuntosPorGanador(reporte.getPuntosPorGanador() + ganados);
                else if (ganados == val.get(KEY_INVERTIDO)) reporte.setPuntosPorMarcadorInvertido(reporte.getPuntosPorMarcadorInvertido() + ganados);

                totalPartidos += ganados;

                if (guardarCambios) {
                    pred.setPuntosGanados(ganados);
                    prediccionRepo.save(pred);
                }
            }
        }

        // 2. CLASIFICADOS (Equipos que llegaron a 16avos)
        Set<Long> equiposEn16avos = obtenerEquiposEnFase(FASE_16);
        List<PrediccionClasificado> misClasificados = clasificadosRepo.findByUsuarioId(usuarioId);
        int totalClasificados = 0;
        int ptsPorAcierto = val.get(KEY_CLASIFICADO);

        for (PrediccionClasificado pc : misClasificados) {
            if (equiposEn16avos.contains(pc.getEquipo().getId())) {
                totalClasificados += ptsPorAcierto;
                if (guardarCambios) {
                    pc.setAcerto(true);
                    clasificadosRepo.save(pc);
                }
            } else if (guardarCambios) {
                pc.setAcerto(false);
                clasificadosRepo.save(pc);
            }
        }
        reporte.setPuntosPorClasificados(totalClasificados);

        // 3. ESPECIALES (Campeón, Goleador, Palo)
        Optional<PrediccionEspecial> espOpt = especialRepo.findByUsuarioId(usuarioId);
        if (espOpt.isPresent()) {
            PrediccionEspecial esp = espOpt.get();

            // A. CAMPEÓN (Lee de ResultadoOficial)
            Long idCampeon = obtenerIdCampeonReal();
            if (idCampeon != null && esp.getEquipoCampeon() != null &&
                    esp.getEquipoCampeon().getId().equals(idCampeon)) {
                reporte.setPuntosPorCampeon(val.get(KEY_CAMPEON));
            }

            // B. GOLEADOR (Lee de ResultadoOficial)
            Long idGoleador = obtenerIdGoleadorReal();
            if (idGoleador != null && esp.getJugadorGoleador() != null &&
                    esp.getJugadorGoleador().getId().equals(idGoleador)) {
                reporte.setPuntosPorGoleador(val.get(KEY_GOLEADOR));
            }

            // C. PALO (Gallo Tapado) - Suma ACUMULATIVA por ronda
            if (esp.getEquipoPalo() != null) {
                Long idPalo = esp.getEquipoPalo().getId();
                int ptsPalo = val.get(KEY_PALO); // Ej: 5 pts por ronda
                int acumuladoPalo = 0;

                if (equipoJugoFase(idPalo, FASE_16)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, FASE_8)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, FASE_4)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, FASE_SEMIS)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, FASE_FINAL)) acumuladoPalo += ptsPalo;

                reporte.setPuntosPorPalo(acumuladoPalo);
            }
        }

        // TOTAL FINAL
        int granTotal = totalPartidos + totalClasificados +
                reporte.getPuntosPorCampeon() +
                reporte.getPuntosPorGoleador() +
                reporte.getPuntosPorPalo();

        reporte.setTotalGeneral(granTotal);
        return reporte;
    }

    // ==========================================
    // 🛠️ HELPERS LÓGICOS
    // ==========================================

    private int calcularLogicaPartido(Prediccion pred, Partido real, Map<String, Integer> val) {
        int pL = pred.getGolesLocalPredicho();
        int pV = pred.getGolesVisitantePredicho();
        int rL = real.getGolesLocalReal();
        int rV = real.getGolesVisitanteReal();

        if (pL == rL && pV == rV) return val.get(KEY_EXACTO);
        if (pL == rV && pV == rL) return val.get(KEY_INVERTIDO);
        if (Integer.compare(pL, pV) == Integer.compare(rL, rV)) return val.get(KEY_GANADOR);

        return 0;
    }

    private Map<String, Integer> cargarConfiguracionPuntos() {
        Map<String, Integer> mapa = new HashMap<>();
        // Defaults
        mapa.put(KEY_EXACTO, 5); mapa.put(KEY_GANADOR, 3); mapa.put(KEY_INVERTIDO, 1);
        mapa.put(KEY_CLASIFICADO, 2); mapa.put(KEY_CAMPEON, 15); mapa.put(KEY_GOLEADOR, 10);
        mapa.put(KEY_PALO, 5);

        configRepo.findAll().forEach(c -> {
            try { mapa.put(c.getClave(), Integer.parseInt(c.getValor())); }
            catch (Exception e) {}
        });
        return mapa;
    }

    private Set<Long> obtenerEquiposEnFase(String nombreFase) {
        Set<Long> ids = new HashSet<>();
        List<Partido> partidos = partidoRepo.findByFaseNombreIgnoreCaseOrderByFechaPartidoAsc(nombreFase);
        for (Partido p : partidos) {
            if (p.getEquipoLocal() != null) ids.add(p.getEquipoLocal().getId());
            if (p.getEquipoVisitante() != null) ids.add(p.getEquipoVisitante().getId());
        }
        return ids;
    }

    private boolean equipoJugoFase(Long equipoId, String nombreFase) {
        return obtenerEquiposEnFase(nombreFase).contains(equipoId);
    }

    // ✅ NUEVO: Lee de la tabla ResultadoOficial
    private Long obtenerIdCampeonReal() {
        return resultadoRepo.findById(1L)
                .map(r -> r.getEquipoCampeon())
                .map(Equipo::getId)
                .orElse(null);
    }

    // ✅ NUEVO: Lee de la tabla ResultadoOficial
    private Long obtenerIdGoleadorReal() {
        return resultadoRepo.findById(1L)
                .map(r -> r.getJugadorGoleador())
                .map(Jugador::getId)
                .orElse(null);
    }
}