package com.mundial.polla_mundialista.controller;

import com.mundial.polla_mundialista.entity.Usuario;
import com.mundial.polla_mundialista.repository.UsuarioRepository;
import com.mundial.polla_mundialista.service.PuntosService;
import com.mundial.polla_mundialista.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mundial.polla_mundialista.dto.DesglosePuntosDTO;

import java.util.List;

@RestController
@RequestMapping("/api/puntos")
@CrossOrigin(origins = "*") // Permitir acceso desde React
@RequiredArgsConstructor
public class PuntosController {

    private final PuntosService puntosService;
    private final UsuarioRepository usuarioRepo;

    /**
     * Recalcula los puntos de un usuario específico.
     * Útil cuando un usuario reclama que sus puntos no se ven reflejados.
     */
    @PostMapping("/recalcular/{usuarioId}")
    public ResponseEntity<String> recalcularUsuario(@PathVariable Long usuarioId) {
        try {
            puntosService.recalcularPuntosUsuario(usuarioId);
            return ResponseEntity.ok("✅ Puntos recalculados correctamente para el usuario ID: " + usuarioId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error recalculando puntos: " + e.getMessage());
        }
    }

    /**
     * Recalcula los puntos DE TODOS los usuarios 'ROLE_USER'.
     * Útil para ejecutar al final de una fase o tras corregir un marcador erróneo.
     * ADVERTENCIA: Puede tardar si hay muchos usuarios.
     */
    @PostMapping("/recalcular-todos")
    public ResponseEntity<String> recalcularTodos() {
        // Buscamos solo los usuarios normales (no admins)
        List<Usuario> usuarios = usuarioRepo.findByRolNombreOrderByPuntosTotalesDesc(AppConstants.ROLE_USER);

        int cont = 0;
        for (Usuario u : usuarios) {
            puntosService.recalcularPuntosUsuario(u.getId());
            cont++;
        }

        return ResponseEntity.ok("✅ Proceso masivo finalizado. Usuarios recalculados: " + cont);
    }

    /**
     * Obtiene el desglose detallado de puntos de un usuario.
     * Ideal para mostrar en el perfil del usuario: "Mira de dónde vienen tus puntos".
     */
    @GetMapping("/desglose/{usuarioId}")
    public ResponseEntity<DesglosePuntosDTO> obtenerDesglose(@PathVariable Long usuarioId) {
        DesglosePuntosDTO desglose = puntosService.obtenerDesgloseUsuario(usuarioId);
        return ResponseEntity.ok(desglose);
    }
}