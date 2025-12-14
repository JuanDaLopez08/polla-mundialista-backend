package com.mundial.polla_mundialista.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de Errores de Validación (@Valid)
    // Ej: "Goles negativos", "Falta ID usuario"
    // Devuelve un mapa con el campo y el error: { "golesLocal": "no puede ser negativo" }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        return new ResponseEntity<>(errores, HttpStatus.BAD_REQUEST); // 400
    }

    // 2. Manejo de Errores de Lógica (RuntimeException)
    // Ej: "Tiempo expirado", "Usuario no encontrado", "Partido ya empezó"
    // Devuelve: { "error": "Mensaje específico del problema" }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> manejarLogica(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // 400
    }

    // 3. Manejo de Errores Generales (Cualquier otra cosa inesperada)
    // Ej: NullPointer, Fallo de Base de Datos inesperado
    // Devuelve un 500 pero con un JSON controlado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarTodo(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Ocurrió un error interno en el servidor");
        // En producción, podrías quitar el detalle para no dar pistas a hackers
        error.put("detalle", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }
}