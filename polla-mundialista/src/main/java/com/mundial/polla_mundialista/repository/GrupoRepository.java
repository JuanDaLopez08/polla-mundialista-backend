package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    // Para buscar si el Grupo A ya existe y no duplicarlo
    Optional<Grupo> findByNombre(String nombre);
}