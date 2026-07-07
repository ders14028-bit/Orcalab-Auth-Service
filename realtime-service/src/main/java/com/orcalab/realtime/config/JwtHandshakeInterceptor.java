package com.orcalab.realtime.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtHandshakeInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtil.esTokenValido(token)) {
                    Long usuarioId = jwtUtil.extraerUsuarioId(token);
                    String rol = jwtUtil.extraerRol(token);

                    var authToken = new UsernamePasswordAuthenticationToken(
                            usuarioId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                    );

                    accessor.setUser(authToken);
                } else {
                    throw new IllegalArgumentException("Token JWT inválido o expirado");
                }
            } else {
                throw new IllegalArgumentException("Falta el header Authorization en la conexión STOMP");
            }
        }

        return message;
    }
}