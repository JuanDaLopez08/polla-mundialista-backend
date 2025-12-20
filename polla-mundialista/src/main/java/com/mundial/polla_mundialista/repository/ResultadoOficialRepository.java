package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.ResultadoOficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoOficialRepository extends JpaRepository<ResultadoOficial, Long> {
    // No necesitamos métodos extra, con findById(1L) basta.
}