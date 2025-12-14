package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    // Método para buscar un rol por su nombre (Ej: "ROLE_ADMIN")
    // Lo usaremos para no crear roles duplicados
    Optional<Rol> findByNombre(String nombre);
}
