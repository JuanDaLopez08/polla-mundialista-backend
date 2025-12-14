package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estadios")
@Data
public class Estadio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre; // Ej: "Estadio Azteca"

    @Column(nullable = false)
    private String ciudad; // Ej: "Ciudad de México"

    private String pais;   // Ej: "México"
}