package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Partido;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {

    // Método existente (Buscar por ID de fase)
    List<Partido> findByFaseIdOrderByFechaPartidoAsc(Long faseId);

    // NUEVO: Buscar por el NOMBRE de la fase
    // Spring Data navega: Partido -> Fase -> Nombre
    // 'IgnoreCase' permite buscar "final", "Final" o "FINAL" sin distinción.
    List<Partido> findByFaseNombreIgnoreCaseOrderByFechaPartidoAsc(String nombreFase);

    // Para listar todos ordenados (ya lo usabas en el findAll con Sort, pero este queda explícito)
    List<Partido> findAllByOrderByFechaPartidoAsc();
}