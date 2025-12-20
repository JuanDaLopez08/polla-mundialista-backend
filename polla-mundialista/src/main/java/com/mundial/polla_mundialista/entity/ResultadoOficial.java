package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "resultados_oficiales")
@Data
public class ResultadoOficial {

    // Usaremos un ID fijo (1) porque solo hay un resultado final por mundial
    @Id
    private Long id = 1L;

    @OneToOne
    @JoinColumn(name = "equipo_campeon_id")
    private Equipo equipoCampeon;

    @OneToOne
    @JoinColumn(name = "jugador_goleador_id")
    private Jugador jugadorGoleador;
}