package com.example.unilib.model;

import java.util.Locale;

/**
 * The four kinds of account in UniLib.
 *
 * <p>The value kept in Firestore is always {@link #name()} (for example
 * {@code "PRESIDENT"}). Roles are often edited by hand in the Firebase console,
 * so {@link #fromString(String)} reads them loosely: extra spaces, any casing and
 * a few common spellings all work. Anything it does not recognise returns
 * {@code null} instead of throwing, so a typo in the console shows a readable
 * message rather than crashing the app.
 */
public enum Role {
    STUDENT, PRESIDENT, LECTURER, ADMIN;

    /** Reads a role out of a Firestore value. Returns null when it is unknown. */
    public static Role fromString(String raw) {
        if (raw == null) return null;
        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (key.isEmpty()) return null;

        for (Role role : values()) {
            if (role.name().equals(key)) return role;
        }

        // Spellings people tend to type in the Firebase console.
        if (key.equals("LECTURE") || key.equals("TEACHER") || key.equals("INSTRUCTOR")) return LECTURER;
        if (key.equals("CLASS_PRESIDENT")) return PRESIDENT;
        if (key.equals("ADMINISTRATOR")) return ADMIN;
        return null;
    }
}
