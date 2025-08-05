package com.tracktrove.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocketConfig is a Spring configuration class that automatically registers
 * all beans annotated with @ServerEndpoint.
 * Without this class, Spring Boot would not recognize the WebSocketService
 * as a server endpoint, causing connection failures.
 */
@Configuration
public class WebSocketConfig {

    /**
     * Creates and exposes a ServerEndpointExporter bean.
     * This bean scans for all classes annotated with @ServerEndpoint
     * and registers them with the underlying WebSocket container.
     *
     * @return A ServerEndpointExporter bean.
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
