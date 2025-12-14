package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones_clasificados", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "equipo_id"}) // Un usuario no puede elegir al mismo equipo dos veces
})
@Data
public class PrediccionClasificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    // Guardamos a qué grupo pertenece el equipo para facilitar consultas
    // (Ej: "Traeme todos los clasificados que Juan eligió del Grupo A")
    @ManyToOne
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    // Campo para saber si acertó (se llenará al final de la fase)
    private Boolean acerto = false;
}