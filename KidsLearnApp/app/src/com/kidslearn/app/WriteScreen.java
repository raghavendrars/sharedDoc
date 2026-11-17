package com.kidslearn.app;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Finger-writing with automatic checking, plus recording your own voice saying it. */
@SuppressWarnings("deprecation")
final class WriteScreen {
    static final int COLOR = 0xFF00695C;
    static final String[] MODES = {"Letters", "Numbers", "Words", "Kannada", "Hindi"};
    private static final String[] WORDS = {"cat", "dog", "sun", "hat", "pen", "bus", "cup", "box", "fish", "frog", "milk", "tree", "moon", "star", "ball", "book"};

    private final MainActivity a;
    private final int mode;
    private final List<String> items = new ArrayList<>();
    private final Set<Integer> starred = new HashSet<>();
    private int idx;
    private WritePad pad;
    private TextView target, result, recBtn, guideBtn;
    private MediaRecorder rec;
    private MediaPlayer player;
    private final File file;

    private WriteScreen(MainActivity a, int mode) {
        this.a = a;
        this.mode = mode;
        this.file = new File(a.getCacheDir(), "my-voice.m4a");
        switch (mode) {
            case 0:
                for (char c = 'A'; c <= 'Z'; c++) items.add(String.valueOf(c));
                for (char c = 'a'; c <= 'z'; c++) items.add(String.valueOf(c));
                break;
            case 1:
                for (int i = 0; i <= 20; i++) items.add(String.valueOf(i));
                break;
            case 2:
                for (String w : WORDS) items.add(w);
                break;
            default:
                String[][] src = mode == 3 ? Data.join(Data.KN_V, Data.KN_C) : Data.join(Data.HI_V, Data.HI_C);
                for (String[] r : src) items.add(r[0]);
                break;
        }
    }

    static Runnable open(final MainActivity a, final int mode) {
        return () -> a.show(new WriteScreen(a, mode).make(), false, COLOR, MainActivity.LEARN);
    }

    private String lang() {
        return mode == 3 ? "kn" : mode == 4 ? "hi" : "en";
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Write · " + MODES[mode]));

        target = Ui.text(a, "", 24, Color.WHITE, true);
        Ui.click(target, this::say);
        page.addView(target);

        pad = new WritePad(a);
        LinearLayout.LayoutParams plp = Ui.lp(Ui.MATCH, 0, 1);
        plp.setMargins(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
        page.addView(pad, plp);

        result = Ui.text(a, "", 19, 0xFFFFF59D, true);
        page.addView(result);

        LinearLayout row1 = new LinearLayout(a);
        row1.setPadding(Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6), 0);
        row1.addView(tool("◀", () -> show(idx - 1)), Ui.lp(a, 0, Ui.dp(a, 48), 0.8f, 3));
        guideBtn = tool("Guide ✓", this::toggleGuide);
        row1.addView(guideBtn, Ui.lp(a, 0, Ui.dp(a, 48), 1.3f, 3));
        row1.addView(tool("Undo", () -> pad.undo()), Ui.lp(a, 0, Ui.dp(a, 48), 1f, 3));
        row1.addView(tool("Clear", () -> {
            pad.clear();
            result.setText("");
        }), Ui.lp(a, 0, Ui.dp(a, 48), 1f, 3));
        row1.addView(tool("▶", () -> show(idx + 1)), Ui.lp(a, 0, Ui.dp(a, 48), 0.8f, 3));
        page.addView(row1);

        LinearLayout row2 = new LinearLayout(a);
        row2.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), Ui.dp(a, 14));
        TextView check = tool("✓ Check", this::check);
        check.setBackground(Ui.round(0xFFFFD54F, Ui.dp(a, 14)));
        row2.addView(check, Ui.lp(a, 0, Ui.dp(a, 52), 1f, 3));
        recBtn = tool("🎙 Record", this::record);
        row2.addView(recBtn, Ui.lp(a, 0, Ui.dp(a, 52), 1f, 3));
        row2.addView(tool("▶ Hear me", this::playBack), Ui.lp(a, 0, Ui.dp(a, 52), 1f, 3));
        page.addView(row2);

        a.cleanup = this::stopAudio;
        show(0);
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private void show(int i) {
        if (i < 0 || i >= items.size()) return;
        idx = i;
        String it = items.get(i);
        target.setText("Write:  " + it + "    (" + (i + 1) + "/" + items.size() + ")  🔊");
        pad.set(it);
        result.setText("Trace it with your finger, then press ✓ Check");
        say();
    }

    private void say() {
        String it = items.get(idx);
        if (!a.speaker.say(it, lang())) result.setText("No " + MODES[mode] + " voice on this phone");
    }

    private void toggleGuide() {
        pad.guide = !pad.guide;
        pad.invalidate();
        guideBtn.setText(pad.guide ? "Guide ✓" : "Guide ✗");
        result.setText(pad.guide ? "Trace over the grey shape" : "Write it from memory!");
    }

    private void check() {
        float s = pad.score();
        if (s < 0) {
            result.setText("Write something first ✏️");
            return;
        }
        if (s >= 0.72f) {
            result.setText("⭐⭐⭐ Excellent writing!");
            a.speaker.good();
            a.speaker.say("Excellent!");
            reward(2);
        } else if (s >= 0.52f) {
            result.setText("⭐⭐ Good! Now say it aloud and record it 🎙");
            a.speaker.good();
            a.speaker.say("Good job!");
            reward(1);
        } else if (s >= 0.38f) {
            result.setText("⭐ Nearly! Clear and try once more");
            a.speaker.say("Nearly! Try again.");
        } else {
            result.setText("Try again — follow the shape carefully");
            a.speaker.bad();
            Ui.shake(pad);
        }
    }

    private void reward(int stars) {
        a.confetti();
        if (starred.add(idx)) a.addStars(stars);
    }

    // ---- voice recording ----

    private void record() {
        if (rec != null) {
            stopRec();
            result.setText("Recorded! Press ▶ Hear me");
            return;
        }
        a.withPermission(Manifest.permission.RECORD_AUDIO, this::startRec);
    }

    private void startRec() {
        stopPlay();
        a.speaker.stop();
        try {
            rec = new MediaRecorder();
            rec.setAudioSource(MediaRecorder.AudioSource.MIC);
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            rec.setAudioSamplingRate(44100);
            rec.setAudioEncodingBitRate(96000);
            rec.setMaxDuration(10000);
            rec.setOutputFile(file.getAbsolutePath());
            rec.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRec();
                    result.setText("Recorded! Press ▶ Hear me");
                }
            });
            rec.prepare();
            rec.start();
            recBtn.setText("⏹ Stop");
            result.setText("🔴 Recording… say “" + items.get(idx) + "”");
        } catch (Exception e) {
            if (rec != null) rec.release();
            rec = null;
            result.setText("The microphone could not be used");
        }
    }

    private void stopRec() {
        if (rec == null) return;
        try {
            rec.stop();
        } catch (RuntimeException ignored) {
        }
        rec.release();
        rec = null;
        if (recBtn != null) recBtn.setText("🎙 Record");
    }

    private void playBack() {
        if (rec != null) stopRec();
        if (!file.exists() || file.length() == 0) {
            result.setText("Press 🎙 Record first");
            return;
        }
        stopPlay();
        try {
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(mp -> stopPlay());
            player.prepare();
            player.start();
            result.setText("🔈 This is you!");
        } catch (Exception e) {
            stopPlay();
            result.setText("Could not play the recording");
        }
    }

    private void stopPlay() {
        if (player == null) return;
        try {
            player.release();
        } catch (RuntimeException ignored) {
        }
        player = null;
    }

    private void stopAudio() {
        stopRec();
        stopPlay();
    }
}

/** A writing pad. score() compares what was written with the shape of the target. */
final class WritePad extends View {
    private static final int G = 40;
    private final ArrayList<Path> strokes = new ArrayList<>();
    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path cur;
    private float lx, ly;
    private String target = "";
    boolean guide = true;

    WritePad(Context c) {
        super(c);
        setBackground(Ui.round(Color.WHITE, Ui.dp(c, 22)));
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setColor(0xFF1565C0);
        pen.setStrokeWidth(Ui.dp(c, 12));
        guidePaint.setColor(0xFFE0E4E8);
        guidePaint.setTypeface(Typeface.DEFAULT_BOLD);
        guidePaint.setTextAlign(Paint.Align.CENTER);
        linePaint.setColor(0xFFFFCDD2);
        linePaint.setStrokeWidth(Ui.dp(c, 1.5f));
    }

    void set(String t) {
        target = t;
        clear();
    }

    void undo() {
        if (!strokes.isEmpty()) strokes.remove(strokes.size() - 1);
        invalidate();
    }

    void clear() {
        strokes.clear();
        invalidate();
    }

    private void drawTarget(Canvas c, Paint paint, float w, float h) {
        paint.setTextSize(100);
        float tw = Math.max(1, paint.measureText(target));
        paint.setTextSize(Math.min(h * 0.7f, 100 * w * 0.85f / tw));
        Paint.FontMetrics fm = paint.getFontMetrics();
        c.drawText(target, w / 2, h / 2 - (fm.ascent + fm.descent) / 2, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                cur = new Path();
                cur.moveTo(x, y);
                cur.lineTo(x + 0.1f, y + 0.1f);
                strokes.add(cur);
                lx = x;
                ly = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (cur != null) {
                    cur.quadTo(lx, ly, (x + lx) / 2, (y + ly) / 2);
                    lx = x;
                    ly = y;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (cur != null) cur.lineTo(x, y);
                cur = null;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        c.drawLine(Ui.dp(getContext(), 16), h * 0.15f, w - Ui.dp(getContext(), 16), h * 0.15f, linePaint);
        c.drawLine(Ui.dp(getContext(), 16), h * 0.85f, w - Ui.dp(getContext(), 16), h * 0.85f, linePaint);
        if (guide && !target.isEmpty()) drawTarget(c, guidePaint, w, h);
        for (Path p : strokes) c.drawPath(p, pen);
    }

    /** 0..1 for how well the writing matches the target's shape, or -1 if nothing is written. */
    float score() {
        if (strokes.isEmpty() || getWidth() == 0) return -1;
        int w = Math.max(1, getWidth() / 4), h = Math.max(1, getHeight() / 4);
        Bitmap tb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas tc = new Canvas(tb);
        tc.scale(0.25f, 0.25f);
        Paint solid = new Paint(guidePaint);
        solid.setColor(Color.BLACK);
        drawTarget(tc, solid, getWidth(), getHeight());
        Bitmap sb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas sc = new Canvas(sb);
        sc.scale(0.25f, 0.25f);
        for (Path p : strokes) sc.drawPath(p, pen);
        boolean[] t = grid(tb), s = grid(sb);
        tb.recycle();
        sb.recycle();
        if (t == null || s == null) return -1;
        boolean[] td = dilate(t), sd = dilate(s);
        int tn = 0, tHit = 0, sn = 0, sHit = 0;
        for (int i = 0; i < G * G; i++) {
            if (t[i]) {
                tn++;
                if (sd[i]) tHit++;
            }
            if (s[i]) {
                sn++;
                if (td[i]) sHit++;
            }
        }
        float coverage = tHit / (float) tn, precision = sHit / (float) sn;
        return Math.min(coverage, precision);
    }

    /** The ink, cropped to its bounding box and scaled into a G×G grid (keeping its shape). */
    private static boolean[] grid(Bitmap b) {
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        int minx = w, miny = h, maxx = -1, maxy = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((px[y * w + x] >>> 24) > 64) {
                    if (x < minx) minx = x;
                    if (x > maxx) maxx = x;
                    if (y < miny) miny = y;
                    if (y > maxy) maxy = y;
                }
        if (maxx < 0) return null;
        int bw = maxx - minx + 1, bh = maxy - miny + 1, size = Math.max(bw, bh);
        float ox = (size - bw) / 2f, oy = (size - bh) / 2f;
        boolean[] g = new boolean[G * G];
        for (int y = miny; y <= maxy; y++)
            for (int x = minx; x <= maxx; x++)
                if ((px[y * w + x] >>> 24) > 64) {
                    int gx = Math.min(G - 1, (int) ((x - minx + ox) * G / size));
                    int gy = Math.min(G - 1, (int) ((y - miny + oy) * G / size));
                    g[gy * G + gx] = true;
                }
        return g;
    }

    private static boolean[] dilate(boolean[] g) {
        boolean[] out = new boolean[G * G];
        int r = 2;
        for (int y = 0; y < G; y++)
            for (int x = 0; x < G; x++) {
                if (!g[y * G + x]) continue;
                for (int dy = -r; dy <= r; dy++)
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < G && ny < G) out[ny * G + nx] = true;
                    }
            }
        return out;
    }
}
