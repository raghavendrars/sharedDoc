package com.kidslearn.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parents area behind a PIN: daily time limit, time spent, 7-day history, scores, YouTube links, backup. */
final class ParentScreen {
    static final int COLOR = 0xFF455A64, NIGHT = 0xFF283593;
    private static final int[] LIMITS = {0, 15, 30, 45, 60, 90, 120, 150, 180, 240};

    private ParentScreen() {
    }

    // ---- PIN ----

    private static String hash(String salt, String pin) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((salt + ":" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format(Locale.ROOT, "%02x", x));
            return b.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean pinOk(MainActivity a, String pin) {
        return hash(a.prefs.getString("pinSalt", ""), pin).equals(a.prefs.getString("pinHash", ""));
    }

    private static void savePin(MainActivity a, String pin) {
        byte[] s = new byte[16];
        new SecureRandom().nextBytes(s);
        String salt = Base64.encodeToString(s, Base64.NO_WRAP);
        a.prefs.edit().putString("pinSalt", salt).putString("pinHash", hash(salt, pin)).apply();
    }

    private static EditText pinField(MainActivity a, String hint) {
        EditText e = new EditText(a);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setTypeface(android.graphics.Typeface.DEFAULT);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        e.setHint(hint);
        e.setGravity(Gravity.CENTER);
        e.setTextSize(24);
        return e;
    }

    private static LinearLayout padded(MainActivity a, View... views) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 20);
        box.setPadding(p, Ui.dp(a, 8), p, 0);
        for (View v : views) box.addView(v);
        return box;
    }

    static void open(final MainActivity a) {
        final Runnable go = () -> a.show(dashboard(a), false, COLOR, MainActivity.NONE);
        if (!a.prefs.contains("pinHash")) createPin(a, go);
        else askPin(a, "Parents area", go);
    }

    static void askPin(final MainActivity a, String title, final Runnable ok) {
        final EditText e = pinField(a, "PIN");
        new AlertDialog.Builder(a).setTitle(title).setMessage("Enter the parent PIN").setView(padded(a, e))
                .setPositiveButton("OK", (d, w) -> {
                    if (pinOk(a, e.getText().toString())) ok.run();
                    else Toast.makeText(a, "Wrong PIN", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    static void createPin(final MainActivity a, final Runnable ok) {
        final EditText p1 = pinField(a, "New PIN (4–6 digits)"), p2 = pinField(a, "Type it again");
        new AlertDialog.Builder(a).setTitle("Create a parent PIN")
                .setMessage("This PIN protects the parents area and unlocks extra time. Keep it secret from the children.")
                .setView(padded(a, p1, p2))
                .setPositiveButton("Save", (d, w) -> {
                    String x = p1.getText().toString(), y = p2.getText().toString();
                    if (x.length() < 4) {
                        Toast.makeText(a, "Use 4 to 6 digits", Toast.LENGTH_LONG).show();
                        createPin(a, ok);
                    } else if (!x.equals(y)) {
                        Toast.makeText(a, "The two PINs are different", Toast.LENGTH_LONG).show();
                        createPin(a, ok);
                    } else {
                        savePin(a, x);
                        Toast.makeText(a, "PIN saved", Toast.LENGTH_SHORT).show();
                        ok.run();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ---- time-up screen ----

    static View timeUp(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(NIGHT);
        int p = Ui.dp(a, 28);
        page.setPadding(p, p, p, p);
        page.addView(Ui.text(a, "🌙", 96, Color.WHITE, false));
        page.addView(Ui.text(a, "Time's up for today!", 30, Color.WHITE, true));
        TextView sub = Ui.text(a, "You learned for " + Stats.mins(a.stats.total(MainActivity.today())) + " today.\nCome back tomorrow! 😊", 20, 0xFFC5CAE9, false);
        sub.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 24));
        page.addView(sub);
        TextView unlock = Ui.button(a, "👪 Parent: unlock", Color.WHITE, Ui.INK, () -> askPin(a, "Unlock", () ->
                new AlertDialog.Builder(a).setTitle("Give more time")
                        .setItems(new String[]{"15 more minutes", "30 more minutes", "1 more hour", "No limit for the rest of today"}, (d, w) -> {
                            int[] extra = {15, 30, 60, 24 * 60};
                            a.unlock(extra[w]);
                        }).show()));
        page.addView(unlock, Ui.lp(Ui.MATCH, Ui.WRAP, 0));
        return page;
    }

    // ---- videos for the child ----

    static View videos(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFFC62828);
        page.addView(Ui.topBar(a, "My videos"));
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 20);
        box.setPadding(p, 0, p, p);
        List<String[]> links = a.stats.links();
        if (links.isEmpty()) {
            box.addView(Ui.text(a, "📺\nNo videos yet.\n\nAsk a parent to add YouTube links in the Parents area (👪 at the top of the home screen).", 20, Color.WHITE, true));
        }
        for (final String[] l : links) {
            TextView b = Ui.button(a, "▶  " + l[0], Color.WHITE, Ui.INK, () -> a.openVideo(l[1]));
            LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
            lp.topMargin = Ui.dp(a, 12);
            box.addView(b, lp);
        }
        TextView note = Ui.text(a, "Videos open in YouTube and need the internet.", 15, 0xFFFFCDD2, false);
        note.setPadding(0, Ui.dp(a, 16), 0, 0);
        box.addView(note);
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));
        return page;
    }

    // ---- dashboard ----

    private static LinearLayout card(MainActivity a, LinearLayout list, String title) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 18)));
        int p = Ui.dp(a, 16);
        c.setPadding(p, p, p, p);
        TextView h = Ui.text(a, title, 19, Ui.INK, true);
        h.setGravity(Gravity.START);
        c.addView(h);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.topMargin = Ui.dp(a, 12);
        list.addView(c, lp);
        return c;
    }

    private static TextView line(MainActivity a, LinearLayout c, String s, int color) {
        TextView t = Ui.text(a, s, 16, color, false);
        t.setGravity(Gravity.START);
        t.setPadding(0, Ui.dp(a, 4), 0, 0);
        c.addView(t);
        return t;
    }

    private static TextView smallButton(MainActivity a, String label, Runnable r) {
        TextView b = Ui.text(a, label, 16, Color.WHITE, true);
        b.setBackground(Ui.round(COLOR, Ui.dp(a, 12)));
        b.setPadding(Ui.dp(a, 16), Ui.dp(a, 10), Ui.dp(a, 16), Ui.dp(a, 10));
        Ui.click(b, r);
        return b;
    }

    private static void refresh(MainActivity a) {
        a.show(dashboard(a), false, COLOR, MainActivity.NONE);
    }

    static View dashboard(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Parents"));
        final LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 12);
        list.setPadding(p, 0, p, p);
        final int day = MainActivity.today();

        // today
        LinearLayout c = card(a, list, "📅 Today");
        line(a, c, "Time used: " + Stats.mins(a.stats.total(day)), Ui.INK);
        for (int i = 0; i < Stats.CATS.length; i++)
            line(a, c, "●  " + Stats.NAMES[i] + ": " + Stats.mins(a.stats.get(day, Stats.CATS[i])), Stats.COLORS[i]);
        int lim = a.limitMinutes();
        if (lim > 0) {
            long allowed = a.allowedMs(), left = Math.max(0, allowed - a.stats.total(day));
            line(a, c, "Allowed today: " + (allowed / 60000) + " min · " + Stats.mins(left) + " left", Ui.INK);
        }
        line(a, c, "Stars earned so far: ⭐ " + a.stars(), Ui.INK);

        // daily limit
        c = card(a, list, "⏱ Daily time limit");
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final TextView limLabel = Ui.text(a, "", 20, Ui.INK, true);
        final Runnable showLim = () -> {
            int v = a.limitMinutes();
            limLabel.setText(v == 0 ? "No limit" : v + " minutes a day");
        };
        row.addView(smallButton(a, "  −  ", () -> {
            a.prefs.edit().putInt("limit", step(a.limitMinutes(), -1)).apply();
            showLim.run();
        }));
        row.addView(limLabel, Ui.lp(0, Ui.WRAP, 1));
        row.addView(smallButton(a, "  +  ", () -> {
            a.prefs.edit().putInt("limit", step(a.limitMinutes(), 1)).apply();
            showLim.run();
        }));
        showLim.run();
        LinearLayout.LayoutParams rlp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        rlp.topMargin = Ui.dp(a, 8);
        c.addView(row, rlp);
        line(a, c, "When the time is used up the app locks until a parent enters the PIN. Time in the parents area is not counted.", 0xFF78909C);

        // 7 days
        c = card(a, list, "📊 Last 7 days");
        long[][] v = new long[7][Stats.CATS.length];
        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            int d = day - 6 + i;
            for (int k = 0; k < Stats.CATS.length; k++) v[i][k] = a.stats.get(d, Stats.CATS[k]);
            labels[i] = Stats.dayShort(d);
        }
        c.addView(new WeekChart(a, v, labels), Ui.lp(Ui.MATCH, Ui.dp(a, 190), 0));
        StringBuilder legend = new StringBuilder();
        for (int k = 0; k < Stats.CATS.length; k++) legend.append(k == 0 ? "" : "   ").append("■ ").append(Stats.NAMES[k]);
        line(a, c, legend.toString(), 0xFF607D8B);
        for (int i = 6; i >= 0; i--) {
            int d = day - 6 + i;
            long tot = a.stats.total(d);
            StringBuilder s = new StringBuilder(Stats.date(d) + ":  " + Stats.mins(tot));
            if (tot > 0) {
                s.append("  (");
                for (int k = 0; k < Stats.CATS.length; k++)
                    s.append(k == 0 ? "" : ", ").append(Stats.NAMES[k].split(" ")[0].toLowerCase()).append(' ').append(Math.round(v[i][k] / 60000.0));
                s.append(")");
            }
            line(a, c, s.toString(), Ui.INK);
        }

        // scores
        c = card(a, list, "🏆 Scores");
        JSONArray sc = a.stats.scores();
        if (sc.length() == 0) line(a, c, "No quiz scores yet.", 0xFF78909C);
        else {
            Map<String, int[]> agg = new LinkedHashMap<>();
            for (int i = 0; i < sc.length(); i++) {
                JSONObject o = sc.optJSONObject(i);
                if (o == null) continue;
                int[] t = agg.get(o.optString("q"));
                if (t == null) agg.put(o.optString("q"), t = new int[3]);
                t[0] += o.optInt("w");
                t[1] += o.optInt("t");
                t[2]++;
            }
            for (Map.Entry<String, int[]> e : agg.entrySet()) {
                int[] t = e.getValue();
                line(a, c, e.getKey() + ":  " + (t[1] == 0 ? 0 : Math.round(100f * t[0] / t[1])) + "% right  (" + t[2] + (t[2] == 1 ? " quiz)" : " quizzes)"), Ui.INK);
            }
            TextView recent = line(a, c, "Recent", Ui.INK);
            recent.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            recent.setPadding(0, Ui.dp(a, 10), 0, 0);
            for (int i = sc.length() - 1; i >= Math.max(0, sc.length() - 10); i--) {
                JSONObject o = sc.optJSONObject(i);
                if (o == null) continue;
                line(a, c, Stats.date(o.optInt("d")) + " · " + o.optString("q") + " · " + o.optInt("w") + "/" + o.optInt("t"), 0xFF546E7A);
            }
        }

        // YouTube links
        c = card(a, list, "▶️ YouTube links for My videos");
        final List<String[]> links = a.stats.links();
        if (links.isEmpty()) line(a, c, "No links yet.", 0xFF78909C);
        for (int i = 0; i < links.size(); i++) {
            final int k = i;
            LinearLayout lr = new LinearLayout(a);
            lr.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = Ui.text(a, links.get(i)[0] + "\n" + links.get(i)[1], 14, Ui.INK, false);
            t.setGravity(Gravity.START);
            lr.addView(t, Ui.lp(0, Ui.WRAP, 1));
            lr.addView(smallButton(a, "🗑", () -> {
                links.remove(k);
                a.stats.setLinks(links);
                refresh(a);
            }));
            LinearLayout.LayoutParams llp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
            llp.topMargin = Ui.dp(a, 8);
            c.addView(lr, llp);
        }
        final EditText title = new EditText(a);
        title.setHint("Title, e.g. Number songs");
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText url = new EditText(a);
        url.setHint("https://www.youtube.com/watch?v=…");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        c.addView(title);
        c.addView(url);
        c.addView(smallButton(a, "Add link", () -> {
            String t = title.getText().toString().trim(), u = url.getText().toString().trim();
            Uri uri = Uri.parse(u);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean yt = host.equals("youtu.be") || host.equals("youtube.com") || host.endsWith(".youtube.com");
            if (t.isEmpty() || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || !yt) {
                Toast.makeText(a, "Enter a title and a YouTube link (youtube.com or youtu.be)", Toast.LENGTH_LONG).show();
                return;
            }
            links.add(new String[]{t, u});
            a.stats.setLinks(links);
            Toast.makeText(a, "Link added", Toast.LENGTH_SHORT).show();
            refresh(a);
        }));

        // online questions
        c = card(a, list, "🌐 Online questions");
        CheckBox online = new CheckBox(a);
        online.setText("Use online questions in Quiz Time for EVS, General Knowledge and Animals");
        online.setTextSize(16);
        online.setChecked(a.prefs.getBoolean("online", false));
        online.setOnCheckedChangeListener((b, on) -> a.prefs.edit().putBoolean("online", on).apply());
        c.addView(online);
        line(a, c, "Questions come from opentdb.com (in English, easy level) and are not checked by this app. Without internet the built-in questions are used.", 0xFF78909C);

        // class and reading help
        c = card(a, list, "🎓 Class & reading help");
        line(a, c, "Class: " + Grade.NAMES[a.grade()] + ". Harder lessons appear as the class goes up.", Ui.INK);
        c.addView(smallButton(a, "Change class", () -> a.show(Grade.picker(a), false, Grade.COLOR, MainActivity.NONE)));
        CheckBox guide = new CheckBox(a);
        guide.setText("Voice guide: read every button aloud before opening it (for children who can't read yet)");
        guide.setTextSize(16);
        guide.setChecked(a.voiceGuide());
        guide.setOnCheckedChangeListener((b, on) -> a.prefs.edit().putBoolean("voiceGuide", on).putBoolean("voiceGuideSet", true).apply());
        c.addView(guide);

        // voice: type, speed, pitch, installed voices
        c = card(a, list, "🔈 Voice, speed and pitch");
        line(a, c, "Android doesn't say which installed voices are male or female, so these buttons change the pitch and pick a matching voice when the phone names one.", 0xFF78909C);
        LinearLayout vr = new LinearLayout(a);
        vr.setPadding(0, Ui.dp(a, 8), 0, 0);
        vr.addView(smallButton(a, "👩 Female", () -> voiceType(a, true)), Ui.lp(a, 0, Ui.WRAP, 1, 3));
        vr.addView(smallButton(a, "👨 Male", () -> voiceType(a, false)), Ui.lp(a, 0, Ui.WRAP, 1, 3));
        vr.addView(smallButton(a, "🔊 Test", () -> a.speaker.say("Hello! I am your reading friend. Cat, ship, boat.")), Ui.lp(a, 0, Ui.WRAP, 1, 3));
        c.addView(vr);
        final TextView rateLabel = line(a, c, "", Ui.INK);
        android.widget.SeekBar rate = new android.widget.SeekBar(a);
        rate.setMax(100);
        rate.setProgress(Math.round((a.prefs.getFloat("rate", 0.85f) - 0.5f) * 100));
        rateLabel.setText("Speed: " + String.format(Locale.ROOT, "%.2f", a.prefs.getFloat("rate", 0.85f)) + "×");
        rate.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar s, int v, boolean user) {
                float r = 0.5f + v / 100f;
                rateLabel.setText("Speed: " + String.format(Locale.ROOT, "%.2f", r) + "×");
                if (user) {
                    a.prefs.edit().putFloat("rate", r).apply();
                    a.speaker.setRate(r);
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar s) {
                a.speaker.say("This is my speed.");
            }
        });
        c.addView(rate);
        final TextView pitchLabel = line(a, c, "", Ui.INK);
        android.widget.SeekBar pitch = new android.widget.SeekBar(a);
        pitch.setMax(110);
        pitch.setProgress(Math.round((a.prefs.getFloat("pitch", 1.1f) - 0.5f) * 100));
        pitchLabel.setText("Pitch: " + String.format(Locale.ROOT, "%.2f", a.prefs.getFloat("pitch", 1.1f)));
        pitch.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar s, int v, boolean user) {
                float pt = 0.5f + v / 100f;
                pitchLabel.setText("Pitch: " + String.format(Locale.ROOT, "%.2f", pt));
                if (user) {
                    a.prefs.edit().putFloat("pitch", pt).apply();
                    a.speaker.setPitch(pt);
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar s) {
                a.speaker.say("This is my voice.");
            }
        });
        c.addView(pitch);
        LinearLayout pick = new LinearLayout(a);
        pick.setPadding(0, Ui.dp(a, 8), 0, 0);
        final String[][] langs = {{"en", "English"}, {"kn", "Kannada"}, {"hi", "Hindi"}};
        for (final String[] l : langs)
            pick.addView(smallButton(a, l[1] + " voice", () -> chooseVoice(a, l[0], l[1])), Ui.lp(a, 0, Ui.WRAP, 1, 3));
        c.addView(pick);

        // word bank import
        c = card(a, list, "📖 Word bank");
        line(a, c, WordsData.bank(a).size() + " words. Add more from a CSV file with one word per line: English,Kannada,Hindi,picture,category (picture and category are optional).", 0xFF78909C);
        c.addView(smallButton(a, "Import word list (CSV)", () -> importWords(a)));

        // accent
        c = card(a, list, "🗣️ English voice");
        line(a, c, "Use one accent everywhere so your child hears the same sounds every time.", 0xFF78909C);
        LinearLayout ar = new LinearLayout(a);
        ar.setPadding(0, Ui.dp(a, 8), 0, 0);
        final String[][] accents = {{"GB", "British"}, {"US", "American"}, {"IN", "Indian"}};
        String curAcc = a.prefs.getString("accent", "GB");
        for (final String[] acc : accents) {
            TextView b = smallButton(a, (acc[0].equals(curAcc) ? "✓ " : "") + acc[1], () -> {
                a.prefs.edit().putString("accent", acc[0]).apply();
                a.speaker.setAccent(acc[0]);
                a.speaker.say("Hello! This is the " + acc[1] + " English voice. Cat, ship, boat.");
                refresh(a);
            });
            if (!acc[0].equals(curAcc)) b.setAlpha(0.65f);
            ar.addView(b, Ui.lp(a, 0, Ui.WRAP, 1, 3));
        }
        c.addView(ar);
        line(a, c, "If a voice is not installed, the phone's default English voice is used.", 0xFF78909C);

        // ideas for parents
        c = card(a, list, "💡 Ideas for home activities");
        final String[][] ideas = {
                {"UNICEF — parenting and play", "https://www.unicef.org/parenting"},
                {"Oxford Owl — free reading at home", "https://www.oxfordowl.co.uk/for-home/"},
                {"Pinterest — learning activity ideas", "https://www.pinterest.com/search/pins/?q=learning%20activities%20for%20kids"}};
        for (final String[] idea : ideas) {
            TextView b = smallButton(a, "🌐 " + idea[0], () -> a.openLink(idea[1]));
            LinearLayout.LayoutParams ilp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
            ilp.topMargin = Ui.dp(a, 8);
            c.addView(b, ilp);
        }
        line(a, c, "These open outside the app and need the internet.", 0xFF78909C);

        // backup
        c = card(a, list, "💾 Backup");
        line(a, c, "Saves settings, PIN, stars, time history, scores, sentences and links to a file you choose (for example on Google Drive).", 0xFF78909C);
        LinearLayout br = new LinearLayout(a);
        br.setPadding(0, Ui.dp(a, 8), 0, 0);
        br.addView(smallButton(a, "Export backup", () -> exportBackup(a)), Ui.lp(a, 0, Ui.WRAP, 1, 4));
        br.addView(smallButton(a, "Import backup", () -> importBackup(a)), Ui.lp(a, 0, Ui.WRAP, 1, 4));
        c.addView(br);

        // PIN & help
        c = card(a, list, "🔑 PIN and help");
        c.addView(smallButton(a, "Change PIN", () -> askPin(a, "Change PIN", () -> createPin(a, () -> refresh(a)))));
        line(a, c, "Kannada and Hindi audio uses the phone's text-to-speech voices. If a voice is missing, open the phone Settings › Accessibility (or System) › Text-to-speech › Google › Install voice data, and add Kannada and Hindi.", 0xFF78909C);

        ScrollView sv = new ScrollView(a);
        sv.addView(list);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));
        return page;
    }

    /** Female: higher pitch; male: lower pitch. Also picks a voice whose name says so, if the phone has one. */
    private static void voiceType(MainActivity a, boolean female) {
        float pt = female ? 1.25f : 0.8f;
        a.prefs.edit().putFloat("pitch", pt).apply();
        a.speaker.setPitch(pt);
        int found = 0;
        for (String lang : new String[]{"en", "kn", "hi"}) {
            for (String name : a.speaker.voices(lang)) {
                String n = name.toLowerCase(Locale.ROOT);
                boolean isFemale = n.contains("female"), isMale = n.contains("male") && !isFemale;
                if (female ? isFemale : isMale) {
                    a.prefs.edit().putString("voice_" + lang, name).apply();
                    a.speaker.setVoice(lang, name);
                    found++;
                    break;
                }
            }
        }
        a.speaker.say(female ? "Hello! This is the female voice." : "Hello! This is the male voice.");
        Toast.makeText(a, found > 0 ? "Voice and pitch changed" : "Pitch changed. Use the voice buttons below to try the other installed voices.", Toast.LENGTH_LONG).show();
        refresh(a);
    }

    private static void chooseVoice(final MainActivity a, final String lang, String name) {
        final java.util.List<String> voices = a.speaker.voices(lang);
        if (voices.isEmpty()) {
            Toast.makeText(a, "No " + name + " voices are installed. See Settings › Text-to-speech on the phone.", Toast.LENGTH_LONG).show();
            return;
        }
        final String[] items = new String[voices.size() + 1];
        items[0] = "Phone default";
        for (int i = 0; i < voices.size(); i++) items[i + 1] = "Voice " + (i + 1) + "  (" + voices.get(i) + ")";
        new AlertDialog.Builder(a).setTitle(name + " voice").setItems(items, (d, w) -> {
            String v = w == 0 ? null : voices.get(w - 1);
            if (v == null) a.prefs.edit().remove("voice_" + lang).apply();
            else a.prefs.edit().putString("voice_" + lang, v).apply();
            a.speaker.setVoice(lang, v);
            a.speaker.say(lang.equals("kn") ? "ನಮಸ್ಕಾರ" : lang.equals("hi") ? "नमस्ते" : "Hello! This is my voice.", lang);
        }).show();
    }

    private static void importWords(final MainActivity a) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream"});
        a.forResult(i, "No file app found on this phone", (code, data) -> {
            if (code != Activity.RESULT_OK || data == null || data.getData() == null) return;
            StringBuilder keep = new StringBuilder();
            int good = 0, bad = 0;
            try (InputStream in = a.getContentResolver().openInputStream(data.getData())) {
                if (in == null) throw new IllegalStateException("no stream");
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                String lineText;
                int lines = 0;
                while ((lineText = r.readLine()) != null && lines++ < 20000) {
                    if (lineText.trim().isEmpty()) continue;
                    if (WordsData.parse(lineText) != null) {
                        keep.append(lineText.replace("﻿", "")).append('\n');
                        good++;
                    } else bad++;
                }
            } catch (Exception e) {
                Toast.makeText(a, "The file could not be read", Toast.LENGTH_LONG).show();
                return;
            }
            if (good == 0) {
                Toast.makeText(a, "No words found. Each line needs: English,Kannada,Hindi", Toast.LENGTH_LONG).show();
                return;
            }
            try (OutputStream os = new java.io.FileOutputStream(new java.io.File(a.getFilesDir(), "words.csv"), true)) {
                os.write(keep.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                Toast.makeText(a, "The words could not be saved", Toast.LENGTH_LONG).show();
                return;
            }
            int before = WordsData.bank(a).size();
            WordsData.reset();
            int added = WordsData.bank(a).size() - before;
            Toast.makeText(a, "Added " + added + " new words" + (bad > 1 ? " (" + (bad - 1) + " lines skipped)" : ""), Toast.LENGTH_LONG).show();
            refresh(a);
        });
    }

    private static int step(int current, int dir) {
        int i = 0;
        while (i < LIMITS.length - 1 && LIMITS[i] < current) i++;
        return LIMITS[Math.max(0, Math.min(LIMITS.length - 1, i + dir))];
    }

    // ---- backup files ----

    private static void exportBackup(final MainActivity a) {
        final String json;
        try {
            JSONObject o = new JSONObject();
            o.put("app", "KidsLearnApp");
            o.put("version", 1);
            o.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date()));
            o.put("settings", Stats.dump(a.prefs));
            o.put("progress", Stats.dump(a.stats.sp));
            json = o.toString(2);
        } catch (Exception e) {
            Toast.makeText(a, "Could not prepare the backup", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "KidsLearnApp-backup-" + new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date()) + ".json");
        a.forResult(i, "No file app found on this phone", (code, data) -> {
            if (code != Activity.RESULT_OK || data == null || data.getData() == null) return;
            try (OutputStream os = a.getContentResolver().openOutputStream(data.getData(), "wt")) {
                if (os == null) throw new IllegalStateException("no stream");
                os.write(json.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(a, "Backup saved", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(a, "Could not save the backup", Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void importBackup(final MainActivity a) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
        a.forResult(i, "No file app found on this phone", (code, data) -> {
            if (code != Activity.RESULT_OK || data == null || data.getData() == null) return;
            final JSONObject o;
            try (InputStream in = a.getContentResolver().openInputStream(data.getData())) {
                if (in == null) throw new IllegalStateException("no stream");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                    if (out.size() > 4 * 1024 * 1024) throw new IllegalStateException("too big");
                }
                o = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
                if (!"KidsLearnApp".equals(o.optString("app")) || o.optJSONObject("settings") == null || o.optJSONObject("progress") == null)
                    throw new IllegalStateException("not ours");
            } catch (Exception e) {
                Toast.makeText(a, "This is not a KidsLearnApp backup file", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(a).setTitle("Import backup?")
                    .setMessage("Replace the current settings and progress with the backup from " + o.optString("created", "an earlier date")
                            + "?\n\nThe PIN saved in the backup will be used from now on.")
                    .setPositiveButton("Import", (d, w) -> {
                        try {
                            Stats.restore(a.prefs, o.getJSONObject("settings"));
                            Stats.restore(a.stats.sp, o.getJSONObject("progress"));
                            a.speaker.on = a.prefs.getBoolean("sound", true);
                            Toast.makeText(a, "Backup imported", Toast.LENGTH_LONG).show();
                            refresh(a);
                        } catch (Exception e) {
                            Toast.makeText(a, "The backup could not be read", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancel", null).show();
        });
    }
}

/** Stacked bars: minutes per day, coloured by category. */
final class WeekChart extends View {
    private final long[][] v;
    private final String[] labels;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    WeekChart(Context c, long[][] v, String[] labels) {
        super(c);
        this.v = v;
        this.labels = labels;
        p.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), top = Ui.dp(getContext(), 18), bottom = h - Ui.dp(getContext(), 22);
        long max = 10 * 60000L;
        for (long[] d : v) {
            long t = 0;
            for (long x : d) t += x;
            max = Math.max(max, t);
        }
        float slot = w / v.length, bw = slot * 0.55f;
        p.setTextSize(Ui.dp(getContext(), 12));
        for (int i = 0; i < v.length; i++) {
            float cx = slot * (i + 0.5f), y = bottom;
            long tot = 0;
            for (int k = 0; k < v[i].length; k++) {
                float bh = (bottom - top) * v[i][k] / (float) max;
                if (bh <= 0) continue;
                p.setColor(Stats.COLORS[k]);
                r.set(cx - bw / 2, y - bh, cx + bw / 2, y);
                c.drawRect(r, p);
                y -= bh;
                tot += v[i][k];
            }
            p.setColor(0xFF37474F);
            c.drawText(Math.round(tot / 60000.0) + "m", cx, y - Ui.dp(getContext(), 4), p);
            p.setColor(0xFF78909C);
            c.drawText(labels[i], cx, h - Ui.dp(getContext(), 6), p);
        }
        p.setColor(0xFFCFD8DC);
        c.drawRect(0, bottom, w, bottom + Ui.dp(getContext(), 1), p);
    }
}
