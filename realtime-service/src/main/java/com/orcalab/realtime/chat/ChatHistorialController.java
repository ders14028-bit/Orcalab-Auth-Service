package com.orcalab.realtime.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatHistorialController {

    private final MensajeRepository mensajeRepository;

    public ChatHistorialController(MensajeRepository mensajeRepository) {
        this.mensajeRepository = mensajeRepository;
    }

    @GetMapping("/api/salas/{salaId}/mensajes")
    public List<MensajeResponse> historial(@PathVariable Long salaId) {
        return mensajeRepository.findBySalaIdOrderByTimestampAsc(salaId).stream()
                .map(MensajeResponse::new)
                .toList();
    }
}