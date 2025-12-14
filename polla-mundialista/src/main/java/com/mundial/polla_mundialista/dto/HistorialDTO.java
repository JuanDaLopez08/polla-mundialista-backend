package com.mundial.polla_mundialista.dto;

import lombok.Data;
import java.util.List;

@Data
public class HistorialDTO {
    // Cabecera del Jugador (Lo que se ve antes de desplegar la lista)
    private String username;
    private Integer puntosTotales;

    // Lista de sus apuestas (Detalle)
    private List<PrediccionResumenDTO> predicciones;

    // Clase interna estática para definir cómo se ve cada fila del historial
    // Usamos esto para no crear otro archivo separado innecesariamente
    @Data
    public static class PrediccionResumenDTO {
        private Long partidoId;
        private String equipoLocal;
        private String equipoVisitante;
        private String urlEscudoLocal;
        private String urlEscudoVisitante;
        private Integer golesLocalPredicho;
        private Integer golesVisitantePredicho;
        private Integer puntosGanados;
        private String estadoPartido; // "FINALIZADO", "PROGRAMADO"
    }
}