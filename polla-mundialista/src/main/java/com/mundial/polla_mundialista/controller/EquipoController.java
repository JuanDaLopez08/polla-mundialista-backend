package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Equipo;
import com.mundial.polla_mundialista.repository.EquipoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "*")
public class EquipoController {

    private final EquipoRepository equipoRepository;

    public EquipoController(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    // Endpoint: Listar todos los equipos
    // Uso: Llenar selects de "Campeón", "Palo" o "Clasificados" en el Frontend.
    @GetMapping
    public List<Equipo> obtenerTodosLosEquipos() {
        // MEJORA: Devolvemos la lista ordenada alfabéticamente por nombre
        return equipoRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre"));
    }
}