package com.jarus.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;

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

    @Bean
    public org.springframework.session.SessionRepositoryCustomizer<JdbcIndexedSessionRepository>
    upsertSessionAttributes() {
        return repository -> repository.setCreateSessionAttributeQuery(
            "INSERT INTO SPRING_SESSION_ATTRIBUTES " +
            "(SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME) " +
            "DO UPDATE SET ATTRIBUTE_BYTES = EXCLUDED.ATTRIBUTE_BYTES"
        );
    }
}
