package com.mundial.polla_mundialista.repository;

import com.mundial.polla_mundialista.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // ✅ SOLUCIÓN ELEGANTE (Query Method):
    // "findBy" + "Rol" (objeto en Usuario) + "Nombre" (campo en objeto Rol)
    // Spring Data JPA hace el JOIN automáticamente por ti.
    //
    // NOTA: Esto asume que tu entidad 'Rol' tiene un campo llamado 'nombre' (String).
    // Si tu campo en la entidad Rol se llama 'name', cambia el método a: findByRolNameOrderBy...
    List<Usuario> findByRolNombreOrderByPuntosTotalesDesc(String nombreRol);

    // Métodos para la Seguridad (Login)
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);

    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}