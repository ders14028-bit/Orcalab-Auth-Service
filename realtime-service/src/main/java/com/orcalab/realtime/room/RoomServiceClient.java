package com.orcalab.realtime.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;

/**
 * Verifica el rol real de un usuario en una sala consultando directamente a room-service (la
 * base de datos de verdad), en vez de confiar en el caché en memoria de SalaEstadoService — que
 * se llena de forma asíncrona vía Redis Streams y puede tardar hasta 1s en reflejar cambios
 * recientes de membresía/rol. Para verificaciones de permisos (crear/eliminar canal) esa demora
 * es inaceptable, así que se consulta la misma fuente REST que ya usa el front.
 */
@Component
public class RoomServiceClient {

    private final RestClient restClient;

    public RoomServiceClient(RestClient.Builder builder,
                              @Value("${app.services.room-service-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean esLider(Long salaId, Long usuarioId, String token) {
        try {
            MiembroDto[] miembros = restClient.get()
                    .uri("/api/salas/{salaId}/miembros", salaId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(MiembroDto[].class);

            if (miembros == null) return false;

            return Arrays.stream(miembros)
                    .anyMatch(m -> usuarioId.equals(m.usuarioId()) && "LIDER".equals(m.rolEnSala()));
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo verificar el rol del usuario en room-service", e);
        }
    }

    /**
     * Verifica membresia contra room-service (fuente de verdad), no contra el cache en memoria
     * de SalaEstadoService — igual que esLider(), para que un usuario expulsado recientemente
     * (o cuya sala fue eliminada) pierda acceso de inmediato aunque su JWT siga siendo valido.
     * GET /api/salas/{salaId} ya hace ese chequeo internamente (verificarEsMiembro) usando el
     * usuarioId resuelto del propio token reenviado, asi que reusarlo evita duplicar la logica.
     */
    public boolean esMiembro(Long salaId, String token) {
        try {
            restClient.get()
                    .uri("/api/salas/{salaId}", salaId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.BadRequest e) {
            // Forbidden = no es miembro; BadRequest = la sala no existe en room-service.
            // Ambos casos, para quien pregunta, equivalen a "no tienes acceso a esta sala".
            return false;
        } catch (RestClientException e) {
            throw new IllegalStateException("No se pudo verificar la membresía en room-service", e);
        }
    }

    public record MiembroDto(Long usuarioId, String rolEnSala) {}
}
