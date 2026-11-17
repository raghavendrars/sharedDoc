package com.kidslearn.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.List;
import java.util.Locale;

/** Speaks with the phone's own text-to-speech voice (English, Kannada, Hindi), plays right/wrong tones and tells the talking face when it speaks. */
final class Speaker implements TextToSpeech.OnInitListener {
    interface Talk {
        void talking(boolean on);
    }

    boolean on;
    Talk talk;
    private TextToSpeech tts;
    private boolean ready;
    private String current = "";
    private ToneGenerator tone;
    private int seq;
    private final Handler main = new Handler(Looper.getMainLooper());

    Speaker(Context c, boolean on) {
        this.on = on;
        try {
            tts = new TextToSpeech(c.getApplicationContext(), this);
        } catch (RuntimeException e) {
            tts = null;
        }
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        } catch (RuntimeException e) {
            tone = null;
        }
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || tts == null) return;
        try {
            tts.setSpeechRate(0.85f);
            tts.setPitch(1.1f);
            ready = use("en");
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                    post(!id.startsWith("gap"));
                }

                @Override
                public void onDone(String id) {
                    post(false);
                }

                @Override
                public void onError(String id) {
                    post(false);
                }

                @Override
                public void onStop(String id, boolean interrupted) {
                    post(false);
                }
            });
        } catch (RuntimeException e) {
            ready = false;
        }
    }

    private void post(final boolean v) {
        main.post(() -> {
            if (talk != null) talk.talking(v);
        });
    }

    private static boolean ok(int result) {
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
    }

    /** Switches the voice. English tries the Indian accent first, then US, then any English. */
    private boolean use(String lang) {
        boolean good;
        if (lang.equals("en")) {
            good = ok(tts.setLanguage(new Locale("en", "IN"))) || ok(tts.setLanguage(Locale.US)) || ok(tts.setLanguage(Locale.ENGLISH));
        } else {
            good = ok(tts.setLanguage(new Locale(lang, "IN")));
        }
        if (good) current = lang;
        return good;
    }

    void say(String s) {
        say(s, "en");
    }

    /** Returns false only when the phone has no voice for that language; true otherwise (including when sound is off). */
    boolean say(String s, String lang) {
        if (!on || s == null || s.isEmpty()) return true;
        if (!ready) return lang.equals("en");
        try {
            if (!lang.equals(current) && !use(lang)) {
                use("en");
                return false;
            }
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "u" + (seq++));
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    /** Speaks the lines one after another with a pause after each. */
    boolean sayLines(List<String> lines, String lang, int gapMs) {
        if (!on || lines.isEmpty()) return true;
        if (!ready) return lang.equals("en");
        try {
            if (!lang.equals(current) && !use(lang)) {
                use("en");
                return false;
            }
            for (int i = 0; i < lines.size(); i++) {
                tts.speak(lines.get(i), i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, "u" + (seq++));
                if (gapMs > 0 && i < lines.size() - 1) tts.playSilentUtterance(gapMs, TextToSpeech.QUEUE_ADD, "gap" + (seq++));
            }
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    void good() {
        beep(ToneGenerator.TONE_PROP_ACK);
    }

    void bad() {
        beep(ToneGenerator.TONE_PROP_NACK);
    }

    private void beep(int type) {
        if (!on || tone == null) return;
        try {
            tone.startTone(type, 180);
        } catch (RuntimeException ignored) {
        }
    }

    void stop() {
        if (tts != null && ready) {
            try {
                tts.stop();
            } catch (RuntimeException ignored) {
            }
        }
        post(false);
    }

    void shutdown() {
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (RuntimeException ignored) {
            }
        }
        if (tone != null) tone.release();
    }
}
