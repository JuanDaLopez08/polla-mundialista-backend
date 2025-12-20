package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.PrediccionClasificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrediccionClasificadoRepository extends JpaRepository<PrediccionClasificado, Long> {

    // 1. Traer todos los clasificados de un usuario (para la tabla general)
    List<PrediccionClasificado> findByUsuarioId(Long usuarioId);

    // 2. ✅ LA CORRECCIÓN: Filtrar por Usuario y por Grupo
    // Spring entiende: Busca en Prediccion -> campo 'usuario' (id) Y campo 'equipo' -> campo 'grupo' -> (id)
    List<PrediccionClasificado> findByUsuarioIdAndEquipoGrupoId(Long usuarioId, Long grupoId);

    // 3. Validar si ya existe (evitar duplicados de equipo/usuario)
    Optional<PrediccionClasificado> findByUsuarioIdAndEquipoId(Long usuarioId, Long equipoId);

    // 4. Contar cuántos equipos de un grupo específico ha seleccionado el usuario
    long countByUsuarioIdAndEquipoGrupoId(Long usuarioId, Long grupoId);
}