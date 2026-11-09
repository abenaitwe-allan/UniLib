package com.example.unilib.model;
import com.google.firebase.firestore.DocumentId;

import java.util.List;

public class User {
    @DocumentId
    private String id;          // filled by Firestore from the document id (the Auth uid)
    private String name;
    private String email;
    /**
     * Kept as a plain String on purpose. Firestore's own enum mapper throws when
     * it meets a value it does not know, so a role typed by hand in the console
     * (or left missing) would take the whole app down. Reading it as a String and
     * parsing it with {@link Role#fromString(String)} keeps the app alive and lets
     * us show a proper message.
     */
    private String role;
    /** The programme the member belongs to, for example "Computer Science". */
    private String programId;
    /** The name of that programme, kept here so it can be shown without a read. */
    private String programName;
    /** The course number the member is in, for example "CSC1101". */
    private String courseCode;
    /** The name of that course unit, for example "Programming Fundamentals". */
    private String courseTitle;
    private boolean active;
    /** For lecturers: programmes they teach (e.g. ["Computer Science", "Information Technology"]). */
    private List<String> teachingPrograms;

    public User() { }           // required by Firestore

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    /** The role exactly as Firestore stores it, for example "PRESIDENT". */
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<String> getTeachingPrograms() { return teachingPrograms; }
    public void setTeachingPrograms(List<String> teachingPrograms) { this.teachingPrograms = teachingPrograms; }

    /**
     * The role as an enum, or null when Firestore holds something we do not know.
     * Deliberately not a bean getter, so Firestore keeps reading and writing the
     * plain "role" string above and never adds a second field.
     */
    public Role parsedRole() { return Role.fromString(role); }
}
