package com.kelvin.industry.session.service;

import com.kelvin.industry.session.bean.Session;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionManager {

    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();


    //根据sessionId查询session
    public Session getById(String id){
        return sessionMap.computeIfAbsent(id, sessionId -> {
            Session session = new Session();
            session.setId(UUID.randomUUID().toString());
            session.setMessages(new ArrayDeque<>());
            session.setCreatedAt(LocalDateTime.now());
            return session;
        });
    }

    //保存session
    public String saveSession(Session session){
        sessionMap.put(session.getId(), session);
        return session.getId();
    }

    //清空session
    public boolean clearSessions (String sessionId){
        Session session = sessionMap.get(sessionId);
        if (null != session){
            session = null;
        }
        sessionMap.put(sessionId, null);
        return true;
    }

}
