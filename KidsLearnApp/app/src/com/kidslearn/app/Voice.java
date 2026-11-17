package com.kidslearn.app;

import android.Manifest;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Records the child's voice (up to 10 s) with a live sound-wave display, and plays it back with the same wave. */
@SuppressWarnings("deprecation")
final class Voice {
    interface Status {
        void on(String message, boolean recording);
    }

    private final MainActivity a;
    private final WaveView wave;
    private final File file;
    private final List<Float> levels = new ArrayList<>();
    private final Handler h = new Handler(Looper.getMainLooper());
    private MediaRecorder rec;
    private MediaPlayer player;
    private Status status;
    private int replay;

    private final Runnable pollRec = new Runnable() {
        @Override
        public void run() {
            if (rec == null) return;
            float lvl;
            try {
                lvl = (float) Math.sqrt(rec.getMaxAmplitude() / 32767f);
            } catch (RuntimeException e) {
                lvl = 0;
            }
            levels.add(lvl);
            wave.add(lvl);
            h.postDelayed(this, 60);
        }
    };

    private final Runnable pollPlay = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;
            wave.add(replay < levels.size() ? levels.get(replay++) : 0f);
            h.postDelayed(this, 60);
        }
    };

    Voice(MainActivity a, WaveView wave, String name, Status status) {
        this.a = a;
        this.wave = wave;
        this.status = status;
        this.file = new File(a.getCacheDir(), name + ".m4a");
    }

    void toggleRecord(final String what) {
        if (rec != null) {
            stopRec();
            status.on("Recorded! Press ▶ Me to hear yourself", false);
            return;
        }
        a.withPermission(Manifest.permission.RECORD_AUDIO, () -> start(what));
    }

    private void start(String what) {
        stopPlay();
        a.speaker.stop();
        levels.clear();
        wave.clear();
        try {
            rec = new MediaRecorder();
            rec.setAudioSource(MediaRecorder.AudioSource.MIC);
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            rec.setAudioSamplingRate(44100);
            rec.setAudioEncodingBitRate(96000);
            rec.setMaxDuration(10000);
            rec.setOutputFile(file.getAbsolutePath());
            rec.setOnInfoListener((mr, w, extra) -> {
                if (w == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRec();
                    status.on("Recorded! Press ▶ Me to hear yourself", false);
                }
            });
            rec.prepare();
            rec.start();
            h.post(pollRec);
            status.on("🔴 Recording… say “" + what + "”", true);
        } catch (Exception e) {
            if (rec != null) rec.release();
            rec = null;
            status.on("The microphone could not be used", false);
        }
    }

    private void stopRec() {
        if (rec == null) return;
        h.removeCallbacks(pollRec);
        try {
            rec.stop();
        } catch (RuntimeException ignored) {
        }
        rec.release();
        rec = null;
    }

    void play() {
        if (rec != null) stopRec();
        if (!file.exists() || file.length() == 0) {
            status.on("Press 🎙 Record first", false);
            return;
        }
        stopPlay();
        try {
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(mp -> stopPlay());
            player.prepare();
            player.start();
            replay = 0;
            wave.clear();
            h.post(pollPlay);
            status.on("🔈 This is you!", false);
        } catch (Exception e) {
            stopPlay();
            status.on("Could not play the recording", false);
        }
    }

    private void stopPlay() {
        if (player == null) return;
        h.removeCallbacks(pollPlay);
        try {
            player.release();
        } catch (RuntimeException ignored) {
        }
        player = null;
    }

    void release() {
        stopRec();
        stopPlay();
    }
}

/** Scrolling sound-wave bars. */
final class WaveView extends View {
    private static final int N = 60;
    private final float[] v = new float[N];
    private int head;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    WaveView(Context c) {
        super(c);
        p.setColor(0xFFFFFFFF);
    }

    void add(float level) {
        v[head] = Math.max(0f, Math.min(1f, level));
        head = (head + 1) % N;
        invalidate();
    }

    void clear() {
        for (int i = 0; i < N; i++) v[i] = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), bw = w / N, mid = h / 2;
        for (int i = 0; i < N; i++) {
            float lvl = v[(head + i) % N], bh = Math.max(Ui.dp(getContext(), 2), lvl * h * 0.95f);
            p.setAlpha(110 + (int) (145 * i / (float) N));
            r.set(i * bw + bw * 0.2f, mid - bh / 2, (i + 1) * bw - bw * 0.2f, mid + bh / 2);
            c.drawRoundRect(r, bw / 2, bw / 2, p);
        }
    }
}
