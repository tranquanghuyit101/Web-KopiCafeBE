package com.kopi.kopi.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseHub {
    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> sessions = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer userId) {
        SseEmitter em = new SseEmitter(0L);
        sessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(em);
        em.onCompletion(() -> sessions.getOrDefault(userId, new CopyOnWriteArrayList<>()).remove(em));
        em.onTimeout(em::complete);
        return em;
    }

    public void push(Integer userId, Object event) {
        var list = sessions.get(userId);
        if (list == null) return;
        for (SseEmitter em : list) {
            try { em.send(SseEmitter.event().name("notification").data(event)); }
            catch (IOException e) { em.complete(); }
        }
    }
}
