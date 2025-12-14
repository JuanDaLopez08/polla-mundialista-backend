package com.mundial.polla_mundialista.service;

import com.mundial.polla_mundialista.entity.Partido;
import com.mundial.polla_mundialista.entity.Prediccion;
import com.mundial.polla_mundialista.repository.ConfiguracionRepository;
import com.mundial.polla_mundialista.util.AppConstants;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PuntosService {

    private final ConfiguracionRepository configRepo;

    public PuntosService(ConfiguracionRepository configRepo) {
        this.configRepo = configRepo;
    }

    /**
     * Método 1: Calcular puntos de un partido individual.
     * Este es el que busca TorneoService.
     */
    public int calcularPuntosPartido(Prediccion prediccion, Partido partidoReal) {
        // Validación básica
        if (partidoReal.getGolesLocalReal() == null || partidoReal.getGolesVisitanteReal() == null) {
            return 0;
        }

        int ptsExacto = obtenerPuntosPorTipo(AppConstants.CONF_PUNTOS_EXACTO);
        int ptsGanador = obtenerPuntosPorTipo(AppConstants.CONF_PUNTOS_GANADOR);
        int ptsInvertido = obtenerPuntosPorTipo(AppConstants.CONF_PUNTOS_MARCADOR_INVERTIDO);

        int pronosticoLocal = prediccion.getGolesLocalPredicho();
        int pronosticoVisitante = prediccion.getGolesVisitantePredicho();
        int realLocal = partidoReal.getGolesLocalReal();
        int realVisitante = partidoReal.getGolesVisitanteReal();

        // Lógica de Puntos
        if (pronosticoLocal == realLocal && pronosticoVisitante == realVisitante) {
            return ptsExacto;
        }

        String signoPrediccion = obtenerSigno(pronosticoLocal, pronosticoVisitante);
        String signoReal = obtenerSigno(realLocal, realVisitante);

        if (signoPrediccion.equals(signoReal)) {
            return ptsGanador;
        }

        if (pronosticoLocal == realVisitante && pronosticoVisitante == realLocal) {
            return ptsInvertido;
        }

        return 0;
    }

    /**
     * Método 2: Obtener puntos de configuración para premios especiales.
     * Este lo usan TorneoService y AdminController.
     */
    public int obtenerPuntosPorTipo(String tipoConfig) {
        return configRepo.findByClave(tipoConfig)
                .map(c -> Integer.parseInt(c.getValor()))
                .orElse(0); // Retorna 0 si no encuentra la config, para no romper el flujo
    }

    // Privado
    private String obtenerSigno(int local, int visitante) {
        if (local > visitante) return "L";
        if (visitante > local) return "V";
        return "E";
    }
}