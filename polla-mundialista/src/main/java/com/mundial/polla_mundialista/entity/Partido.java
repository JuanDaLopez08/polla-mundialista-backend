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

    @ManyToOne
    @JoinColumn(name = "fase_id", nullable = false)
    private Fase fase;

    @ManyToOne
    @JoinColumn(name = "equipo_local_id")
    private Equipo equipoLocal;

    @ManyToOne
    @JoinColumn(name = "equipo_visitante_id")
    private Equipo equipoVisitante;

    @Column(nullable = false)
    private LocalDateTime fechaPartido;

    @ManyToOne
    @JoinColumn(name = "estadio_id")
    private Estadio estadio;

    @Column(name = "numero_partido", unique = true)
    private Integer numeroPartido;

    // Resultados Tiempo Regular + Extra
    private Integer golesLocalReal;
    private Integer golesVisitanteReal;

    // ==========================================
    // NUEVOS CAMPOS: PENALES
    // ==========================================
    private Integer golesPenalesLocal;
    private Integer golesPenalesVisitante;

    @Column(nullable = false)
    private String estado;

    // Ganador oficial (Calculado por goles o penales)
    @ManyToOne
    @JoinColumn(name = "ganador_id")
    private Equipo ganador;
}