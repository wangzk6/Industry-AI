package com.kelvin.industry.enterprise.session.repository;

import com.kelvin.industry.enterprise.session.model.AgentSession;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryAgentSessionRepository implements AgentSessionRepository {

    private final ConcurrentMap<String, AgentSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<AgentSession> findById(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public AgentSession save(AgentSession session) {
        sessions.put(session.getId(), session);
        return session;
    }

    @Override
    public void deleteById(String id) {
        sessions.remove(id);
    }

    @Override
    public List<AgentSession> findAll() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public long count() {
        return sessions.size();
    }
}
