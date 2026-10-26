package com.example.unilib.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * One course unit inside a programme, for example "CSC1101 Programming
 * Fundamentals" under "Computer Science".
 *
 * <p>The admin creates these. A student only sees the course units that belong
 * to the programme they picked.
 */
public class CourseUnit {

    @DocumentId
    private String id;
    /** The id of the programme this course unit belongs to. */
    private String programId;
    /** The course number, for example "CSC1101". */
    private String code;
    /** The course name, for example "Programming Fundamentals". */
    private String title;
    /** A course unit that is switched off is hidden but keeps its documents. */
    private boolean active;
    private Timestamp createdAt;

    public CourseUnit() { }           // required by Firestore

    public CourseUnit(String id, String programId, String code, String title) {
        this.id = id;
        this.programId = programId;
        this.code = code;
        this.title = title;
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** The text shown in the dropdowns, for example "CSC1101 Programming Fundamentals". */
    public String label() {
        if (code == null || code.isEmpty()) return title == null ? "" : title;
        if (title == null || title.isEmpty()) return code;
        return code + " " + title;
    }
}
