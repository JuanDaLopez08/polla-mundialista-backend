package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "configuracion")
@Data
public class Configuracion {

    @Id
    @Column(nullable = false, unique = true)
    private String clave; // Ej: "PUNTOS_EXACTO", "PUNTOS_CAMPEON"

    @Column(nullable = false)
    private String valor; // Ej: "5", "10"
}