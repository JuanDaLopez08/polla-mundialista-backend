package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    // Método vital para el sorteo del Palo (buscar los marcados como true)
    List<Equipo> findByEsCandidatoPaloTrue();

    // Método para evitar duplicados al cargar datos iniciales
    Optional<Equipo> findByNombre(String nombre);
}