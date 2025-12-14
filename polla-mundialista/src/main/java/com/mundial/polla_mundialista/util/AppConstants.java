package com.mundial.polla_mundialista.util;

public class AppConstants {

    // Roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    // Claves de Configuración (Base de Datos)
    public static final String CONF_PUNTOS_EXACTO = "PUNTOS_EXACTO";
    public static final String CONF_PUNTOS_GANADOR = "PUNTOS_GANADOR";
    public static final String CONF_PUNTOS_CAMPEON = "PUNTOS_CAMPEON";
    public static final String CONF_PUNTOS_GOLEADOR = "PUNTOS_GOLEADOR";
    public static final String CONF_PUNTOS_PALO = "PUNTOS_PALO";
    public static final String CONF_PUNTOS_CLASIFICADO = "PUNTOS_CLASIFICADO";
    public static final String CONF_PUNTOS_MARCADOR_INVERTIDO = "PUNTOS_MARCADOR_INVERTIDO";

    // Estados de Partido
    public static final String ESTADO_PROGRAMADO = "PROGRAMADO";
    public static final String ESTADO_EN_JUEGO = "EN_JUEGO";
    public static final String ESTADO_FINALIZADO = "FINALIZADO";

    // Estados de Fase
    public static final String FASE_ABIERTA = "ABIERTA";
    public static final String FASE_CERRADA = "CERRADA";

    // Mensajes de Error Comunes
    public static final String ERR_USUARIO_NO_ENCONTRADO = "Usuario no encontrado";
    public static final String ERR_PARTIDO_NO_ENCONTRADO = "Partido no encontrado";
    public static final String ERR_JUGADOR_NO_ENCONTRADO = "Jugador no encontrado";
    public static final String ERR_EQUIPO_NO_ENCONTRADO = "Equipo no encontrado";
    public static final String ERR_TIEMPO_EXPIRADO = "El tiempo para realizar esta acción ha expirado.";

    //URL Repechaje estandar
    public static final String URL_REPECHAJE = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Question_mark_%28red%29.svg/640px-Question_mark_%28red%29.svg.png";

    // Constructor privado para evitar instanciación
    private AppConstants() {
    }
}