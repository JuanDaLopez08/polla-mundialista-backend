package com.mundial.polla_mundialista.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Importar esto
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "predicciones_clasificados")
public class PrediccionClasificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Evita el error de recursión infinita o proxy al serializar Usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "predicciones", "password", "rol"})
    private Usuario usuario;

    // Evita el error de proxy al serializar Equipo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "jugadores"})
    private Equipo equipo;

    // Si tienes un campo booleano de si acertó o no
    private Boolean acerto;
}