package com.wastina.trust.controller;

/**
 * Signals that a requested resource does not exist. Mapped to {@code 404} so that services can
 * stop throwing a bare {@code RuntimeException}, which previously surfaced as {@code 500}.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException of(String resource, Object identifier) {
        return new EntityNotFoundException(resource + " was not found: " + identifier + ".");
    }
}