package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Estadio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadioRepository extends JpaRepository<Estadio, Long> {
    // Buscamos estadio por nombre para no duplicarlos en el DataSeeder
    Optional<Estadio> findByNombre(String nombre);
}
