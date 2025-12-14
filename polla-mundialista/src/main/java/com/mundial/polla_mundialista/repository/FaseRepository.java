package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Fase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaseRepository extends JpaRepository<Fase, Long> {
    // Para buscar si ya existe la "Fase de Grupos"
    Optional<Fase> findByNombre(String nombre);
}