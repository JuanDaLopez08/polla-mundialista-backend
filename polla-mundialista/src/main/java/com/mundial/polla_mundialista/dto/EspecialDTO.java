package com.mundial.polla_mundialista.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EspecialDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "Debes seleccionar un equipo campeón")
    private Long equipoCampeonId;

    @NotNull(message = "Debes seleccionar un jugador goleador")
    private Long jugadorGoleadorId;
}