package com.mundial.polla_mundialista.service;

import com.mundial.polla_mundialista.dto.PosicionDTO;
import com.mundial.polla_mundialista.entity.*;
import com.mundial.polla_mundialista.repository.*;
import com.mundial.polla_mundialista.util.AppConstants;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TorneoService {

    private final PartidoRepository partidoRepo;
    private final EquipoRepository equipoRepo;
    private final PrediccionRepository prediccionRepo;
    private final UsuarioRepository usuarioRepo;
    private final PuntosService puntosService;
    private final PrediccionEspecialRepository prediccionEspecialRepo;
    private final ConfiguracionRepository configRepo;
    private final RolRepository rolRepo;
    private final FaseRepository faseRepo;
    private final JugadorRepository jugadorRepo;
    private final ResultadoOficialRepository resultadoRepo;

    // Nombres de Fases
    private static final String FASE_GRUPOS = "Fase de Grupos";
    private static final String FASE_16 = "Dieciseisavos de Final";
    private static final String FASE_8 = "Octavos de Final";
    private static final String FASE_4 = "Cuartos de Final";
    private static final String FASE_SEMIS = "Semifinales";
    private static final String FASE_FINAL = "Final";
    private static final String FASE_3ER = "Tercer Puesto";

    public TorneoService(PartidoRepository partidoRepo, EquipoRepository equipoRepo,
                         PrediccionRepository prediccionRepo, UsuarioRepository usuarioRepo,
                         PuntosService puntosService, PrediccionEspecialRepository prediccionEspecialRepo,
                         ConfiguracionRepository configRepo, RolRepository rolRepo, FaseRepository faseRepo,
                         JugadorRepository jugadorRepo, ResultadoOficialRepository resultadoRepo) {
        this.partidoRepo = partidoRepo;
        this.equipoRepo = equipoRepo;
        this.prediccionRepo = prediccionRepo;
        this.usuarioRepo = usuarioRepo;
        this.puntosService = puntosService;
        this.prediccionEspecialRepo = prediccionEspecialRepo;
        this.configRepo = configRepo;
        this.rolRepo = rolRepo;
        this.faseRepo = faseRepo;
        this.jugadorRepo = jugadorRepo;
        this.resultadoRepo = resultadoRepo;
    }

    // ==========================================
    // 1. REGISTRO DE RESULTADOS (Motor Principal)
    // ==========================================
    @Transactional
    public String registrarResultadoPartido(Long partidoId, Integer golesLocal, Integer golesVisitante) {
        Partido partido = partidoRepo.findById(partidoId)
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_PARTIDO_NO_ENCONTRADO));

        partido.setGolesLocalReal(golesLocal);
        partido.setGolesVisitanteReal(golesVisitante);
        partido.setEstado(AppConstants.ESTADO_FINALIZADO);
        partidoRepo.save(partido);

        // A. Actualizar puntos de usuarios que apostaron en este partido
        List<Prediccion> apuestas = prediccionRepo.findByPartidoId(partido.getId());
        for (Prediccion p : apuestas) {
            puntosService.recalcularPuntosUsuario(p.getUsuario().getId());
        }

        StringBuilder mensaje = new StringBuilder("Resultado guardado. Puntos actualizados para " + apuestas.size() + " usuarios. ");
        Fase faseActual = partido.getFase();

        // B. Avanzar Ganador (Solo fases eliminatorias)
        if (!FASE_GRUPOS.equalsIgnoreCase(faseActual.getNombre())) {
            Equipo ganador = determinarGanador(partido);
            if (ganador != null) {
                String avanceMsg = avanzarSiguienteRonda(partido.getNumeroPartido(), ganador);
                mensaje.append(avanceMsg);
            }
        }

        // C. Verificar Fin de Fase
        boolean faseTerminada = partidoRepo.findByFaseIdOrderByFechaPartidoAsc(faseActual.getId())
                .stream()
                .noneMatch(p -> !AppConstants.ESTADO_FINALIZADO.equals(p.getEstado()));

        if (faseTerminada) {
            mensaje.append(cerrarFase(faseActual.getId()));
        }

        return mensaje.toString();
    }

    // ==========================================
    // 2. GESTIÓN DE RESULTADOS ESPECIALES (CAMPEÓN / GOLEADOR)
    // ==========================================
    @Transactional
    public String procesarPuntosCampeon(Long equipoId) {
        Equipo campeon = equipoRepo.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));

        // 1. Guardar la verdad oficial (Tabla dedicada)
        ResultadoOficial resultado = resultadoRepo.findById(1L).orElse(new ResultadoOficial());
        resultado.setId(1L);
        resultado.setEquipoCampeon(campeon);
        resultadoRepo.save(resultado);

        // 2. Recalcular puntos masivamente
        int c = recalcularMasivoUsuarios();
        return "Campeón registrado: " + campeon.getNombre() + ". Puntos actualizados a " + c + " usuarios.";
    }

    @Transactional
    public String procesarPuntosGoleador(Long jugadorId) {
        Jugador goleador = jugadorRepo.findById(jugadorId)
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

        // 1. Guardar la verdad oficial
        ResultadoOficial resultado = resultadoRepo.findById(1L).orElse(new ResultadoOficial());
        resultado.setId(1L);
        resultado.setJugadorGoleador(goleador);
        resultadoRepo.save(resultado);

        // 2. Recalcular puntos masivamente
        int c = recalcularMasivoUsuarios();
        return "Goleador registrado: " + goleador.getNombre() + ". Puntos actualizados a " + c + " usuarios.";
    }

    // Helper para recalcular a todos los usuarios "ROLE_USER"
    private int recalcularMasivoUsuarios() {
        int c = 0;
        List<Usuario> usuarios = usuarioRepo.findAll();
        for (Usuario u : usuarios) {
            if (u.getRol() != null && AppConstants.ROLE_USER.equals(u.getRol().getNombre())) {
                puntosService.recalcularPuntosUsuario(u.getId());
                c++;
            }
        }
        return c;
    }

    // ==========================================
    // 3. GESTIÓN DE FASES Y CRUCES
    // ==========================================
    @Transactional
    public String cerrarFase(Long faseId) {
        Fase fase = faseRepo.findById(faseId).orElseThrow();

        if (AppConstants.FASE_CERRADA.equals(fase.getEstado())) return " (Fase ya cerrada).";

        fase.setEstado(AppConstants.FASE_CERRADA);
        faseRepo.save(fase);

        String nombre = fase.getNombre();

        if (FASE_GRUPOS.equalsIgnoreCase(nombre)) {
            generarLlavesDieciseisavos();
            recalcularMasivoUsuarios(); // Da los puntos de clasificados
        } else if (FASE_16.equalsIgnoreCase(nombre)) abrirFase(FASE_8);
        else if (FASE_8.equalsIgnoreCase(nombre)) abrirFase(FASE_4);
        else if (FASE_4.equalsIgnoreCase(nombre)) abrirFase(FASE_SEMIS);
        else if (FASE_SEMIS.equalsIgnoreCase(nombre)) {
            abrirFase(FASE_3ER);
            abrirFase(FASE_FINAL);
        }

        return " Fase " + nombre + " cerrada.";
    }

    private void abrirFase(String nombreFase) {
        faseRepo.findByNombre(nombreFase).ifPresent(f -> {
            f.setEstado(AppConstants.FASE_ABIERTA);
            faseRepo.save(f);
        });
    }

    private String generarLlavesDieciseisavos() {
        abrirFase(FASE_16);
        Map<String, List<PosicionDTO>> grupos = calcularTablaPosicionesPorGrupo();

        List<Equipo> primeros = new ArrayList<>();
        List<Equipo> segundos = new ArrayList<>();
        List<PosicionDTO> tercerosCandidatos = new ArrayList<>();

        grupos.forEach((nombre, lista) -> {
            if (lista.size() >= 1) primeros.add(equipoRepo.findById(lista.get(0).getEquipoId()).orElse(null));
            if (lista.size() >= 2) segundos.add(equipoRepo.findById(lista.get(1).getEquipoId()).orElse(null));
            if (lista.size() >= 3) tercerosCandidatos.add(lista.get(2));
        });

        // Ordenar terceros por puntos, diferencia de gol, goles a favor
        tercerosCandidatos.sort((a, b) -> {
            if (a.getPuntos() != b.getPuntos()) return b.getPuntos() - a.getPuntos();
            if (a.getDiferenciaGoles() != b.getDiferenciaGoles()) return b.getDiferenciaGoles() - a.getDiferenciaGoles();
            return b.getGolesFavor() - a.getGolesFavor();
        });

        List<Equipo> mejoresTerceros = tercerosCandidatos.stream()
                .limit(8)
                .map(dto -> equipoRepo.findById(dto.getEquipoId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Equipo> clasificados = new ArrayList<>();
        clasificados.addAll(primeros);
        clasificados.addAll(segundos);
        clasificados.addAll(mejoresTerceros);
        clasificados.removeIf(Objects::isNull);

        List<Partido> partidos16 = partidoRepo.findByFaseNombreIgnoreCaseOrderByFechaPartidoAsc(FASE_16);

        int matchIdx = 0;
        for (int i = 0; i < clasificados.size() - 1; i += 2) {
            if (matchIdx < partidos16.size()) {
                Partido p = partidos16.get(matchIdx++);
                p.setEquipoLocal(clasificados.get(i));
                p.setEquipoVisitante(clasificados.get(i + 1));
                partidoRepo.save(p);
                // Si el equipo es Palo, al clasificar a 16avos no suma puntos aquí,
                // los sumará al recalcular usuarios tras cerrar la fase.
            }
        }
        return " Cruces generados.";
    }

    // ==========================================
    // 4. BRACKET Y HELPERS
    // ==========================================
    private String avanzarSiguienteRonda(int numeroPartidoActual, Equipo ganador) {
        Integer siguientePartidoId = null;
        boolean esLocal = true;

        // 16avos -> Octavos
        if (numeroPartidoActual == 73) { siguientePartidoId = 89; esLocal = true; }
        else if (numeroPartidoActual == 74) { siguientePartidoId = 89; esLocal = false; }
        else if (numeroPartidoActual == 75) { siguientePartidoId = 90; esLocal = true; }
        else if (numeroPartidoActual == 76) { siguientePartidoId = 90; esLocal = false; }
        else if (numeroPartidoActual == 77) { siguientePartidoId = 91; esLocal = true; }
        else if (numeroPartidoActual == 78) { siguientePartidoId = 91; esLocal = false; }
        else if (numeroPartidoActual == 79) { siguientePartidoId = 92; esLocal = true; }
        else if (numeroPartidoActual == 80) { siguientePartidoId = 92; esLocal = false; }
        else if (numeroPartidoActual == 81) { siguientePartidoId = 93; esLocal = true; }
        else if (numeroPartidoActual == 82) { siguientePartidoId = 93; esLocal = false; }
        else if (numeroPartidoActual == 83) { siguientePartidoId = 94; esLocal = true; }
        else if (numeroPartidoActual == 84) { siguientePartidoId = 94; esLocal = false; }
        else if (numeroPartidoActual == 85) { siguientePartidoId = 95; esLocal = true; }
        else if (numeroPartidoActual == 86) { siguientePartidoId = 95; esLocal = false; }
        else if (numeroPartidoActual == 87) { siguientePartidoId = 96; esLocal = true; }
        else if (numeroPartidoActual == 88) { siguientePartidoId = 96; esLocal = false; }

        // Octavos -> Cuartos
        else if (numeroPartidoActual == 89) { siguientePartidoId = 97; esLocal = true; }
        else if (numeroPartidoActual == 90) { siguientePartidoId = 97; esLocal = false; }
        else if (numeroPartidoActual == 91) { siguientePartidoId = 98; esLocal = true; }
        else if (numeroPartidoActual == 92) { siguientePartidoId = 98; esLocal = false; }
        else if (numeroPartidoActual == 93) { siguientePartidoId = 99; esLocal = true; }
        else if (numeroPartidoActual == 94) { siguientePartidoId = 99; esLocal = false; }
        else if (numeroPartidoActual == 95) { siguientePartidoId = 100; esLocal = true; }
        else if (numeroPartidoActual == 96) { siguientePartidoId = 100; esLocal = false; }

        // Cuartos -> Semis
        else if (numeroPartidoActual == 97) { siguientePartidoId = 101; esLocal = true; }
        else if (numeroPartidoActual == 98) { siguientePartidoId = 101; esLocal = false; }
        else if (numeroPartidoActual == 99) { siguientePartidoId = 102; esLocal = true; }
        else if (numeroPartidoActual == 100) { siguientePartidoId = 102; esLocal = false; }

        // Semis -> Final y 3er Puesto
        else if (numeroPartidoActual == 101 || numeroPartidoActual == 102) {
            siguientePartidoId = 104; // Final
            esLocal = (numeroPartidoActual == 101);

            // Perdedor al 3er Puesto (103)
            Optional<Partido> pActualOpt = partidoRepo.findAll().stream()
                    .filter(p -> p.getNumeroPartido() != null && p.getNumeroPartido() == numeroPartidoActual)
                    .findFirst();

            if (pActualOpt.isPresent()) {
                Partido pActual = pActualOpt.get();
                Equipo perdedor = pActual.getGolesLocalReal() > pActual.getGolesVisitanteReal()
                        ? pActual.getEquipoVisitante() : pActual.getEquipoLocal();

                final boolean esLocal3ro = esLocal;
                partidoRepo.findAll().stream()
                        .filter(p -> p.getNumeroPartido() != null && p.getNumeroPartido() == 103)
                        .findFirst()
                        .ifPresent(p3 -> {
                            if (esLocal3ro) p3.setEquipoLocal(perdedor);
                            else p3.setEquipoVisitante(perdedor);
                            partidoRepo.save(p3);
                        });
            }
        }

        if (siguientePartidoId != null) {
            Integer finalSig = siguientePartidoId;
            Boolean finalLocal = esLocal;

            Optional<Partido> matchOpt = partidoRepo.findAll().stream()
                    .filter(p -> p.getNumeroPartido() != null && p.getNumeroPartido().equals(finalSig))
                    .findFirst();

            if (matchOpt.isPresent()) {
                Partido siguiente = matchOpt.get();
                if (finalLocal) siguiente.setEquipoLocal(ganador);
                else siguiente.setEquipoVisitante(ganador);
                partidoRepo.save(siguiente);
                return " Ganador " + ganador.getNombre() + " avanza al partido #" + finalSig;
            }
        }
        return " Fin torneo.";
    }

    public Map<String, List<PosicionDTO>> calcularTablaPosicionesPorGrupo() {
        Map<Long, PosicionDTO> stats = calcularEstadisticasBase();
        Map<String, List<PosicionDTO>> tablaPorGrupo = new TreeMap<>();

        for (PosicionDTO dto : stats.values()) {
            equipoRepo.findById(dto.getEquipoId()).ifPresent(equipo -> {
                if (equipo.getGrupo() != null) {
                    String grupo = equipo.getGrupo().getNombre();
                    tablaPorGrupo.putIfAbsent(grupo, new ArrayList<>());
                    tablaPorGrupo.get(grupo).add(dto);
                }
            });
        }
        for (List<PosicionDTO> grupo : tablaPorGrupo.values()) {
            grupo.sort(this::compararEquiposOlimpico);
        }
        return tablaPorGrupo;
    }

    // ✅ MÉTODO RESTAURADO PARA ARREGLAR EL ERROR DEL CONTROLLER
    @Transactional
    public String corregirLlaveFaseFinal(Long partidoId, Long localId, Long visitanteId) {
        Partido partido = partidoRepo.findById(partidoId)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        Equipo local = equipoRepo.findById(localId)
                .orElseThrow(() -> new RuntimeException("Equipo Local no encontrado"));

        Equipo visitante = equipoRepo.findById(visitanteId)
                .orElseThrow(() -> new RuntimeException("Equipo Visitante no encontrado"));

        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partidoRepo.save(partido);
        return "Llave corregida: " + local.getNombre() + " vs " + visitante.getNombre();
    }

    @Transactional
    public String sortearPalos() {
        List<Equipo> palos = equipoRepo.findByEsCandidatoPaloTrue();
        if (palos.isEmpty()) return "Error: No hay equipos candidatos a palo.";

        List<Usuario> usuarios = usuarioRepo.findAll().stream()
                .filter(u -> u.getRol() != null && AppConstants.ROLE_USER.equals(u.getRol().getNombre()))
                .collect(Collectors.toList());

        Collections.shuffle(palos);
        int idx = 0;
        int asignados = 0;

        for (Usuario u : usuarios) {
            PrediccionEspecial pe = prediccionEspecialRepo.findByUsuarioId(u.getId())
                    .orElse(new PrediccionEspecial());
            pe.setUsuario(u);

            if (pe.getEquipoPalo() == null) {
                if (idx >= palos.size()) {
                    idx = 0;
                    Collections.shuffle(palos);
                }
                pe.setEquipoPalo(palos.get(idx++));
                prediccionEspecialRepo.save(pe);
                asignados++;
            }
        }
        return "Sorteo completado. Palos asignados: " + asignados;
    }

    // --- Helpers privados ---

    private Map<Long, PosicionDTO> calcularEstadisticasBase() {
        Map<Long, PosicionDTO> stats = new HashMap<>();

        // Inicializar DTOs para todos los equipos
        equipoRepo.findAll().stream()
                .filter(e -> e.getGrupo() != null)
                .forEach(e -> {
                    PosicionDTO dto = new PosicionDTO();
                    dto.setEquipoId(e.getId());
                    dto.setNombreEquipo(e.getNombre());
                    dto.setUrlEscudo(e.getUrlEscudo());
                    // Inicializamos la lista para evitar NullPointerException
                    dto.setResultados(new ArrayList<>());
                    stats.put(e.getId(), dto);
                });

        // Procesar partidos de grupos FINALIZADOS en orden cronológico
        partidoRepo.findAllByOrderByFechaPartidoAsc().stream() // <--- IMPORTANTE: Ordenar por fecha
                .filter(p -> AppConstants.ESTADO_FINALIZADO.equals(p.getEstado()) &&
                        FASE_GRUPOS.equals(p.getFase().getNombre()))
                .forEach(p -> actualizarEstadisticasPartido(p, stats));

        return stats;
    }

    private void actualizarEstadisticasPartido(Partido p, Map<Long, PosicionDTO> stats) {
        if (p.getEquipoLocal() == null || p.getEquipoVisitante() == null) return;

        PosicionDTO l = stats.get(p.getEquipoLocal().getId());
        PosicionDTO v = stats.get(p.getEquipoVisitante().getId());

        // Si por alguna razón no están en el mapa (ej: equipos eliminados/borrados), salimos
        if (l == null || v == null) return;

        int gl = p.getGolesLocalReal();
        int gv = p.getGolesVisitanteReal();

        // Actualizar Estadísticas Numéricas
        l.setPartidosJugados(l.getPartidosJugados() + 1);
        v.setPartidosJugados(v.getPartidosJugados() + 1);

        l.setGolesFavor(l.getGolesFavor() + gl);
        l.setGolesContra(l.getGolesContra() + gv);
        l.setDiferenciaGoles(l.getGolesFavor() - l.getGolesContra());

        v.setGolesFavor(v.getGolesFavor() + gv);
        v.setGolesContra(v.getGolesContra() + gl);
        v.setDiferenciaGoles(v.getGolesFavor() - v.getGolesContra());

        // Lógica de Puntos y RACHA (Resultados)
        if (gl > gv) {
            // Gana Local
            l.setPuntos(l.getPuntos() + 3);
            l.setPartidosGanados(l.getPartidosGanados() + 1);
            l.getResultados().add("G"); // <--- NUEVO

            v.setPartidosPerdidos(v.getPartidosPerdidos() + 1);
            v.getResultados().add("P"); // <--- NUEVO
        } else if (gv > gl) {
            // Gana Visitante
            v.setPuntos(v.getPuntos() + 3);
            v.setPartidosGanados(v.getPartidosGanados() + 1);
            v.getResultados().add("G"); // <--- NUEVO

            l.setPartidosPerdidos(l.getPartidosPerdidos() + 1);
            l.getResultados().add("P"); // <--- NUEVO
        } else {
            // Empate
            l.setPuntos(l.getPuntos() + 1);
            l.setPartidosEmpatados(l.getPartidosEmpatados() + 1);
            l.getResultados().add("E"); // <--- NUEVO

            v.setPuntos(v.getPuntos() + 1);
            v.setPartidosEmpatados(v.getPartidosEmpatados() + 1);
            v.getResultados().add("E"); // <--- NUEVO
        }
    }

    private int compararEquiposOlimpico(PosicionDTO a, PosicionDTO b) {
        if (a.getPuntos() != b.getPuntos()) return b.getPuntos() - a.getPuntos();
        if (a.getDiferenciaGoles() != b.getDiferenciaGoles()) return b.getDiferenciaGoles() - a.getDiferenciaGoles();
        return b.getGolesFavor() - a.getGolesFavor();
    }

    private Equipo determinarGanador(Partido p) {
        if (p.getGolesLocalReal() > p.getGolesVisitanteReal()) return p.getEquipoLocal();
        if (p.getGolesVisitanteReal() > p.getGolesLocalReal()) return p.getEquipoVisitante();
        return null;
    }
}