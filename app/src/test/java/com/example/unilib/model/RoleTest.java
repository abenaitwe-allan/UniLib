package com.example.unilib.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Roles are often edited by hand in the Firebase console, so reading them must
 * never throw. An unknown value has to come back as null so the app can show a
 * readable message instead of crashing.
 */
public class RoleTest {

    @Test
    public void readsTheExactNames() {
        assertEquals(Role.STUDENT, Role.fromString("STUDENT"));
        assertEquals(Role.PRESIDENT, Role.fromString("PRESIDENT"));
        assertEquals(Role.LECTURER, Role.fromString("LECTURER"));
        assertEquals(Role.ADMIN, Role.fromString("ADMIN"));
    }

    @Test
    public void ignoresCaseAndSpaces() {
        assertEquals(Role.PRESIDENT, Role.fromString("president"));
        assertEquals(Role.ADMIN, Role.fromString("Admin"));
        assertEquals(Role.LECTURER, Role.fromString("  lecturer  "));
        assertEquals(Role.STUDENT, Role.fromString("student"));
    }

    @Test
    public void understandsTheUsualMistakes() {
        assertEquals(Role.LECTURER, Role.fromString("Lecture"));
        assertEquals(Role.LECTURER, Role.fromString("TEACHER"));
        assertEquals(Role.PRESIDENT, Role.fromString("Class President"));
        assertEquals(Role.ADMIN, Role.fromString("Administrator"));
    }

    @Test
    public void returnsNullForAnythingElse() {
        assertNull(Role.fromString(null));
        assertNull(Role.fromString(""));
        assertNull(Role.fromString("   "));
        assertNull(Role.fromString("SUPERUSER"));
        assertNull(Role.fromString("123"));
    }
}
