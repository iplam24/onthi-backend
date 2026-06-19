package com.onthi.v_edu.config;

import org.springframework.context.ApplicationEvent;

public class MongoUriChangedEvent extends ApplicationEvent {
    private final String newUri;

    public MongoUriChangedEvent(Object source, String newUri) {
        super(source);
        this.newUri = newUri;
    }

    public String getNewUri() {
        return newUri;
    }
}
