package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.dto.DesglosePuntosDTO;
import com.mundial.polla_mundialista.dto.RankingDetalladoDTO;
import com.mundial.polla_mundialista.entity.Usuario;
import com.mundial.polla_mundialista.repository.UsuarioRepository;
import com.mundial.polla_mundialista.service.PuntosService;
import com.mundial.polla_mundialista.util.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PuntosService puntosService;

    // ==========================================
    // ⚖️ LOGICA DE ORDENAMIENTO (RANKING)
    // ==========================================
    private final Comparator<Usuario> comparadorRanking = (u1, u2) -> {
        // 1. PUNTOS TOTALES (Mayor gana)
        int comparePuntos = Integer.compare(u2.getPuntosTotales(), u1.getPuntosTotales());
        if (comparePuntos != 0) return comparePuntos;

        // 2. MARCADORES EXACTOS (Mayor gana)
        int exactos1 = u1.getCantidadAciertosExactos() != null ? u1.getCantidadAciertosExactos() : 0;
        int exactos2 = u2.getCantidadAciertosExactos() != null ? u2.getCantidadAciertosExactos() : 0;
        int compareExactos = Integer.compare(exactos2, exactos1);
        if (compareExactos != 0) return compareExactos;

        // 3. ACIERTOS DE GANADOR (Mayor gana)
        int ganador1 = u1.getCantidadAciertosGanador() != null ? u1.getCantidadAciertosGanador() : 0;
        int ganador2 = u2.getCantidadAciertosGanador() != null ? u2.getCantidadAciertosGanador() : 0;
        int compareGanador = Integer.compare(ganador2, ganador1);
        if (compareGanador != 0) return compareGanador;

        // 4. ACERTO CAMPEÓN (True gana a False)
        boolean camp1 = Boolean.TRUE.equals(u1.getAcertoCampeon());
        boolean camp2 = Boolean.TRUE.equals(u2.getAcertoCampeon());
        if (camp1 != camp2) {
            return camp1 ? -1 : 1; // Si u1 acertó (-1), va antes.
        }

        // 5. FECHA DE PREDICCIÓN (Menor fecha gana - "El que madruga")
        if (u1.getUltimaFechaPrediccion() == null && u2.getUltimaFechaPrediccion() == null) return 0;
        if (u1.getUltimaFechaPrediccion() == null) return 1; // u1 al final
        if (u2.getUltimaFechaPrediccion() == null) return -1; // u2 al final

        return u1.getUltimaFechaPrediccion().compareTo(u2.getUltimaFechaPrediccion());
    };

    // ==========================================
    // ENDPOINTS
    // ==========================================

    @GetMapping("/ranking-completo")
    public ResponseEntity<List<RankingDetalladoDTO>> obtenerRankingCompleto() {
        List<Usuario> usuarios = usuarioRepository.findByRolNombreOrderByPuntosTotalesDesc(AppConstants.ROLE_USER);

        // Aplicar el super comparador
        usuarios.sort(comparadorRanking);

        List<RankingDetalladoDTO> ranking = new ArrayList<>();
        for (Usuario u : usuarios) {
            DesglosePuntosDTO desglose = puntosService.obtenerDesgloseUsuario(u.getId());
            RankingDetalladoDTO dto = getRankingDetalladoDTO(u, desglose);
            ranking.add(dto);
        }
        return ResponseEntity.ok(ranking);
    }

    private static RankingDetalladoDTO getRankingDetalladoDTO(Usuario u, DesglosePuntosDTO desglose) {
        RankingDetalladoDTO dto = new RankingDetalladoDTO();
        dto.setUsuarioId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setAvatar(u.getUsername().length() > 2 ? u.getUsername().substring(0, 2).toUpperCase() : u.getUsername().toUpperCase());
        dto.setExacto(desglose.getPuntosPorMarcadorExacto());
        dto.setGanador(desglose.getPuntosPorGanador());
        dto.setInvertido(desglose.getPuntosPorMarcadorInvertido());
        dto.setClasificados(desglose.getPuntosPorClasificados());
        dto.setCampeon(desglose.getPuntosPorCampeon());
        dto.setGoleador(desglose.getPuntosPorGoleador());
        dto.setPalo(desglose.getPuntosPorPalo());
        dto.setTotal(desglose.getTotalGeneral());
        return dto;
    }

    @GetMapping("/ranking")
    public List<UsuarioRankingDTO> obtenerRankingSimple() {
        List<Usuario> usuarios = usuarioRepository.findByRolNombreOrderByPuntosTotalesDesc(AppConstants.ROLE_USER);

        // Aplicar el super comparador también aquí
        usuarios.sort(comparadorRanking);

        return usuarios.stream()
                .map(u -> new UsuarioRankingDTO(u.getId(), u.getUsername(), u.getPuntosTotales()))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    static class UsuarioRankingDTO {
        private Long id;
        private String username;
        private Integer puntosTotales;
    }
}