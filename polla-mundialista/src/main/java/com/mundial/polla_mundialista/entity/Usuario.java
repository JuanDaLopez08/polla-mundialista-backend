package com.mundial.polla_mundialista.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore // Seguridad: La contraseña nunca viaja en el JSON
    @Column(nullable = false)
    private String password;

    @Column(name = "puntos_totales")
    private Integer puntosTotales = 0;

    // --- RELACIÓN CON ROL ---
    // Muchos usuarios pueden tener el mismo rol (Many-to-One)
    @ManyToOne(fetch = FetchType.EAGER) // Eager para cargar el rol al iniciar sesión
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    // --- SEGURIDAD: RECUPERACIÓN DE CONTRASEÑA ---
    @Column(name = "token_recuperacion")
    private String tokenRecuperacion;

    @Column(name = "fecha_expiracion_token")
    private LocalDateTime fechaExpiracionToken;
}