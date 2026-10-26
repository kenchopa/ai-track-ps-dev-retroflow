package com.stackcraft.retroflow.entity;

/**
 * Lifecycle of a retrospective. The only allowed transition is OPEN to CLOSED.
 */
public enum RetrospectiveStatus {
    OPEN,
    CLOSED
}
