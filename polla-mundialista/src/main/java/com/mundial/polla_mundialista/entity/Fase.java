package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "fases")
@Data
public class Fase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre; // Ej: "Fase de Grupos", "Octavos de Final"

    @Column(nullable = false)
    private String estado; // "ABIERTA", "CERRADA", "EN_JUEGO"

    // Esta es la fecha clave para la "Regla de Oro"
    // Si la fecha actual > fechaLimite, ya no se pueden editar predicciones de esta fase.
    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;
}