package com.mundial.polla_mundialista.dto;

import lombok.Data;

@Data
public class RankingDetalladoDTO {
    private Long usuarioId;
    private String username;
    private String avatar; // Iniciales (ej: "JU")

    // Desglose detallado (Viene de DesglosePuntosDTO)
    private int exacto;
    private int ganador;
    private int invertido;
    private int clasificados;
    private int campeon;
    private int goleador;
    private int palo;

    private int total; // Puntos totales
}