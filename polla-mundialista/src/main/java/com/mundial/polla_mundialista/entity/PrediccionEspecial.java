package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "predicciones_especiales")
@Data
public class PrediccionEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un usuario solo tiene UNA hoja de predicciones especiales (Campeón, Goleador, Palo)
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    // El equipo que creen que va a ganar (Relación con tabla Equipos)
    @ManyToOne
    @JoinColumn(name = "equipo_campeon_id")
    private Equipo equipoCampeon;

    // El jugador goleador (Relación con tabla Jugadores)
    // Esto es clave: ahora apunta a un ID de jugador real, no a un texto.
    @ManyToOne
    @JoinColumn(name = "jugador_goleador_id")
    private Jugador jugadorGoleador;

    // El equipo "Palo" (Underdog) que el sistema le asignará después del sorteo
    @ManyToOne
    @JoinColumn(name = "equipo_palo_id")
    private Equipo equipoPalo;
}