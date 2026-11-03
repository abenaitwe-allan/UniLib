package com.example.unilib.ui;
import android.app.Activity;
import android.content.Intent;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.ui.admin.AdminDashboardActivity;
import com.example.unilib.ui.auth.LoginActivity;
import com.example.unilib.ui.lecturer.LecturerInboxActivity;
import com.example.unilib.ui.president.PresidentDashboardActivity;
import com.example.unilib.ui.student.StudentHomeActivity;
public final class RoleRouter {
    private RoleRouter() { }
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_ROLE = "extra_role";
    public static final String EXTRA_PROGRAM_ID = "extra_program_id";
    public static final String EXTRA_PROGRAM_NAME = "extra_program_name";

    public static void openHomeFor(Activity from, User user) {
        Role role = user.parsedRole();
        if (role == null) {
            // No usable role: drop the session and go back to the login screen.
            new AuthRepository(from).logout();
            Intent back = new Intent(from, LoginActivity.class);
            back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            from.startActivity(back);
            from.finish();
            return;
        }

        Class<?> target;
        switch (role) {
            case ADMIN:     target = AdminDashboardActivity.class; break;
            case LECTURER:  target = LecturerInboxActivity.class; break;
            case PRESIDENT: target = PresidentDashboardActivity.class; break;
            default:        target = StudentHomeActivity.class; break;
        }
        Intent i = new Intent(from, target);
        i.putExtra(EXTRA_NAME, user.getName());
        i.putExtra(EXTRA_ROLE, role.name());
        i.putExtra(EXTRA_PROGRAM_ID, user.getProgramId());
        i.putExtra(EXTRA_PROGRAM_NAME, user.getProgramName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(i);
        from.finish();
    }
}
