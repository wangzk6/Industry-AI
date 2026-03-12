package com.kelvin.industry.enterprise.session.repository;

import com.kelvin.industry.enterprise.session.model.AgentSession;

import java.util.List;
import java.util.Optional;

public interface AgentSessionRepository {

    Optional<AgentSession> findById(String id);

    AgentSession save(AgentSession session);

    void deleteById(String id);

    List<AgentSession> findAll();

    long count();
}
