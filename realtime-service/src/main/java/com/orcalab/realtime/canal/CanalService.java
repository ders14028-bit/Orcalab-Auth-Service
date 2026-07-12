package com.orcalab.realtime.canal;

import com.orcalab.realtime.config.AuthContext;
import com.orcalab.realtime.state.SalaEstadoService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CanalService {

    public static final String NOMBRE_CANAL_POR_DEFECTO = "general";

    private final CanalRepository canalRepository;
    private final SalaEstadoService salaEstadoService;
    private final AuthContext authContext;
    private final SimpMessagingTemplate messagingTemplate;

    public CanalService(CanalRepository canalRepository, SalaEstadoService salaEstadoService,
                         AuthContext authContext, SimpMessagingTemplate messagingTemplate) {
        this.canalRepository = canalRepository;
        this.salaEstadoService = salaEstadoService;
        this.authContext = authContext;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Canal> listar(Long salaId) {
        return canalRepository.findBySalaIdOrderByFechaCreacionAsc(salaId);
    }

    public Canal crear(Long salaId, CanalRequest request) {
        Long usuarioId = authContext.usuarioIdActual();

        if (!salaEstadoService.esLider(salaId, usuarioId)) {
            throw new AccessDeniedException("Solo el líder de la sala puede crear canales");
        }

        Canal canal = new Canal(salaId, request.getNombre().trim(), request.getTipo(), usuarioId);
        canal = canalRepository.save(canal);

        messagingTemplate.convertAndSend("/topic/sala/" + salaId + "/canales", canal);

        return canal;
    }

    /** Se invoca cuando se crea una sala, para que el chat tenga un canal de texto disponible desde el inicio. */
    public void crearCanalPorDefectoSiNoExiste(Long salaId, Long creadorId) {
        if (canalRepository.existsBySalaId(salaId)) {
            return;
        }
        Canal canal = new Canal(salaId, NOMBRE_CANAL_POR_DEFECTO, TipoCanal.TEXTO, creadorId);
        canalRepository.save(canal);
    }
}
