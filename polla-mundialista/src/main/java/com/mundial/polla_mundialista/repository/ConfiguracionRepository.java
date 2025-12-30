package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {

    // Spring entiende automáticamente que debe buscar en la columna 'clave'
    Optional<Configuracion> findByClave(String clave);
}