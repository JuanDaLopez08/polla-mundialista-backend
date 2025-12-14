package com.mundial.polla_mundialista.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrediccionDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "El ID del partido es obligatorio")
    private Long partidoId;

    @NotNull(message = "Debes ingresar los goles del local")
    @Min(value = 0, message = "Los goles no pueden ser negativos")
    private Integer golesLocal;

    @NotNull(message = "Debes ingresar los goles del visitante")
    @Min(value = 0, message = "Los goles no pueden ser negativos")
    private Integer golesVisitante;
}