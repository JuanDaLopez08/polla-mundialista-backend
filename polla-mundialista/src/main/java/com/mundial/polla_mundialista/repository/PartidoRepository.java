package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Partido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // <--- IMPORTANTE: No olvides importar esto

@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {

    // ==========================================
    // NUEVO MÉTODO (OBLIGATORIO PARA BRACKET)
    // ==========================================
    // Busca un partido específico por su número oficial (1-104)
    // Retorna Optional para evitar NullPointerException si no existe.
    Optional<Partido> findByNumeroPartido(Integer numeroPartido);

    // ==========================================
    // MÉTODOS EXISTENTES
    // ==========================================

    // Buscar por ID de fase
    List<Partido> findByFaseIdOrderByFechaPartidoAsc(Long faseId);

    // Buscar por NOMBRE de fase
    List<Partido> findByFaseNombreIgnoreCaseOrderByFechaPartidoAsc(String nombreFase);

    // Listar todos ordenados por fecha
    List<Partido> findAllByOrderByFechaPartidoAsc();
}