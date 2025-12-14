package com.mundial.polla_mundialista.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "roles")
@Data
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El nombre del rol, por convención de Spring Security se usa: "ROLE_ADMIN", "ROLE_USER"
    @Column(nullable = false, unique = true)
    private String nombre;
}