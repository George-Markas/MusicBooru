package com.example.musicbooru.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabaseViewInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseViewInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createViews() {
        try {
            var result = entityManager.createNativeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '_user'").getSingleResult();
            if (((Number) result).intValue() == 0) {
                logger.warn("Base table _user does not exist, skipping view creation");
                return;
            }

            String dropViewSql = "DROP VIEW IF EXISTS user_auth_view CASCADE";
            entityManager.createNativeQuery(dropViewSql).executeUpdate();

            String createViewSql = """
                CREATE VIEW user_auth_view AS
                SELECT
                    id,
                    username,
                    password,
                    role
                FROM _user
                """;

            entityManager.createNativeQuery(createViewSql).executeUpdate();
            logger.info("User auth view created successfully");
        } catch (Exception e) {
            // Don't fail the application if view creation fails
            logger.error("Error creating user auth view: {}", e.getMessage(), e);
        }
    }
}