package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "grupos")
@Data
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del grupo: "A", "B", "C", "D"...
    @Column(nullable = false, unique = true, length = 1)
    private String nombre;
}