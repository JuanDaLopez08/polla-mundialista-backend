package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.PrediccionClasificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrediccionClasificadoRepository extends JpaRepository<PrediccionClasificado, Long> {

    // Buscar clasificados de un usuario por grupo (para mostrarlos en pantalla)
    List<PrediccionClasificado> findByUsuarioIdAndGrupoId(Long usuarioId, Long grupoId);

    // Buscar todos los clasificados de un usuario (para calcular puntos al final)
    List<PrediccionClasificado> findByUsuarioId(Long usuarioId);

    // Validación: Saber si ya eligió a este equipo
    boolean existsByUsuarioIdAndEquipoId(Long usuarioId, Long equipoId);
}