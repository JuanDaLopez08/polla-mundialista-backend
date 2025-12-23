package com.mundial.polla_mundialista.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "partido_id"})
})
@Data
public class Prediccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @JsonIgnore evita que se envíe toda la info del usuario al pedir la lista de predicciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    @Column(nullable = false)
    private Integer golesLocalPredicho;

    @Column(nullable = false)
    private Integer golesVisitantePredicho;

    // AUDITORÍA: Solo guarda el momento exacto en que el usuario dio clic a "Guardar".
    // La validación de sí "está a tiempo o no" se hará comparando esta fecha
    // contra la fecha de la FASE del partido.
    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private Integer puntosGanados = 0;
}