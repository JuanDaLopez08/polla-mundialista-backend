package com.mundial.polla_mundialista.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PosicionDTO implements Comparable<PosicionDTO> {
    private Long equipoId;
    private String nombreEquipo;
    private String urlEscudo;

    private int puntos;
    private int partidosJugados;
    private int partidosGanados;
    private int partidosEmpatados;
    private int partidosPerdidos;
    private int golesFavor;
    private int golesContra;
    private int diferenciaGoles;

    // Historial de resultados (Ej: ["G", "E", "P"]) para mostrar la racha en la tabla
    private List<String> resultados = new ArrayList<>();

    // Lógica para ordenar automáticamente la tabla (Criterios FIFA)
    // 1. Puntos
    // 2. Diferencia de Gol
    // 3. Goles a Favor
    @Override
    public int compareTo(PosicionDTO otro) {
        if (this.puntos != otro.puntos) {
            return otro.puntos - this.puntos; // Mayor puntaje primero
        }
        if (this.diferenciaGoles != otro.diferenciaGoles) {
            return otro.diferenciaGoles - this.diferenciaGoles; // Mayor diferencia primero
        }
        return otro.golesFavor - this.golesFavor; // Más goles primero
    }
}
