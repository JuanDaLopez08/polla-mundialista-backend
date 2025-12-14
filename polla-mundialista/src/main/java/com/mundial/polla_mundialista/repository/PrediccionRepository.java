package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Prediccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrediccionRepository extends JpaRepository<Prediccion, Long> {

    // Para mostrarle al usuario sus apuestas
    List<Prediccion> findByUsuarioId(Long usuarioId);

    // Para calcular puntos masivamente cuando termina un partido
    List<Prediccion> findByPartidoId(Long partidoId);
}