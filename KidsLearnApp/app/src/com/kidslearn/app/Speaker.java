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

/**
 * Speaks with the phone's own text-to-speech voice (English in one chosen accent, Kannada, Hindi), plays right/wrong
 * tones, tells the talking face when it speaks, and reports which word is being read (for read-along).
 */
final class Speaker implements TextToSpeech.OnInitListener {
    interface Talk {
        void talking(boolean on);
    }

    interface Track {
        void range(String id, int start, int end);

        void done(String id);
    }

    boolean on;
    Talk talk;
    Track track;
    private String accent;
    private TextToSpeech tts;
    private boolean ready;
    private String current = "";
    private ToneGenerator tone;
    private int seq;
    private final Handler main = new Handler(Looper.getMainLooper());

    Speaker(Context c, boolean on, String accent) {
        this.on = on;
        this.accent = accent;
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

    /** "GB", "US" or "IN": the one English accent used everywhere in the app. */
    static Locale english(String accent) {
        if ("US".equals(accent)) return Locale.US;
        if ("IN".equals(accent)) return new Locale("en", "IN");
        return Locale.UK;
    }

    void setAccent(String accent) {
        this.accent = accent;
        current = "";
    }

    private float rate = 0.85f, pitch = 1.1f;
    private final java.util.Map<String, String> voiceNames = new java.util.HashMap<>();

    void setRate(float r) {
        rate = r;
        if (tts != null && ready) tts.setSpeechRate(r);
    }

    void setPitch(float p) {
        pitch = p;
        if (tts != null && ready) tts.setPitch(p);
    }

    /** A chosen installed voice for "en", "kn" or "hi" (null = the phone's default). */
    void setVoice(String lang, String name) {
        if (name == null || name.isEmpty()) voiceNames.remove(lang);
        else voiceNames.put(lang, name);
        current = "";
    }

    /** Names of the installed voices for a language (empty until the voice engine is ready). */
    java.util.List<String> voices(String lang) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (tts == null || !ready) return out;
        String want = lang.equals("en") ? "en" : lang;
        try {
            for (android.speech.tts.Voice v : tts.getVoices()) {
                if (v.getLocale() == null || !want.equals(v.getLocale().getLanguage())) continue;
                if (lang.equals("en") && !v.getLocale().getCountry().equals(english(accent).getCountry())) continue;
                if (v.getFeatures() != null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                out.add(v.getName());
            }
        } catch (RuntimeException ignored) {
        }
        java.util.Collections.sort(out);
        return out;
    }

    private void applyVoice(String lang) {
        String name = voiceNames.get(lang);
        if (name == null) return;
        try {
            for (android.speech.tts.Voice v : tts.getVoices())
                if (v.getName().equals(name)) {
                    tts.setVoice(v);
                    return;
                }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || tts == null) return;
        try {
            tts.setSpeechRate(rate);
            tts.setPitch(pitch);
            ready = use("en");
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                    post(!id.startsWith("gap"));
                }

                @Override
                public void onDone(final String id) {
                    post(false);
                    main.post(() -> {
                        if (track != null) track.done(id);
                    });
                }

                @Override
                public void onError(String id) {
                    post(false);
                }

                @Override
                public void onStop(String id, boolean interrupted) {
                    post(false);
                }

                @Override
                public void onRangeStart(final String id, final int start, final int end, int frame) {
                    main.post(() -> {
                        if (track != null) track.range(id, start, end);
                    });
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

    /** Switches the voice. English uses the chosen accent, falling back to any English voice. */
    private boolean use(String lang) {
        boolean good;
        if (lang.equals("en")) good = ok(tts.setLanguage(english(accent))) || ok(tts.setLanguage(Locale.ENGLISH));
        else good = ok(tts.setLanguage(new Locale(lang, "IN")));
        if (good) {
            current = lang;
            applyVoice(lang);
            tts.setSpeechRate(rate);
            tts.setPitch(pitch);
        }
        return good;
    }

    void say(String s) {
        say(s, "en");
    }

    /** Returns false only when the phone has no voice for that language; true otherwise (including when sound is off). */
    boolean say(String s, String lang) {
        return speak(s, lang, "u" + (seq++));
    }

    /** Speaks and returns the id that read-along callbacks will carry, or null if nothing was spoken. */
    String sayTracked(String s) {
        return sayTracked(s, "en");
    }

    String sayTracked(String s, String lang) {
        String id = "t" + (seq++);
        return speak(s, lang, id) && on && ready ? id : null;
    }

    private boolean speak(String s, String lang, String id) {
        if (!on || s == null || s.isEmpty()) return true;
        if (!ready) return lang.equals("en");
        try {
            if (!lang.equals(current) && !use(lang)) {
                use("en");
                return false;
            }
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, id);
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
