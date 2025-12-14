package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Usuario;
import com.mundial.polla_mundialista.repository.UsuarioRepository;
import com.mundial.polla_mundialista.util.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Endpoint para el Ranking de participantes
    // URL: GET http://localhost:8080/api/usuarios/ranking
    @GetMapping("/ranking")
    public List<UsuarioRankingDTO> obtenerRanking() {
        // 1. Buscamos los usuarios en la BD
        List<Usuario> usuarios = usuarioRepository.findByRolNombreOrderByPuntosTotalesDesc(AppConstants.ROLE_USER);

        // 2. Mapeamos (convertimos) la lista de Entidades a DTOs para limpiar la respuesta
        return usuarios.stream()
                .map(u -> new UsuarioRankingDTO(
                        u.getId(),
                        u.getUsername(), // Solo mostramos nombre de usuario
                        u.getPuntosTotales() // Y sus puntos
                ))
                .collect(Collectors.toList());
    }

    // ==========================================
    // DTO INTERNO (Para proteger datos sensibles)
    // ==========================================
    @Data
    @AllArgsConstructor
    static class UsuarioRankingDTO {
        private Long id;
        private String username;
        private Integer puntosTotales;
    }
}