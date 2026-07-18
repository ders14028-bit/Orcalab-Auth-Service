package com.orcalab.realtime.chat;

import com.orcalab.realtime.canal.CanalRepository;
import com.orcalab.realtime.config.AuthContext;
import com.orcalab.realtime.room.RoomServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatHistorialController {

    private final MensajeRepository mensajeRepository;
    private final CanalRepository canalRepository;
    private final RoomServiceClient roomServiceClient;
    private final AuthContext authContext;

    public ChatHistorialController(MensajeRepository mensajeRepository, CanalRepository canalRepository,
                                    RoomServiceClient roomServiceClient, AuthContext authContext) {
        this.mensajeRepository = mensajeRepository;
        this.canalRepository = canalRepository;
        this.roomServiceClient = roomServiceClient;
        this.authContext = authContext;
    }

    @GetMapping("/api/salas/{salaId}/canales/{canalId}/mensajes")
    public ResponseEntity<List<MensajeResponse>> historial(@PathVariable Long salaId, @PathVariable String canalId) {
        // 404 (no 403) si el canal no existe o pertenece a otra sala, para no confirmarle a un
        // no-miembro que el canalId que probó sí existe en alguna sala ajena.
        boolean canalPerteneceASala = canalRepository.findById(canalId)
                .map(c -> c.getSalaId().equals(salaId))
                .orElse(false);
        if (!canalPerteneceASala) {
            return ResponseEntity.notFound().build();
        }

        if (!roomServiceClient.esMiembro(salaId, authContext.tokenActual())) {
            throw new AccessDeniedException("No eres miembro de esta sala");
        }

        List<MensajeResponse> mensajes = mensajeRepository.findByCanalIdOrderByTimestampAsc(canalId).stream()
                .map(MensajeResponse::new)
                .toList();
        return ResponseEntity.ok(mensajes);
    }
}