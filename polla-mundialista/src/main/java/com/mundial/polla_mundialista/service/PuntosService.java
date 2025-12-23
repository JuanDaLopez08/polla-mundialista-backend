package com.mundial.polla_mundialista.service;

import com.mundial.polla_mundialista.dto.DesglosePuntosDTO;
import com.mundial.polla_mundialista.entity.*;
import com.mundial.polla_mundialista.repository.*;
import com.mundial.polla_mundialista.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ResultadoOficialRepository resultadoRepo;

    /**
     * RECALCULA TODO Y GUARDA (Incluye lógica de desempate)
     */
    @Transactional
    public void recalcularPuntosUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Calcular Puntos Totales (Lógica Estándar)
        DesglosePuntosDTO desglose = calcularDesglose(usuarioId, true);
        usuario.setPuntosTotales(desglose.getTotalGeneral());

        // 2. Calcular Criterios de Desempate (Exactos, Ganadores, Campeón, Fecha)
        calcularCriteriosDesempate(usuario);

        usuarioRepo.save(usuario);
    }

    public DesglosePuntosDTO obtenerDesgloseUsuario(Long usuarioId) {
        return calcularDesglose(usuarioId, false);
    }

    // ==========================================
    // 🧠 LÓGICA DE DESEMPATE
    // ==========================================
    private void calcularCriteriosDesempate(Usuario usuario) {
        List<Prediccion> predicciones = prediccionRepo.findByUsuarioId(usuario.getId());
        Map<String, Integer> val = cargarConfiguracionPuntos();

        int countExactos = 0;
        int countGanadores = 0;
        LocalDateTime maxFecha = null; // Para guardar la última vez que el usuario hizo un cambio

        // 1. Recorrer predicciones de partidos
        for (Prediccion p : predicciones) {
            // Actualizar fecha más reciente (Criterio: El que madruga gana)
            if (p.getFechaRegistro() != null) {
                if (maxFecha == null || p.getFechaRegistro().isAfter(maxFecha)) {
                    maxFecha = p.getFechaRegistro();
                }
            }

            // Solo contamos aciertos para desempate si el partido ya terminó
            Partido partido = p.getPartido();
            if (AppConstants.ESTADO_FINALIZADO.equals(partido.getEstado()) && partido.getGolesLocalReal() != null) {
                int puntosGanados = p.getPuntosGanados();

                // Usamos la configuración para saber qué tipo de acierto fue
                if (puntosGanados == getValorObligatorio(val, AppConstants.CONF_PUNTOS_EXACTO)) {
                    countExactos++;
                } else if (puntosGanados == getValorObligatorio(val, AppConstants.CONF_PUNTOS_GANADOR)) {
                    countGanadores++;
                }
            }
        }

        // 2. Verificar Acierto de Campeón (Criterio 3)
        boolean acertoCampeon = false;
        Optional<PrediccionEspecial> espOpt = especialRepo.findByUsuarioId(usuario.getId());

        if (espOpt.isPresent()) {
            Long idCampeonReal = obtenerIdCampeonReal();
            // Si ya hay un campeón real definido y el usuario tiene predicción
            if (idCampeonReal != null && espOpt.get().getEquipoCampeon() != null) {
                if (espOpt.get().getEquipoCampeon().getId().equals(idCampeonReal)) {
                    acertoCampeon = true;
                }
            }
            // También revisamos la fecha de registro de la predicción especial
            /* (Opcional) Si quieres que la fecha de guardar el campeón cuente para "ultimaFechaPrediccion" */
        }

        // 3. Asignar valores al usuario
        usuario.setCantidadAciertosExactos(countExactos);
        usuario.setCantidadAciertosGanador(countGanadores);
        usuario.setAcertoCampeon(acertoCampeon);
        usuario.setUltimaFechaPrediccion(maxFecha);
    }

    // ==========================================
    // 🧠 MOTOR DE CÁLCULO DE PUNTOS
    // ==========================================
    private DesglosePuntosDTO calcularDesglose(Long usuarioId, boolean guardarCambios) {
        DesglosePuntosDTO reporte = new DesglosePuntosDTO();
        Map<String, Integer> val = cargarConfiguracionPuntos();

        // 1. PARTIDOS
        List<Prediccion> predicciones = prediccionRepo.findByUsuarioId(usuarioId);
        int totalPartidos = 0;

        for (Prediccion prediccion : predicciones) {
            Partido partido = prediccion.getPartido();
            if (AppConstants.ESTADO_FINALIZADO.equals(partido.getEstado()) && partido.getGolesLocalReal() != null) {
                int ganados = calcularLogicaPartido(prediccion, partido, val);

                if (ganados == getValorObligatorio(val, AppConstants.CONF_PUNTOS_EXACTO))
                    reporte.setPuntosPorMarcadorExacto(reporte.getPuntosPorMarcadorExacto() + ganados);
                else if (ganados == getValorObligatorio(val, AppConstants.CONF_PUNTOS_GANADOR))
                    reporte.setPuntosPorGanador(reporte.getPuntosPorGanador() + ganados);
                else if (ganados == getValorObligatorio(val, AppConstants.CONF_PUNTOS_MARCADOR_INVERTIDO))
                    reporte.setPuntosPorMarcadorInvertido(reporte.getPuntosPorMarcadorInvertido() + ganados);

                totalPartidos += ganados;

                if (guardarCambios) {
                    prediccion.setPuntosGanados(ganados);
                    prediccionRepo.save(prediccion);
                }
            }
        }

        // 2. CLASIFICADOS (Solo valida si llegaron a 16avos, no rondas posteriores)
        Set<Long> equiposEnDieciseisavos = obtenerEquiposEnFase(AppConstants.FASE_DIECISEISAVOS);
        List<PrediccionClasificado> misClasificados = clasificadosRepo.findByUsuarioId(usuarioId);
        int totalClasificados = 0;
        int ptsPorAcierto = getValorObligatorio(val, AppConstants.CONF_PUNTOS_CLASIFICADO);

        for (PrediccionClasificado pc : misClasificados) {
            if (equiposEnDieciseisavos.contains(pc.getEquipo().getId())) {
                totalClasificados += ptsPorAcierto;
                if (guardarCambios) { pc.setAcerto(true); clasificadosRepo.save(pc); }
            } else if (guardarCambios) { pc.setAcerto(false); clasificadosRepo.save(pc); }
        }
        reporte.setPuntosPorClasificados(totalClasificados);

        // 3. ESPECIALES
        Optional<PrediccionEspecial> espOpt = especialRepo.findByUsuarioId(usuarioId);
        if (espOpt.isPresent()) {
            PrediccionEspecial esp = espOpt.get();

            // Campeón
            Long idCampeon = obtenerIdCampeonReal();
            if (esp.getEquipoCampeon() != null && esp.getEquipoCampeon().getId().equals(idCampeon)) {
                reporte.setPuntosPorCampeon(getValorObligatorio(val, AppConstants.CONF_PUNTOS_CAMPEON));
            }

            // Goleador
            Long idGoleador = obtenerIdGoleadorReal();
            if (esp.getJugadorGoleador() != null && esp.getJugadorGoleador().getId().equals(idGoleador)) {
                reporte.setPuntosPorGoleador(getValorObligatorio(val, AppConstants.CONF_PUNTOS_GOLEADOR));
            }

            // Palo (Acumulativo por ronda jugada)
            if (esp.getEquipoPalo() != null) {
                Long idPalo = esp.getEquipoPalo().getId();
                int ptsPalo = getValorObligatorio(val, AppConstants.CONF_PUNTOS_PALO);
                int acumuladoPalo = 0;

                if (equipoJugoFase(idPalo, AppConstants.FASE_DIECISEISAVOS)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, AppConstants.FASE_OCTAVOS)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, AppConstants.FASE_CUARTOS)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, AppConstants.FASE_SEMIFINALES)) acumuladoPalo += ptsPalo;
                if (equipoJugoFase(idPalo, AppConstants.FASE_FINAL)) acumuladoPalo += ptsPalo;

                reporte.setPuntosPorPalo(acumuladoPalo);
            }
        }

        int granTotal = totalPartidos + totalClasificados + reporte.getPuntosPorCampeon() + reporte.getPuntosPorGoleador() + reporte.getPuntosPorPalo();
        reporte.setTotalGeneral(granTotal);
        return reporte;
    }

    // --- HELPERS ---
    private int getValorObligatorio(Map<String, Integer> mapa, String clave) {
        if (!mapa.containsKey(clave) || mapa.get(clave) == null) throw new RuntimeException("Falta config en BD: " + clave);
        return mapa.get(clave);
    }

    private int calcularLogicaPartido(Prediccion prediccion, Partido real, Map<String, Integer> val) {
        int pL = prediccion.getGolesLocalPredicho();
        int pV = prediccion.getGolesVisitantePredicho();
        int rL = real.getGolesLocalReal();
        int rV = real.getGolesVisitanteReal();

        if (pL == rL && pV == rV) return getValorObligatorio(val, AppConstants.CONF_PUNTOS_EXACTO);
        if (pL == rV && pV == rL) return getValorObligatorio(val, AppConstants.CONF_PUNTOS_MARCADOR_INVERTIDO);
        if (Integer.compare(pL, pV) == Integer.compare(rL, rV)) return getValorObligatorio(val, AppConstants.CONF_PUNTOS_GANADOR);
        return 0;
    }

    private Map<String, Integer> cargarConfiguracionPuntos() {
        Map<String, Integer> mapa = new HashMap<>();
        configRepo.findAll().forEach(c -> {
            try { mapa.put(c.getClave(), Integer.parseInt(c.getValor())); } catch (NumberFormatException e) { /* Log error */ }
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

    private boolean equipoJugoFase(Long equipoId, String nombreFase) { return obtenerEquiposEnFase(nombreFase).contains(equipoId); }

    private Long obtenerIdCampeonReal() {
        return resultadoRepo.findById(1L).map(ResultadoOficial::getEquipoCampeon).map(Equipo::getId).orElse(null);
    }
    private Long obtenerIdGoleadorReal() {
        return resultadoRepo.findById(1L).map(ResultadoOficial::getJugadorGoleador).map(Jugador::getId).orElse(null);
    }
}