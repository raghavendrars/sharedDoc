package com.pocketledger.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/** Fingerprint unlock. Kept in its own class because the system prompt only exists on Android 9 and newer. */
final class Bio {
    private Bio() { }

    static boolean available(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                BiometricManager bm = c.getSystemService(BiometricManager.class);
                return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
            }
            if (Build.VERSION.SDK_INT >= 28) return c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        } catch (Exception ignored) { }
        return false;
    }

    static void prompt(final Activity a, String title, final Runnable onSuccess) {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            BiometricPrompt p = new BiometricPrompt.Builder(a)
                    .setTitle(title)
                    .setSubtitle("Confirm it is you")
                    .setNegativeButton("Use password", a.getMainExecutor(), (dialog, which) -> { })
                    .build();
            p.authenticate(new CancellationSignal(), a.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    onSuccess.run();
                }
            });
        } catch (Exception ignored) {
            // No sensor or nothing enrolled: the password box is still there.
        }
    }
}

/** First run: create the password and book name. Afterwards: ask for the password or fingerprint on every open. */
public class LockActivity extends Activity {
    private boolean setup, bioShown;
    private EditText bookName, password, confirm;
    private CheckBox useBio;
    private TextView error;
    private static int failures;
    private static long blockedUntil;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_lock);
        setup = !Prefs.hasPassword(this);

        TextView title = findViewById(R.id.lockTitle), sub = findViewById(R.id.lockSub);
        bookName = findViewById(R.id.bookName);
        password = findViewById(R.id.password);
        confirm = findViewById(R.id.confirm);
        useBio = findViewById(R.id.useBio);
        error = findViewById(R.id.lockError);
        Button action = findViewById(R.id.lockAction), bioButton = findViewById(R.id.bioButton);
        boolean bio = Bio.available(this);

        if (setup) {
            title.setText("Welcome");
            sub.setText("Name your book and create a password. You will need the password every time the app opens.");
            action.setText("Create and continue");
            bioButton.setVisibility(View.GONE);
            useBio.setVisibility(bio ? View.VISIBLE : View.GONE);
            if (!bio) useBio.setChecked(false);
        } else {
            title.setText(Prefs.bookName(this));
            sub.setText("Enter your password to open");
            findViewById(R.id.bookLabel).setVisibility(View.GONE);
            bookName.setVisibility(View.GONE);
            findViewById(R.id.confirmLabel).setVisibility(View.GONE);
            confirm.setVisibility(View.GONE);
            useBio.setVisibility(View.GONE);
            boolean useFinger = bio && Prefs.biometric(this);
            bioButton.setVisibility(useFinger ? View.VISIBLE : View.GONE);
            bioButton.setOnClickListener(v -> Bio.prompt(this, "Unlock " + Prefs.bookName(this), this::unlock));
            password.setImeOptions(EditorInfo.IME_ACTION_DONE);
            password.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); return true; }
                return false;
            });
        }
        action.setOnClickListener(v -> submit());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!setup && !bioShown && Prefs.biometric(this) && Bio.available(this)) {
            bioShown = true;
            Bio.prompt(this, "Unlock " + Prefs.bookName(this), this::unlock);
        }
    }

    private void submit() {
        String pw = password.getText().toString();
        if (setup) {
            if (pw.length() < 4) { error.setText("Use at least 4 characters for the password."); return; }
            if (!pw.equals(confirm.getText().toString())) { error.setText("The two passwords do not match."); return; }
            if (!Prefs.setPassword(this, pw)) { error.setText("The password could not be saved. Please try again."); return; }
            Prefs.setBookName(this, bookName.getText().toString());
            Prefs.setBiometric(this, useBio.isChecked() && Bio.available(this));
            unlock();
            return;
        }
        long now = System.currentTimeMillis();
        if (now < blockedUntil) {
            error.setText("Too many wrong tries. Wait " + ((blockedUntil - now) / 1000 + 1) + " seconds.");
            return;
        }
        if (Prefs.checkPassword(this, pw)) {
            failures = 0;
            unlock();
        } else {
            failures++;
            if (failures >= 5) blockedUntil = now + 30_000L;
            error.setText("Wrong password.");
            password.setText("");
        }
    }

    private void unlock() {
        Session.locked = false;
        finish();
    }

    @Override
    public void onBackPressed() {
        // Leaving the lock screen closes the app instead of revealing it.
        finishAffinity();
    }
}
