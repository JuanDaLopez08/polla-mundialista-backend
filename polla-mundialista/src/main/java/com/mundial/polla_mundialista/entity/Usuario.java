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

    // ==========================================
    // 🏆 CAMPOS PARA CRITERIOS DE DESEMPATE
    // ==========================================

    @Column(name = "cantidad_aciertos_exactos")
    private Integer cantidadAciertosExactos = 0;

    @Column(name = "cantidad_aciertos_ganador")
    private Integer cantidadAciertosGanador = 0;

    @Column(name = "acerto_campeon")
    private Boolean acertoCampeon = false;

    @Column(name = "ultima_fecha_prediccion")
    private LocalDateTime ultimaFechaPrediccion;

    // ==========================================

    // --- RELACIÓN CON ROL ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    // --- SEGURIDAD: RECUPERACIÓN DE CONTRASEÑA ---
    @Column(name = "token_recuperacion")
    private String tokenRecuperacion;

    @Column(name = "fecha_expiracion_token")
    private LocalDateTime fechaExpiracionToken;
}