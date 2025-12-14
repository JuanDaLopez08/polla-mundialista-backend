package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "partidos")
@Data
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RELACIÓN: Muchos partidos pertenecen a UNA fase
    @ManyToOne
    @JoinColumn(name = "fase_id", nullable = false)
    private Fase fase;

    // Equipos pueden ser NULL al principio (ej: Octavos de final antes de definirse)
    @ManyToOne
    @JoinColumn(name = "equipo_local_id")
    private Equipo equipoLocal;

    @ManyToOne
    @JoinColumn(name = "equipo_visitante_id")
    private Equipo equipoVisitante;

    @Column(nullable = false)
    private LocalDateTime fechaPartido;

    // RELACIÓN: El partido se juega en un Estadio específico (Tabla Estadios)
    @ManyToOne
    @JoinColumn(name = "estadio_id")
    private Estadio estadio;

    // Número oficial del partido según FIFA (1 al 104)
    @Column(name = "numero_partido", unique = true)
    private Integer numeroPartido;

    // Resultados reales
    private Integer golesLocalReal;
    private Integer golesVisitanteReal;

    @Column(nullable = false)
    private String estado; // "PROGRAMADO", "EN_JUEGO", "FINALIZADO"
}