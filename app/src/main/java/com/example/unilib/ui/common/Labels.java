package com.example.unilib.ui.common;

import android.content.Context;

import com.example.unilib.R;
import com.example.unilib.model.DocStatus;
import com.example.unilib.model.Document;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;

/** Turns roles and document statuses into the words and colours shown on screen. */
public final class Labels {

    private Labels() { }

    public static String role(Context ctx, Role role) {
        if (role == null) return ctx.getString(R.string.role_unknown);
        switch (role) {
            case PRESIDENT: return ctx.getString(R.string.role_president);
            case LECTURER:  return ctx.getString(R.string.role_lecturer);
            case ADMIN:     return ctx.getString(R.string.role_admin);
            default:        return ctx.getString(R.string.role_student);
        }
    }

    public static int roleColor(Context ctx, Role role) {
        if (role == null) return R.color.unilib_text_secondary;
        switch (role) {
            case PRESIDENT: return R.color.unilib_accent;
            case LECTURER:  return R.color.unilib_primary;
            case ADMIN:     return R.color.unilib_error;
            default:        return R.color.unilib_text_secondary;
        }
    }

    public static String status(Context ctx, DocStatus status) {        if (status == null) return ctx.getString(R.string.status_unknown);
        switch (status) {
            case APPROVED: return ctx.getString(R.string.status_approved);
            case REJECTED: return ctx.getString(R.string.status_rejected);
            default:       return ctx.getString(R.string.status_pending);
        }
    }

    public static int statusColor(Context ctx, DocStatus status) {
        if (status == null) return R.color.unilib_text_secondary;
        switch (status) {
            case APPROVED: return R.color.unilib_success;
            case REJECTED: return R.color.unilib_error;
            default:       return R.color.unilib_pending;
        }
    }

    /** The bordered background that matches a document status. */
    public static int statusBackground(Context ctx, DocStatus status) {
        if (status == null) return R.drawable.bg_status_pending;
        switch (status) {
            case APPROVED: return R.drawable.bg_status_approved;
            case REJECTED: return R.drawable.bg_status_rejected;
            default:       return R.drawable.bg_status_pending;
        }
    }

    /**
     * The program and the course unit of a document, as one line. Documents
     * that were shared before the program was picked only have the course
     * number, so a missing part is left out instead of leaving a stray dot.
     */
    public static String programAndCourse(Document document) {
        if (document == null) return "";
        String program = document.getProgramName() == null ? "" : document.getProgramName().trim();
        String course = document.getCourseCode() == null ? "" : document.getCourseCode().trim();
        if (program.isEmpty()) return course;
        if (course.isEmpty()) return program;
        return program + " · " + course;
    }

    /** The same, for a member of the library. */
    public static String programAndCourse(User user) {
        if (user == null) return "";
        String program = user.getProgramName() == null ? "" : user.getProgramName().trim();
        String course = user.getCourseCode() == null ? "" : user.getCourseCode().trim();
        if (program.isEmpty()) return course;
        if (course.isEmpty()) return program;
        return program + " · " + course;
    }

    /** The line under the title of a document: where it belongs and how big it is. */
    public static String documentMeta(Context ctx, Document document) {
        String size = ctx.getString(R.string.doc_size,
                document.getFileSize() / 1024);
        String where = programAndCourse(document);
        return where.isEmpty() ? size : where + "  |  " + size;
    }

    /** The line under the title of a document that waits for a reviewer. */
    public static String reviewMeta(Context ctx, Document document) {
        String by = ctx.getString(R.string.review_shared_by,
                document.getUploaderName() == null ? "" : document.getUploaderName());
        String where = programAndCourse(document);
        return where.isEmpty() ? by : where + "  |  " + by;
    }

    /** The line under the name of a member: the email and where they study. */
    public static String memberMeta(User user) {
        String email = user == null || user.getEmail() == null ? "" : user.getEmail();
        String where = programAndCourse(user);
        if (where.isEmpty()) return email;
        if (email.isEmpty()) return where;
        return email + "  ·  " + where;
    }
}
