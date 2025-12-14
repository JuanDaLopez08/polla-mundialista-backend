package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    // Para verificar si el jugador ya existe antes de crearlo
    Optional<Jugador> findByNombre(String nombre);
}