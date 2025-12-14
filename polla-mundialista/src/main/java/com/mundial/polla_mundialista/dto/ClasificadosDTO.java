package com.mundial.polla_mundialista.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ClasificadosDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "El ID del grupo es obligatorio")
    private Long grupoId;

    // Lista de IDs de los equipos que el usuario dice que clasifican
    // Ej: [1, 3] (México y Corea)
    @NotNull(message = "Debes seleccionar equipos")
    @Size(min = 1, message = "Debes seleccionar al menos un equipo")
    private List<Long> equiposIds;
}