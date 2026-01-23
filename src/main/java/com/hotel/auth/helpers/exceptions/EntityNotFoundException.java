package com.hotel.auth.helpers.exceptions;

public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Object identifier;

    public EntityNotFoundException(String entityName, Object identifier) {
        super(String.format("%s con identificador '%s' no fue encontrado", entityName, identifier));
        this.entityName = entityName;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.entityName = null;
        this.identifier = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
