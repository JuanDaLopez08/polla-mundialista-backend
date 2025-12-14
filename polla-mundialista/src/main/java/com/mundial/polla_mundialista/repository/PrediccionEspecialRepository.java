package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.PrediccionEspecial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrediccionEspecialRepository extends JpaRepository<PrediccionEspecial, Long> {

    // Buscar la predicción especial de un usuario específico
    // Usamos Optional porque puede que el usuario aún no haya elegido nada
    Optional<PrediccionEspecial> findByUsuarioId(Long usuarioId);
}