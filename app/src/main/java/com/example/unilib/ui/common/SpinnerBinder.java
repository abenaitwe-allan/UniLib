package com.example.unilib.ui.common;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.unilib.R;

import java.util.List;

/**
 * Fills the dropdowns in the app.
 *
 * <p>The platform dropdown layouts take their text colour from the device theme,
 * which on a phone in dark mode is a light colour. On the light background this
 * app uses that makes the options unreadable, so every dropdown is built from the
 * app's own layout with the colour pinned.
 */
public final class SpinnerBinder {

    private SpinnerBinder() { }

    /** Fills a dropdown with the given options and selects the first one. */
    public static void bind(Spinner spinner, List<String> options) {
        bind(spinner, options, 0);
    }

    /** Fills a dropdown and selects the option at the given position. */
    public static void bind(Spinner spinner, List<String> options, int selectedPosition) {
        Context context = spinner.getContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_spinner, options);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
        if (selectedPosition >= 0 && selectedPosition < options.size()) {
            spinner.setSelection(selectedPosition);
        }
    }
}
