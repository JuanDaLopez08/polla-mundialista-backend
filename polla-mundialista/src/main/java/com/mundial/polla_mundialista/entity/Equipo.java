package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "equipos")
@Data
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(name = "codigo_iso", length = 3)
    private String codigoIso;

    @Column(name = "url_escudo")
    private String urlEscudo;

    @Column(name = "es_candidato_palo")
    private Boolean esCandidatoPalo = false;

    // --- CAMBIO IMPORTANTE ---
    // Ya no es un texto, ahora es una relación con la tabla Grupos.
    // Muchos equipos pertenecen a UN grupo.
    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;
}