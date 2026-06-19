package com.onthi.v_edu.config;

import com.mongodb.client.MongoDatabase;
import com.onthi.v_edu.common.setting.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
@Primary
@Slf4j
public class DynamicMongoDatabaseFactory implements MongoDatabaseFactory, ApplicationListener<MongoUriChangedEvent> {

    private final SystemSettingService systemSettingService;
    private volatile SimpleMongoClientDatabaseFactory delegate;
    private String currentUri;

    public DynamicMongoDatabaseFactory(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
        initializeDelegate();
    }

    public synchronized void initializeDelegate() {
        String uri = systemSettingService.getSettingValue("MONGODB_URI", "mongodb://localhost:27017/onthi");
        if (uri == null || uri.isBlank()) {
            uri = "mongodb://localhost:27017/onthi";
        }

        if (delegate != null && uri.equals(currentUri)) {
            return;
        }

        log.info("[DYNAMIC MONGO] Initializing MongoDB connection with URI: {}", maskUri(uri));
        try {
            SimpleMongoClientDatabaseFactory oldDelegate = this.delegate;
            SimpleMongoClientDatabaseFactory newDelegate = new SimpleMongoClientDatabaseFactory(uri);
            
            // Warm up/validate connection: try to get database metadata if possible,
            // or just swap the reference immediately.
            this.delegate = newDelegate;
            this.currentUri = uri;

            if (oldDelegate != null) {
                log.info("[DYNAMIC MONGO] Closing old MongoDB connection pool...");
                try {
                    oldDelegate.destroy();
                } catch (Exception e) {
                    log.error("[DYNAMIC MONGO] Error closing old Mongo client: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[DYNAMIC MONGO] Failed to initialize MongoDB connection with URI: {}. Error: {}", maskUri(uri), e.getMessage());
            if (this.delegate == null) {
                // Fallback to localhost if no connection has been initialized yet
                this.delegate = new SimpleMongoClientDatabaseFactory("mongodb://localhost:27017/onthi");
                this.currentUri = "mongodb://localhost:27017/onthi";
            }
        }
    }

    private String maskUri(String uri) {
        if (uri == null) return "";
        return uri.replaceAll("mongodb(\\+srv)?://([^:]+):([^@]+)@", "mongodb$1://$2:******@");
    }

    @Override
    public MongoDatabase getMongoDatabase() throws DataAccessException {
        return delegate.getMongoDatabase();
    }

    @Override
    public MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
        return delegate.getMongoDatabase(dbName);
    }

    @Override
    public org.springframework.dao.support.PersistenceExceptionTranslator getExceptionTranslator() {
        return delegate.getExceptionTranslator();
    }

    @Override
    public com.mongodb.client.ClientSession getSession(com.mongodb.ClientSessionOptions options) throws DataAccessException {
        return delegate.getSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(com.mongodb.client.ClientSession session) {
        return delegate.withSession(session);
    }

    @Override
    public void onApplicationEvent(MongoUriChangedEvent event) {
        log.info("[DYNAMIC MONGO] Received MongoUriChangedEvent. Re-initializing MongoDB...");
        initializeDelegate();
    }
}
