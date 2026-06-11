package com.jarus.ai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

/**
 * Fixes DuplicateKeyException in Spring Session JDBC on PostgreSQL.
 *
 * When two concurrent requests arrive for the same new session, both try to
 * INSERT the SPRING_SECURITY_CONTEXT attribute and one fails with a unique
 * constraint violation.  Replacing the INSERT with an UPSERT
 * (ON CONFLICT DO UPDATE) makes it idempotent.
 */
@Configuration
public class SessionConfig {

    @Autowired
    private JdbcIndexedSessionRepository sessionRepository;

    @PostConstruct
    public void configureUpsert() {
        sessionRepository.setCreateSessionAttributeQuery(
            "INSERT INTO SPRING_SESSION_ATTRIBUTES " +
            "(SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME) " +
            "DO UPDATE SET ATTRIBUTE_BYTES = EXCLUDED.ATTRIBUTE_BYTES"
        );
    }
}
