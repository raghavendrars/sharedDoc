package com.kidslearn.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.TimeZone;

/** Single activity. Each screen is a plain View swapped into the root frame. Also keeps the time log and the daily limit. */
public class MainActivity extends Activity {
    interface Heard {
        void on(ArrayList<String> results);
    }

    interface Result {
        void on(int resultCode, Intent data);
    }

    /** Time categories shown to parents. NONE is not counted (parents area, time-up screen). */
    static final String VIDEO = "video", AUDIO = "audio", QUIZ = "quiz", LEARN = "learn", NONE = "none";

    FrameLayout root;
    Speaker speaker;
    SharedPreferences prefs;
    Stats stats;
    final Random rnd = new Random();
    /** Stops the camera, microphone or player of the current screen; run when the screen changes or the app is paused. */
    Runnable cleanup;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SparseArray<Result> results = new SparseArray<>();
    private final SparseArray<Runnable> perms = new SparseArray<>();
    private boolean atHome, resumed, timeUp;
    private int gen, nextReq = 100;
    private String cat = LEARN;
    private long mark, awaySince;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            flush();
            checkLimit();
            handler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("kids", MODE_PRIVATE);
        stats = new Stats(this);
        stats.prune(today());
        speaker = new Speaker(this, prefs.getBoolean("sound", true));
        root = new FrameLayout(this);
        setContentView(root);
        home();
    }

    static int today() {
        long now = System.currentTimeMillis();
        return (int) ((now + TimeZone.getDefault().getOffset(now)) / 86400000L);
    }

    void show(View v, boolean isHome, int barColor) {
        show(v, isHome, barColor, LEARN);
    }

    void show(View v, boolean isHome, int barColor, String category) {
        flush();
        cat = category;
        gen++;
        atHome = isHome;
        if (cleanup != null) {
            Runnable c = cleanup;
            cleanup = null;
            c.run();
        }
        speaker.stop();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
        root.removeAllViews();
        root.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        getWindow().setStatusBarColor(Ui.darker(barColor));
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(220).start();
    }

    void home() {
        if (timeUp) {
            showTimeUp();
            return;
        }
        show(HomeScreen.build(this), true, HomeScreen.COLOR, LEARN);
    }

    // ---- time log and daily limit ----

    private void flush() {
        long now = SystemClock.elapsedRealtime();
        if (resumed && !timeUp && mark > 0) stats.add(today(), cat, now - mark);
        mark = now;
    }

    int limitMinutes() {
        return prefs.getInt("limit", 0);
    }

    long allowedMs() {
        return (limitMinutes() + stats.bonus(today())) * 60000L;
    }

    long usedToday() {
        flush();
        return stats.total(today());
    }

    private void checkLimit() {
        if (timeUp || limitMinutes() <= 0) return;
        if (stats.total(today()) >= allowedMs()) {
            timeUp = true;
            showTimeUp();
        }
    }

    private void showTimeUp() {
        show(ParentScreen.timeUp(this), false, ParentScreen.NIGHT, NONE);
    }

    void unlock(int extraMinutes) {
        stats.addBonus(today(), extraMinutes);
        timeUp = false;
        home();
    }

    /** Opens a parent's YouTube link outside the app; the time away is counted as video time. */
    void openVideo(String url) {
        try {
            awaySince = SystemClock.elapsedRealtime();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            awaySince = 0;
            Toast.makeText(this, "No app on this phone can open the video", Toast.LENGTH_LONG).show();
        }
    }

    // ---- small helpers for screens ----

    /** Runs r after ms, unless the child has moved to another screen meanwhile. */
    void later(long ms, final Runnable r) {
        final int g = gen;
        handler.postDelayed(() -> {
            if (g == gen && !isFinishing()) r.run();
        }, ms);
    }

    int stars() {
        return prefs.getInt("stars", 0);
    }

    void addStars(int n) {
        prefs.edit().putInt("stars", stars() + n).apply();
        View pill = root.findViewWithTag("stars");
        if (pill instanceof TextView) {
            ((TextView) pill).setText("⭐ " + stars());
            Ui.bounce(pill);
        }
    }

    void confetti() {
        root.addView(new ConfettiView(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    void forResult(Intent i, String failMessage, Result cb) {
        int req = nextReq++;
        results.put(req, cb);
        try {
            startActivityForResult(i, req);
        } catch (ActivityNotFoundException | SecurityException e) {
            results.remove(req);
            Toast.makeText(this, failMessage, Toast.LENGTH_LONG).show();
        }
    }

    void withPermission(String perm, Runnable granted) {
        if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
            granted.run();
            return;
        }
        int req = nextReq++;
        perms.put(req, granted);
        requestPermissions(new String[]{perm}, req);
    }

    /** Opens the phone's own voice-typing box so the child can say a word; the words it heard come back to cb. */
    void listen(String languageTag, String hint, final Heard cb) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, hint);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speaker.stop();
        forResult(i, "Voice typing is not available on this phone", (code, data) -> {
            ArrayList<String> list = data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (code == RESULT_OK && list != null && !list.isEmpty()) cb.on(list);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Result cb = results.get(requestCode);
        results.remove(requestCode);
        if (cb != null) cb.on(resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        Runnable r = perms.get(requestCode);
        perms.remove(requestCode);
        if (r == null) return;
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) r.run();
        else Toast.makeText(this, "Permission is needed for this", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (timeUp) moveTaskToBack(true);
        else if (atHome) super.onBackPressed();
        else home();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        long now = SystemClock.elapsedRealtime();
        if (awaySince > 0) {
            stats.add(today(), VIDEO, Math.min(now - awaySince, 2 * 3600000L));
            awaySince = 0;
        }
        mark = now;
        checkLimit();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 15000);
    }

    @Override
    protected void onPause() {
        flush();
        resumed = false;
        handler.removeCallbacks(tick);
        speaker.stop();
        if (cleanup != null) cleanup.run();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (cleanup != null) cleanup.run();
        speaker.shutdown();
        super.onDestroy();
    }
}
