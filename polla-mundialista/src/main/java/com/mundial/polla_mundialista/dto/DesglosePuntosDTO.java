package com.mundial.polla_mundialista.dto;

import lombok.Data;

@Data
public class DesglosePuntosDTO {
    // Totales acumulados por categoría
    private int puntosPorMarcadorExacto;
    private int puntosPorGanador; // O Empate
    private int puntosPorMarcadorInvertido;
    private int puntosPorClasificados; // Fase de grupos -> 16avos

    // Especiales
    private int puntosPorCampeon;
    private int puntosPorGoleador;
    private int puntosPorPalo; // Gallo Tapado

    // Suma total (Debe coincidir con usuario.puntosTotales)
    private int totalGeneral;
}