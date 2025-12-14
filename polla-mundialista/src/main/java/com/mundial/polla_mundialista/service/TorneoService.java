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
                         ConfiguracionRepository configRepo, RolRepository rolRepo, FaseRepository faseRepo) {
        this.partidoRepo = partidoRepo;
        this.equipoRepo = equipoRepo;
        this.prediccionRepo = prediccionRepo;
        this.usuarioRepo = usuarioRepo;
        this.puntosService = puntosService;
        this.prediccionEspecialRepo = prediccionEspecialRepo;
        this.configRepo = configRepo;
        this.rolRepo = rolRepo;
        this.faseRepo = faseRepo;
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

        // A. Calcular Puntos de Usuarios
        calcularPuntosDePredicciones(partido);

        StringBuilder mensaje = new StringBuilder("Resultado guardado. ");
        Fase faseActual = partido.getFase();

        // B. Avanzar Ganador (Solo fases eliminatorias)
        // Se ejecuta SIEMPRE que no sea grupos, independiente de si la fase termina o no.
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
    // 2. GESTIÓN DE FASES (Cierre y Apertura)
    // ==========================================
    @Transactional
    public String cerrarFase(Long faseId) {
        Fase fase = faseRepo.findById(faseId).orElseThrow();

        if (AppConstants.FASE_CERRADA.equals(fase.getEstado())) {
            return " (Fase ya estaba cerrada).";
        }

        fase.setEstado(AppConstants.FASE_CERRADA);
        faseRepo.save(fase);

        String nombre = fase.getNombre();
        String msgExtra = "";

        // Lógica de encadenamiento de fases
        if (FASE_GRUPOS.equalsIgnoreCase(nombre)) {
            msgExtra = generarLlavesDieciseisavos(); // Este método abre la fase 16avos internamente
        } else if (FASE_16.equalsIgnoreCase(nombre)) {
            abrirFase(FASE_8);
            msgExtra = " Octavos de Final INICIADOS.";
        } else if (FASE_8.equalsIgnoreCase(nombre)) {
            abrirFase(FASE_4);
            msgExtra = " Cuartos de Final INICIADOS.";
        } else if (FASE_4.equalsIgnoreCase(nombre)) {
            abrirFase(FASE_SEMIS);
            msgExtra = " Semifinales INICIADAS.";
        } else if (FASE_SEMIS.equalsIgnoreCase(nombre)) {
            abrirFase(FASE_3ER);
            abrirFase(FASE_FINAL);
            msgExtra = " ¡Final y 3er Puesto Listos!";
        }

        return " Fase " + nombre + " cerrada." + msgExtra;
    }

    private void abrirFase(String nombreFase) {
        faseRepo.findByNombre(nombreFase).ifPresent(f -> {
            f.setEstado(AppConstants.FASE_ABIERTA);
            faseRepo.save(f);
        });
    }

    // ==========================================
    // 3. GENERACIÓN DE CRUCES (16avos)
    // ==========================================
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

        // Ordenar terceros
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

                if (Boolean.TRUE.equals(p.getEquipoLocal().getEsCandidatoPalo())) premiarPalo(p.getEquipoLocal(), obtenerPuntosPalo());
                if (Boolean.TRUE.equals(p.getEquipoVisitante().getEsCandidatoPalo())) premiarPalo(p.getEquipoVisitante(), obtenerPuntosPalo());
            }
        }
        return " Cruces generados.";
    }

    // ==========================================
    // 4. BRACKET (Árbol de Fases Finales)
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

                if (Boolean.TRUE.equals(ganador.getEsCandidatoPalo())) {
                    premiarPalo(ganador, obtenerPuntosPalo());
                }
                return " Ganador " + ganador.getNombre() + " avanza al partido #" + finalSig;
            }
        }
        return " Fin torneo.";
    }

    // ==========================================
    // 5. OTROS MÉTODOS Y HELPERS
    // ==========================================
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

    @Transactional
    public String corregirLlaveFaseFinal(Long partidoId, Long localId, Long visitanteId) {
        Partido partido = partidoRepo.findById(partidoId).orElseThrow(() -> new RuntimeException("Partido no encontrado"));
        Equipo local = equipoRepo.findById(localId).orElseThrow();
        Equipo visitante = equipoRepo.findById(visitanteId).orElseThrow();
        partido.setEquipoLocal(local);
        partido.setEquipoVisitante(visitante);
        partidoRepo.save(partido);
        return "Llave corregida: " + local.getNombre() + " vs " + visitante.getNombre();
    }

    @Transactional
    public String procesarPuntosCampeon(Long equipoId) {
        int pts = puntosService.obtenerPuntosPorTipo(AppConstants.CONF_PUNTOS_CAMPEON);
        int c = 0;
        for (PrediccionEspecial p : prediccionEspecialRepo.findAll()) {
            if (p.getEquipoCampeon() != null && p.getEquipoCampeon().getId().equals(equipoId)) {
                sumarPuntos(p.getUsuario(), pts);
                c++;
            }
        }
        return "Puntos Campeón asignados a " + c + " usuarios.";
    }

    @Transactional
    public String procesarPuntosGoleador(Long jugadorId) {
        int pts = puntosService.obtenerPuntosPorTipo(AppConstants.CONF_PUNTOS_GOLEADOR);
        int c = 0;
        for (PrediccionEspecial p : prediccionEspecialRepo.findAll()) {
            if (p.getJugadorGoleador() != null && p.getJugadorGoleador().getId().equals(jugadorId)) {
                sumarPuntos(p.getUsuario(), pts);
                c++;
            }
        }
        return "Puntos Goleador asignados a " + c + " usuarios.";
    }

    @Transactional
    public String sortearPalos() {
        // 1. Obtener equipos candidatos
        List<Equipo> palos = equipoRepo.findByEsCandidatoPaloTrue();
        if (palos.isEmpty()) return "Error: No hay equipos marcados como 'Candidato a Palo' en la base de datos.";

        // 2. Obtener usuarios participantes (CORRECCIÓN IMPORTANTE: Comparar por nombre de rol)
        // Evitamos u.getRol().equals(rolObjeto) porque falla si no hay equals() en la entidad.
        List<Usuario> usuarios = usuarioRepo.findAll().stream()
                .filter(u -> u.getRol() != null && AppConstants.ROLE_USER.equals(u.getRol().getNombre()))
                .collect(Collectors.toList());

        if (usuarios.isEmpty()) return "Error: No se encontraron usuarios con rol " + AppConstants.ROLE_USER;

        // 3. Asignación aleatoria
        Collections.shuffle(palos);
        int idx = 0;
        int asignados = 0;

        for (Usuario u : usuarios) {
            // Buscamos si ya tiene predicción especial, si no, creamos una
            PrediccionEspecial pe = prediccionEspecialRepo.findByUsuarioId(u.getId())
                    .orElse(new PrediccionEspecial());

            pe.setUsuario(u); // Aseguramos la relación

            // Solo asignamos si aún no tiene palo (para no sobrescribir sorteos previos si se corre 2 veces)
            if (pe.getEquipoPalo() == null) {
                // Si se acaban los equipos, barajamos y empezamos de nuevo (ciclo circular)
                if (idx >= palos.size()) {
                    idx = 0;
                    Collections.shuffle(palos);
                }

                Equipo paloAsignado = palos.get(idx++);
                pe.setEquipoPalo(paloAsignado);

                prediccionEspecialRepo.save(pe);
                asignados++;
            }
        }
        return "Sorteo OK. Se asignaron equipos sorpresa a " + asignados + " usuarios.";
    }

    private void sumarPuntos(Usuario u, int puntos) {
        u.setPuntosTotales((u.getPuntosTotales() == null ? 0 : u.getPuntosTotales()) + puntos);
        usuarioRepo.save(u);
    }

    private void calcularPuntosDePredicciones(Partido partido) {
        List<Prediccion> apuestas = prediccionRepo.findByPartidoId(partido.getId());
        for (Prediccion p : apuestas) {
            Usuario u = p.getUsuario();
            if (p.getPuntosGanados() > 0) u.setPuntosTotales(Math.max(u.getPuntosTotales() - p.getPuntosGanados(), 0));
            int nuevos = puntosService.calcularPuntosPartido(p, partido);
            p.setPuntosGanados(nuevos);
            u.setPuntosTotales((u.getPuntosTotales() == null ? 0 : u.getPuntosTotales()) + nuevos);
            prediccionRepo.save(p);
            usuarioRepo.save(u);
        }
    }

    public int premiarPalo(Equipo equipo, int puntos) {
        int cont = 0;
        for (PrediccionEspecial pe : prediccionEspecialRepo.findAll()) {
            if (pe.getEquipoPalo() != null && pe.getEquipoPalo().getId().equals(equipo.getId())) {
                sumarPuntos(pe.getUsuario(), puntos);
                cont++;
            }
        }
        return cont;
    }

    private int obtenerPuntosPalo() {
        return configRepo.findByClave(AppConstants.CONF_PUNTOS_PALO).map(c -> Integer.parseInt(c.getValor())).orElse(5);
    }

    private Map<Long, PosicionDTO> calcularEstadisticasBase() {
        Map<Long, PosicionDTO> stats = new HashMap<>();
        equipoRepo.findAll().stream().filter(e -> e.getGrupo() != null).forEach(e -> {
            PosicionDTO dto = new PosicionDTO();
            dto.setEquipoId(e.getId());
            dto.setNombreEquipo(e.getNombre());
            dto.setUrlEscudo(e.getUrlEscudo());
            stats.put(e.getId(), dto);
        });
        partidoRepo.findAll().stream()
                .filter(p -> AppConstants.ESTADO_FINALIZADO.equals(p.getEstado()) && FASE_GRUPOS.equals(p.getFase().getNombre()))
                .forEach(p -> actualizarEstadisticasPartido(p, stats));
        return stats;
    }

    private void actualizarEstadisticasPartido(Partido p, Map<Long, PosicionDTO> stats) {
        if (p.getEquipoLocal() == null || p.getEquipoVisitante() == null) return;
        PosicionDTO l = stats.get(p.getEquipoLocal().getId());
        PosicionDTO v = stats.get(p.getEquipoVisitante().getId());
        if (l == null || v == null) return;

        int gl = p.getGolesLocalReal();
        int gv = p.getGolesVisitanteReal();

        l.setGolesFavor(l.getGolesFavor() + gl); l.setGolesContra(l.getGolesContra() + gv);
        l.setDiferenciaGoles(l.getGolesFavor() - l.getGolesContra()); l.setPartidosJugados(l.getPartidosJugados() + 1);

        v.setGolesFavor(v.getGolesFavor() + gv); v.setGolesContra(v.getGolesContra() + gl);
        v.setDiferenciaGoles(v.getGolesFavor() - v.getGolesContra()); v.setPartidosJugados(v.getPartidosJugados() + 1);

        if (gl > gv) {
            l.setPuntos(l.getPuntos() + 3); l.setPartidosGanados(l.getPartidosGanados() + 1);
            v.setPartidosPerdidos(v.getPartidosPerdidos() + 1);
        } else if (gv > gl) {
            v.setPuntos(v.getPuntos() + 3); v.setPartidosGanados(v.getPartidosGanados() + 1);
            l.setPartidosPerdidos(l.getPartidosPerdidos() + 1);
        } else {
            l.setPuntos(l.getPuntos() + 1); l.setPartidosEmpatados(l.getPartidosEmpatados() + 1);
            v.setPuntos(v.getPuntos() + 1); v.setPartidosEmpatados(v.getPartidosEmpatados() + 1);
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
        return null; // Empate en mata-mata (Debería haber penales, aquí asumimos null)
    }
}