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
    private final FaseRepository faseRepo;
    private final JugadorRepository jugadorRepo;
    private final ResultadoOficialRepository resultadoRepo;

    public TorneoService(PartidoRepository partidoRepo, EquipoRepository equipoRepo,
                         PrediccionRepository prediccionRepo, UsuarioRepository usuarioRepo,
                         PuntosService puntosService, PrediccionEspecialRepository prediccionEspecialRepo,
                         FaseRepository faseRepo,
                         JugadorRepository jugadorRepo, ResultadoOficialRepository resultadoRepo) {
        this.partidoRepo = partidoRepo;
        this.equipoRepo = equipoRepo;
        this.prediccionRepo = prediccionRepo;
        this.usuarioRepo = usuarioRepo;
        this.puntosService = puntosService;
        this.prediccionEspecialRepo = prediccionEspecialRepo;
        this.faseRepo = faseRepo;
        this.jugadorRepo = jugadorRepo;
        this.resultadoRepo = resultadoRepo;
    }

    // ==========================================
    // 1. REGISTRO DE RESULTADOS (LÓGICA AUTOMÁTICA PENALES)
    // ==========================================
    @Transactional
    public String registrarResultadoPartido(Long partidoId, Integer golesLocal, Integer golesVisitante,
                                            Integer penalesLocal, Integer penalesVisitante) {

        Partido partido = partidoRepo.findById(partidoId)
                .orElseThrow(() -> new RuntimeException(AppConstants.ERR_PARTIDO_NO_ENCONTRADO));

        // 1. Guardar Marcador Regular
        partido.setGolesLocalReal(golesLocal);
        partido.setGolesVisitanteReal(golesVisitante);
        partido.setEstado(AppConstants.ESTADO_FINALIZADO);

        boolean esFaseGrupos = AppConstants.FASE_GRUPOS.equalsIgnoreCase(partido.getFase().getNombre());

        // 2. Determinar Ganador (Calculado, no recibido)
        if (golesLocal > golesVisitante) {
            partido.setGanador(partido.getEquipoLocal());
            partido.setGolesPenalesLocal(null);
            partido.setGolesPenalesVisitante(null);
        } else if (golesVisitante > golesLocal) {
            partido.setGanador(partido.getEquipoVisitante());
            partido.setGolesPenalesLocal(null);
            partido.setGolesPenalesVisitante(null);
        } else {
            // --- EMPATE EN TIEMPO REGULAR ---
            if (esFaseGrupos) {
                partido.setGanador(null); // Empate válido en grupos
            } else {
                // --- FASE FINAL: DEFINICIÓN POR PENALES ---
                if (penalesLocal == null || penalesVisitante == null) {
                    throw new RuntimeException("ERROR: Empate en fase final. Debe ingresar el marcador de penales.");
                }
                if (penalesLocal.equals(penalesVisitante)) {
                    throw new RuntimeException("ERROR: Los penales no pueden terminar en empate.");
                }

                // Guardar marcador de penales
                partido.setGolesPenalesLocal(penalesLocal);
                partido.setGolesPenalesVisitante(penalesVisitante);

                // Calcular ganador basado en penales
                if (penalesLocal > penalesVisitante) {
                    partido.setGanador(partido.getEquipoLocal());
                } else {
                    partido.setGanador(partido.getEquipoVisitante());
                }
            }
        }

        partidoRepo.save(partido);

        // 3. Actualizar Puntos Usuarios
        List<Prediccion> apuestas = prediccionRepo.findByPartidoId(partido.getId());
        for (Prediccion p : apuestas) {
            puntosService.recalcularPuntosUsuario(p.getUsuario().getId());
        }

        StringBuilder mensaje = new StringBuilder("Resultado guardado. ");

        // 4. Avanzar Ronda (Bracket)
        if (!esFaseGrupos && partido.getGanador() != null) {
            String avanceMsg = avanzarSiguienteRonda(partido.getNumeroPartido(), partido.getGanador());
            mensaje.append(avanceMsg);
        }

        // 5. Verificar Cierre de Fase
        boolean faseTerminada = partidoRepo.findByFaseIdOrderByFechaPartidoAsc(partido.getFase().getId())
                .stream()
                .allMatch(p -> AppConstants.ESTADO_FINALIZADO.equals(p.getEstado()));

        if (faseTerminada) {
            mensaje.append(cerrarFase(partido.getFase().getId()));
        }

        return mensaje.toString();
    }

    // ==========================================
    // MÉTODOS DE APOYO (Idénticos al flujo original)
    // ==========================================
    @Transactional
    public String procesarPuntosCampeon(Long equipoId) {
        Equipo campeon = equipoRepo.findById(equipoId).orElseThrow();
        ResultadoOficial resultado = resultadoRepo.findById(1L).orElse(new ResultadoOficial());
        resultado.setId(1L); resultado.setEquipoCampeon(campeon);
        resultadoRepo.save(resultado);
        recalcularMasivoUsuarios();
        return "Campeón registrado.";
    }

    @Transactional
    public String procesarPuntosGoleador(Long jugadorId) {
        Jugador goleador = jugadorRepo.findById(jugadorId).orElseThrow();
        ResultadoOficial resultado = resultadoRepo.findById(1L).orElse(new ResultadoOficial());
        resultado.setId(1L); resultado.setJugadorGoleador(goleador);
        resultadoRepo.save(resultado);
        recalcularMasivoUsuarios();
        return "Goleador registrado.";
    }

    private int recalcularMasivoUsuarios() {
        List<Usuario> usuarios = usuarioRepo.findAll();
        int c = 0;
        for (Usuario u : usuarios) {
            if (u.getRol() != null && AppConstants.ROLE_USER.equals(u.getRol().getNombre())) {
                puntosService.recalcularPuntosUsuario(u.getId());
                c++;
            }
        }
        return c;
    }

    @Transactional
    public String cerrarFase(Long faseId) {
        Fase fase = faseRepo.findById(faseId).orElseThrow();
        if (AppConstants.FASE_CERRADA.equals(fase.getEstado())) return " (Fase ya cerrada).";
        fase.setEstado(AppConstants.FASE_CERRADA);
        faseRepo.save(fase);

        if (AppConstants.FASE_GRUPOS.equalsIgnoreCase(fase.getNombre())) {
            generarLlavesDieciseisavos();
            recalcularMasivoUsuarios();
        } else if (AppConstants.FASE_DIECISEISAVOS.equalsIgnoreCase(fase.getNombre())) abrirFase(AppConstants.FASE_OCTAVOS);
        else if (AppConstants.FASE_OCTAVOS.equalsIgnoreCase(fase.getNombre())) abrirFase(AppConstants.FASE_CUARTOS);
        else if (AppConstants.FASE_CUARTOS.equalsIgnoreCase(fase.getNombre())) abrirFase(AppConstants.FASE_SEMIFINALES);
        else if (AppConstants.FASE_SEMIFINALES.equalsIgnoreCase(fase.getNombre())) {
            abrirFase(AppConstants.FASE_TERCER_PUESTO);
            abrirFase(AppConstants.FASE_FINAL);
        }
        return " Fase cerrada.";
    }

    private void abrirFase(String nombre) {
        faseRepo.findByNombre(nombre).ifPresent(f -> { f.setEstado(AppConstants.FASE_ABIERTA); faseRepo.save(f); });
    }

    // --- BRACKET LOGIC ---
    private String avanzarSiguienteRonda(int numeroPartidoActual, Equipo ganador) {
        Integer siguientePartidoId = null;
        boolean finalLocal = true;

        if (numeroPartidoActual == 73) { siguientePartidoId = 90; finalLocal = true; }
        else if (numeroPartidoActual == 74) { siguientePartidoId = 89; finalLocal = true; }
        else if (numeroPartidoActual == 75) { siguientePartidoId = 90; finalLocal = false; }
        else if (numeroPartidoActual == 76) { siguientePartidoId = 91; finalLocal = true; }
        else if (numeroPartidoActual == 77) { siguientePartidoId = 89; finalLocal = false; }
        else if (numeroPartidoActual == 78) { siguientePartidoId = 91; finalLocal = false; }
        else if (numeroPartidoActual == 79) { siguientePartidoId = 92; finalLocal = true; }
        else if (numeroPartidoActual == 80) { siguientePartidoId = 92; finalLocal = false; }
        else if (numeroPartidoActual == 81) { siguientePartidoId = 94; finalLocal = true; }
        else if (numeroPartidoActual == 82) { siguientePartidoId = 94; finalLocal = false; }
        else if (numeroPartidoActual == 83) { siguientePartidoId = 93; finalLocal = true; }
        else if (numeroPartidoActual == 84) { siguientePartidoId = 93; finalLocal = false; }
        else if (numeroPartidoActual == 85) { siguientePartidoId = 96; finalLocal = true; }
        else if (numeroPartidoActual == 86) { siguientePartidoId = 95; finalLocal = true; }
        else if (numeroPartidoActual == 87) { siguientePartidoId = 96; finalLocal = false; }
        else if (numeroPartidoActual == 88) { siguientePartidoId = 95; finalLocal = false; }

        else if (numeroPartidoActual == 89) { siguientePartidoId = 97; finalLocal = true; }
        else if (numeroPartidoActual == 90) { siguientePartidoId = 97; finalLocal = false; }
        else if (numeroPartidoActual == 91) { siguientePartidoId = 99; finalLocal = true; }
        else if (numeroPartidoActual == 92) { siguientePartidoId = 99; finalLocal = false; }
        else if (numeroPartidoActual == 93) { siguientePartidoId = 98; finalLocal = true; }
        else if (numeroPartidoActual == 94) { siguientePartidoId = 98; finalLocal = false; }
        else if (numeroPartidoActual == 95) { siguientePartidoId = 100; finalLocal = true; }
        else if (numeroPartidoActual == 96) { siguientePartidoId = 100; finalLocal = false; }

        else if (numeroPartidoActual == 97) { siguientePartidoId = 101; finalLocal = true; }
        else if (numeroPartidoActual == 98) { siguientePartidoId = 101; finalLocal = false; }
        else if (numeroPartidoActual == 99) { siguientePartidoId = 102; finalLocal = true; }
        else if (numeroPartidoActual == 100) { siguientePartidoId = 102; finalLocal = false; }

        else if (numeroPartidoActual == 101 || numeroPartidoActual == 102) {
            siguientePartidoId = 104;
            finalLocal = (numeroPartidoActual == 101);
            boolean finalLocal3ro = finalLocal;

            partidoRepo.findByNumeroPartido(numeroPartidoActual).ifPresent(pActual -> {
                Equipo perdedor = pActual.getGanador().getId().equals(pActual.getEquipoLocal().getId())
                        ? pActual.getEquipoVisitante() : pActual.getEquipoLocal();

                partidoRepo.findByNumeroPartido(103).ifPresent(p3 -> {
                    if (finalLocal3ro) p3.setEquipoLocal(perdedor);
                    else p3.setEquipoVisitante(perdedor);
                    partidoRepo.save(p3);
                });
            });
        }

        if (siguientePartidoId != null) {
            boolean esLocal = finalLocal;
            Optional<Partido> matchOpt = partidoRepo.findByNumeroPartido(siguientePartidoId);
            if (matchOpt.isPresent()) {
                Partido siguiente = matchOpt.get();
                if (esLocal) siguiente.setEquipoLocal(ganador);
                else siguiente.setEquipoVisitante(ganador);
                partidoRepo.save(siguiente);
                return " -> Avanza " + ganador.getNombre();
            }
        }
        return "";
    }

    private String generarLlavesDieciseisavos() {
        abrirFase(AppConstants.FASE_DIECISEISAVOS);
        Map<String, List<PosicionDTO>> tablas = calcularTablaPosicionesPorGrupo();
        Map<String, Equipo> primeros = new HashMap<>();
        Map<String, Equipo> segundos = new HashMap<>();
        List<PosicionDTO> todosTerceros = new ArrayList<>();

        tablas.forEach((grupo, lista) -> {
            if (!lista.isEmpty()) primeros.put(grupo, equipoRepo.findById(lista.get(0).getEquipoId()).orElse(null));
            if (lista.size() >= 2) segundos.put(grupo, equipoRepo.findById(lista.get(1).getEquipoId()).orElse(null));
            if (lista.size() >= 3) todosTerceros.add(lista.get(2));
        });

        todosTerceros.sort((a, b) -> {
            if (a.getPuntos() != b.getPuntos()) return b.getPuntos() - a.getPuntos();
            if (a.getDiferenciaGoles() != b.getDiferenciaGoles()) return b.getDiferenciaGoles() - a.getDiferenciaGoles();
            return b.getGolesFavor() - a.getGolesFavor();
        });

        List<Equipo> mejoresTerceros = todosTerceros.stream().limit(8)
                .map(dto -> equipoRepo.findById(dto.getEquipoId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        asignarPartido(73, segundos.get(AppConstants.GRUPO_A), segundos.get(AppConstants.GRUPO_B));
        asignarPartido(75, primeros.get(AppConstants.GRUPO_F), segundos.get(AppConstants.GRUPO_C));
        asignarPartido(76, primeros.get(AppConstants.GRUPO_C), segundos.get(AppConstants.GRUPO_F));
        asignarPartido(78, segundos.get(AppConstants.GRUPO_E), segundos.get(AppConstants.GRUPO_I));
        asignarPartido(83, segundos.get(AppConstants.GRUPO_K), segundos.get(AppConstants.GRUPO_L));
        asignarPartido(84, primeros.get(AppConstants.GRUPO_H), segundos.get(AppConstants.GRUPO_J));
        asignarPartido(86, primeros.get(AppConstants.GRUPO_J), segundos.get(AppConstants.GRUPO_H));
        asignarPartido(88, segundos.get(AppConstants.GRUPO_D), segundos.get(AppConstants.GRUPO_G));

        Map<Integer, String> llavesConTerceros = getIntegerStringMap();
        Map<Integer, Equipo> asignacionResuelta = resolverCrucesTerceros(llavesConTerceros, mejoresTerceros);

        asignacionResuelta.forEach((nPartido, equipoTercero) -> {
            String grupoCabeza = llavesConTerceros.get(nPartido);
            Equipo equipoCabeza = primeros.get(grupoCabeza);
            asignarPartido(nPartido, equipoCabeza, equipoTercero);
        });

        return "Cruces 16avos OK.";
    }

    private static Map<Integer, String> getIntegerStringMap() {
        Map<Integer, String> llaves = new LinkedHashMap<>();
        llaves.put(74, AppConstants.GRUPO_E); llaves.put(77, AppConstants.GRUPO_I);
        llaves.put(79, AppConstants.GRUPO_A); llaves.put(80, AppConstants.GRUPO_L);
        llaves.put(81, AppConstants.GRUPO_D); llaves.put(82, AppConstants.GRUPO_G);
        llaves.put(85, AppConstants.GRUPO_B); llaves.put(87, AppConstants.GRUPO_K);
        return llaves;
    }

    private void asignarPartido(int numeroPartido, Equipo local, Equipo visitante) {
        if (local == null || visitante == null) return;
        partidoRepo.findAll().stream()
                .filter(p -> p.getNumeroPartido() != null && p.getNumeroPartido() == numeroPartido)
                .findFirst().ifPresent(p -> {
                    p.setEquipoLocal(local);
                    p.setEquipoVisitante(visitante);
                    partidoRepo.save(p);
                });
    }

    private Map<Integer, Equipo> resolverCrucesTerceros(Map<Integer, String> llaves, List<Equipo> tercerosDisponibles) {
        Map<Integer, Equipo> solucion = new HashMap<>();
        List<Integer> ordenPartidos = new ArrayList<>(llaves.keySet());
        if (!backtrackAsignacion(0, ordenPartidos, llaves, new ArrayList<>(tercerosDisponibles), solucion)) {
            for (int i = 0; i < ordenPartidos.size(); i++) {
                if(i < tercerosDisponibles.size()) solucion.put(ordenPartidos.get(i), tercerosDisponibles.get(i));
            }
        }
        return solucion;
    }

    private boolean backtrackAsignacion(int indice, List<Integer> partidos, Map<Integer, String> infoCabezas,
                                        List<Equipo> tercerosRestantes, Map<Integer, Equipo> solucionActual) {
        if (indice == partidos.size()) return true;
        int nPartido = partidos.get(indice);
        String grupoCabezaSerie = infoCabezas.get(nPartido);
        for (int i = 0; i < tercerosRestantes.size(); i++) {
            Equipo tercero = tercerosRestantes.get(i);
            String grupoTercero = (tercero.getGrupo() != null) ? tercero.getGrupo().getNombre() : "";
            if (!grupoCabezaSerie.equalsIgnoreCase(grupoTercero)) {
                solucionActual.put(nPartido, tercero);
                Equipo removido = tercerosRestantes.remove(i);
                if (backtrackAsignacion(indice + 1, partidos, infoCabezas, tercerosRestantes, solucionActual)) return true;
                tercerosRestantes.add(i, removido);
                solucionActual.remove(nPartido);
            }
        }
        return false;
    }

    // ... Helpers de Estadísticas, Sorteo, Corrección Llaves ...
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
        for (List<PosicionDTO> grupo : tablaPorGrupo.values()) grupo.sort(this::compararEquiposOlimpico);
        return tablaPorGrupo;
    }

    private Map<Long, PosicionDTO> calcularEstadisticasBase() {
        Map<Long, PosicionDTO> stats = new HashMap<>();
        equipoRepo.findAll().stream().filter(e -> e.getGrupo() != null).forEach(e -> {
            PosicionDTO dto = new PosicionDTO();
            dto.setEquipoId(e.getId());
            dto.setNombreEquipo(e.getNombre());
            dto.setUrlEscudo(e.getUrlEscudo());
            dto.setResultados(new ArrayList<>());
            stats.put(e.getId(), dto);
        });
        partidoRepo.findAllByOrderByFechaPartidoAsc().stream()
                .filter(p -> AppConstants.ESTADO_FINALIZADO.equals(p.getEstado()) &&
                        AppConstants.FASE_GRUPOS.equals(p.getFase().getNombre()))
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

        l.setPartidosJugados(l.getPartidosJugados() + 1);
        v.setPartidosJugados(v.getPartidosJugados() + 1);
        l.setGolesFavor(l.getGolesFavor() + gl);
        l.setGolesContra(l.getGolesContra() + gv);
        l.setDiferenciaGoles(l.getGolesFavor() - l.getGolesContra());
        v.setGolesFavor(v.getGolesFavor() + gv);
        v.setGolesContra(v.getGolesContra() + gl);
        v.setDiferenciaGoles(v.getGolesFavor() - v.getGolesContra());

        if (gl > gv) {
            l.setPuntos(l.getPuntos() + 3); l.setPartidosGanados(l.getPartidosGanados() + 1);
            l.getResultados().add(AppConstants.PARTIDO_GANADO);
            v.setPartidosPerdidos(v.getPartidosPerdidos() + 1);
            v.getResultados().add(AppConstants.PARTIDO_PERDIDO);
        } else if (gv > gl) {
            v.setPuntos(v.getPuntos() + 3); v.setPartidosGanados(v.getPartidosGanados() + 1);
            v.getResultados().add(AppConstants.PARTIDO_GANADO);
            l.setPartidosPerdidos(l.getPartidosPerdidos() + 1);
            l.getResultados().add(AppConstants.PARTIDO_PERDIDO);
        } else {
            l.setPuntos(l.getPuntos() + 1); l.setPartidosEmpatados(l.getPartidosEmpatados() + 1);
            l.getResultados().add(AppConstants.PARTIDO_EMPATADO);
            v.setPuntos(v.getPuntos() + 1); v.setPartidosEmpatados(v.getPartidosEmpatados() + 1);
            v.getResultados().add(AppConstants.PARTIDO_EMPATADO);
        }
    }

    private int compararEquiposOlimpico(PosicionDTO a, PosicionDTO b) {
        if (a.getPuntos() != b.getPuntos()) return b.getPuntos() - a.getPuntos();
        if (a.getDiferenciaGoles() != b.getDiferenciaGoles()) return b.getDiferenciaGoles() - a.getDiferenciaGoles();
        return b.getGolesFavor() - a.getGolesFavor();
    }

    @Transactional
    public String corregirLlaveFaseFinal(Long pId, Long lId, Long vId) {
        Partido p = partidoRepo.findById(pId).orElseThrow();
        p.setEquipoLocal(equipoRepo.findById(lId).orElseThrow());
        p.setEquipoVisitante(equipoRepo.findById(vId).orElseThrow());
        partidoRepo.save(p);
        return "OK";
    }

    @Transactional
    public String sortearPalos() {
        List<Equipo> palos = equipoRepo.findByEsCandidatoPaloTrue();
        if (palos.isEmpty()) return AppConstants.ERR_EQUIPO_PALO_NO_ENCONTRADO;
        List<Usuario> usuarios = usuarioRepo.findAll().stream()
                .filter(u -> u.getRol() != null && AppConstants.ROLE_USER.equals(u.getRol().getNombre()))
                .toList();
        Collections.shuffle(palos);
        int idx = 0;
        int asignados = 0;
        for (Usuario u : usuarios) {
            PrediccionEspecial pe = prediccionEspecialRepo.findByUsuarioId(u.getId()).orElse(new PrediccionEspecial());
            pe.setUsuario(u);
            if (pe.getEquipoPalo() == null) {
                if (idx >= palos.size()) { idx = 0; Collections.shuffle(palos); }
                pe.setEquipoPalo(palos.get(idx++));
                prediccionEspecialRepo.save(pe);
                asignados++;
            }
        }
        return "Sorteo OK. Asignados: " + asignados;
    }

    public boolean esTorneoFinalizado() {
        return resultadoRepo.existsById(1L) && partidoRepo.findByNumeroPartido(104)
                .map(p -> AppConstants.ESTADO_FINALIZADO.equals(p.getEstado())).orElse(false);
    }
}