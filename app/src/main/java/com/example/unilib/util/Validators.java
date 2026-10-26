package com.example.unilib.util;

import java.util.regex.Pattern;

public final class Validators {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME =
            Pattern.compile("^\\p{L}[\\p{L} .'-]{1,49}$");

    private Validators() { }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /** 6 to 64 characters, no spaces. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6 && password.length() <= 64
                && !password.contains(" ");
    }

    /** At least one letter and one digit. Used when registering. */
    public static boolean hasLetterAndDigit(String password) {
        if (password == null) return false;
        boolean letter = false, digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        return letter && digit;
    }

    /** 2 to 50 characters; letters, spaces, dots, apostrophes and hyphens; starts with a letter. */
    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name.trim()).matches();
    }
}