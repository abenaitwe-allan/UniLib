package com.example.unilib.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * A programme of study, for example "Computer Science".
 *
 * <p>The admin creates these. Students pick one when they register, and the
 * course units they can then choose are the ones that belong to it.
 */
public class Program {

    @DocumentId
    private String id;
    /** The full name shown on screen, for example "Computer Science". */
    private String name;
    /** The short code used in course unit numbers, for example "CSC". */
    private String code;
    /** A programme that is switched off is hidden but keeps its course units. */
    private boolean active;
    private Timestamp createdAt;

    public Program() { }           // required by Firestore

    public Program(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
