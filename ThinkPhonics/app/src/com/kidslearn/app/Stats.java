package com.kidslearn.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Time spent per day and category, quiz scores, parent's YouTube links, and backup to / from JSON. */
final class Stats {
    static final String[] CATS = {MainActivity.VIDEO, MainActivity.AUDIO, MainActivity.QUIZ, MainActivity.LEARN, MainActivity.GAME};
    static final String[] NAMES = {"Videos", "Audio", "Quiz", "Other learning", "Games"};
    static final int[] COLORS = {0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA};
    final SharedPreferences sp;

    Stats(Context c) {
        sp = c.getSharedPreferences("stats", Context.MODE_PRIVATE);
    }

    void add(int day, String cat, long ms) {
        if (ms <= 0 || ms > 3 * 3600000L || MainActivity.NONE.equals(cat)) return;
        String k = "t" + day + "_" + cat;
        sp.edit().putLong(k, sp.getLong(k, 0) + ms).apply();
    }

    long get(int day, String cat) {
        return sp.getLong("t" + day + "_" + cat, 0);
    }

    long total(int day) {
        long t = 0;
        for (String c : CATS) t += get(day, c);
        return t;
    }

    /** Days in a row with at least one minute of learning (today counts once it has a minute). */
    int streak(int today) {
        int d = today, n = 0;
        if (total(d) < 60000) d--;
        while (total(d) >= 60000) {
            n++;
            d--;
        }
        return n;
    }

    int gameBonus(int day) {
        return sp.getInt("g" + day, 0);
    }

    void addGameBonus(int day, int minutes) {
        sp.edit().putInt("g" + day, gameBonus(day) + minutes).apply();
    }

    int bonus(int day) {
        return sp.getInt("b" + day, 0);
    }

    void addBonus(int day, int minutes) {
        sp.edit().putInt("b" + day, bonus(day) + minutes).apply();
    }

    /** Drops time entries older than 60 days. */
    void prune(int today) {
        SharedPreferences.Editor ed = sp.edit();
        for (String k : sp.getAll().keySet()) {
            if (!k.startsWith("t") && !k.startsWith("b") && !k.startsWith("g")) continue;
            int end = k.indexOf('_');
            try {
                int day = Integer.parseInt(k.substring(1, end < 0 ? k.length() : end));
                if (day < today - 60) ed.remove(k);
            } catch (NumberFormatException ignored) {
            }
        }
        ed.apply();
    }

    void addScore(String quiz, int won, int total) {
        JSONArray old = scores(), out = new JSONArray();
        for (int i = Math.max(0, old.length() - 199); i < old.length(); i++) out.put(old.opt(i));
        try {
            JSONObject o = new JSONObject();
            o.put("d", MainActivity.today());
            o.put("q", quiz);
            o.put("w", won);
            o.put("t", total);
            out.put(o);
        } catch (JSONException ignored) {
        }
        sp.edit().putString("scores", out.toString()).apply();
    }

    JSONArray scores() {
        try {
            return new JSONArray(sp.getString("scores", "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    /** Parent's video links: [title, url] pairs. */
    List<String[]> links() {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString("links", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new String[]{o.getString("t"), o.getString("u")});
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    void setLinks(List<String[]> links) {
        JSONArray arr = new JSONArray();
        try {
            for (String[] l : links) {
                JSONObject o = new JSONObject();
                o.put("t", l[0]);
                o.put("u", l[1]);
                arr.put(o);
            }
        } catch (JSONException ignored) {
        }
        sp.edit().putString("links", arr.toString()).apply();
    }

    static String date(int day) {
        SimpleDateFormat f = new SimpleDateFormat("EEE d MMM", Locale.ENGLISH);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(day * 86400000L));
    }

    static String dayShort(int day) {
        SimpleDateFormat f = new SimpleDateFormat("EEE", Locale.ENGLISH);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(day * 86400000L));
    }

    static String mins(long ms) {
        if (ms > 0 && ms < 60000) return "under 1 min";
        return Math.round(ms / 60000.0) + " min";
    }

    // ---- backup ----

    static JSONObject dump(SharedPreferences p) throws JSONException {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            Object v = e.getValue();
            JSONObject x = new JSONObject();
            if (v instanceof Boolean) x.put("b", v);
            else if (v instanceof Integer) x.put("i", v);
            else if (v instanceof Long) x.put("l", v);
            else if (v instanceof Float) x.put("f", ((Float) v).doubleValue());
            else if (v instanceof String) x.put("s", v);
            else continue;
            o.put(e.getKey(), x);
        }
        return o;
    }

    static void restore(SharedPreferences p, JSONObject o) throws JSONException {
        SharedPreferences.Editor ed = p.edit().clear();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            JSONObject x = o.getJSONObject(k);
            if (x.has("b")) ed.putBoolean(k, x.getBoolean("b"));
            else if (x.has("i")) ed.putInt(k, x.getInt("i"));
            else if (x.has("l")) ed.putLong(k, x.getLong("l"));
            else if (x.has("f")) ed.putFloat(k, (float) x.getDouble("f"));
            else if (x.has("s")) ed.putString(k, x.getString("s"));
        }
        ed.commit();
    }
}
