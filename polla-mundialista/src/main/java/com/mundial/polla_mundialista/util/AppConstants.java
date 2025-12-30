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
    public static final String ERR_EQUIPO_LOCAL_NO_ENCONTRADO = "Equipo Local no encontrado";
    public static final String ERR_EQUIPO_VISITANTE_NO_ENCONTRADO = "Equipo Visitante no encontrado";
    public static final String ERR_EQUIPO_PALO_NO_ENCONTRADO = "No hay equipos candidatos a palo";



    //URL Repechaje estandar
    public static final String URL_REPECHAJE = "https://upload.wikimedia.org/wikipedia/commons/1/10/Flag_of_FIFA.svg";

    // Nombres de Fases
    public static final String FASE_GRUPOS = "Fase de Grupos";
    public static final String FASE_DIECISEISAVOS = "Dieciseisavos de Final";
    public static final String FASE_OCTAVOS = "Octavos de Final";
    public static final String FASE_CUARTOS = "Cuartos de Final";
    public static final String FASE_SEMIFINALES = "Semifinales";
    public static final String FASE_FINAL = "Final";
    public static final String FASE_TERCER_PUESTO = "Tercer Puesto";

    //Grupos
    public static final String GRUPO_A= "A";
    public static final String GRUPO_B= "B";
    public static final String GRUPO_C= "C";
    public static final String GRUPO_D= "D";
    public static final String GRUPO_E= "E";
    public static final String GRUPO_F= "F";
    public static final String GRUPO_G= "G";
    public static final String GRUPO_H= "H";
    public static final String GRUPO_I= "I";
    public static final String GRUPO_J= "J";
    public static final String GRUPO_K= "K";
    public static final String GRUPO_L= "L";

    //Racha Partido
    public static final String PARTIDO_GANADO= "G";
    public static final String PARTIDO_EMPATADO= "E";
    public static final String PARTIDO_PERDIDO= "P";


    // Constructor privado para evitar instanciación
    private AppConstants() {
    }
}