package com.tracktrove.service;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WebSocketService is the server endpoint for real-time transaction updates.
 * It manages a set of connected clients and provides a broadcast mechanism.
 * The endpoint path is defined by @ServerEndpoint("/ws").
 */
@Service
@ServerEndpoint("/ws")
public class WebSocketService {

    // A thread-safe Set to store all connected WebSocket sessions.
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    // This method is called when a new WebSocket connection is established.
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("New WebSocket connection established with session ID: " + session.getId());
    }

    // This method is called when a WebSocket connection is closed.
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("WebSocket connection closed for session ID: " + session.getId());
    }

    // This method is called when an error occurs on the WebSocket connection.
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error on session ID: " + session.getId() + ". Error: " + throwable.getMessage());
    }

    /**
     * Broadcasts a message to all connected WebSocket clients.
     * The message is sent as a JSON string.
     *
     * @param message The message to be broadcasted.
     */
    public void broadcast(String message) {
        synchronized (sessions) {
            for (Session session : sessions) {
                try {
                    // Send the message to the client, converting it to a JSON object
                    // to match the front-end's expectation.
                    session.getBasicRemote().sendText("{\"message\": \"" + message + "\"}");
                } catch (IOException e) {
                    System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}