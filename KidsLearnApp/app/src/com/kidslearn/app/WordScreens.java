package com.kidslearn.app;

import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Two-letter words, the three-language word bank and tenses. */
final class WordScreens {
    static final int COLOR = 0xFF5D4037;
    private static final String[] LANGS = {"en", "kn", "hi"};
    private static final String[] LANG_NAMES = {"English", "ಕನ್ನಡ", "हिंदी"};

    private WordScreens() {
    }

    private static LinearLayout page(MainActivity a, String title) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, title));
        return page;
    }

    private static TextView chip(MainActivity a, String label, boolean on, Runnable r) {
        TextView c = Ui.text(a, label, 16, Ui.INK, true);
        c.setBackground(Ui.round(on ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 16)));
        c.setPadding(Ui.dp(a, 14), Ui.dp(a, 6), Ui.dp(a, 14), Ui.dp(a, 6));
        Ui.click(c, r);
        return c;
    }

    private static String langWord(String lang) {
        return lang.equals("kn") ? "Kannada" : lang.equals("hi") ? "Hindi" : "English";
    }

    // ---- two-letter words ----

    static Runnable two(final MainActivity a, final String lang) {
        return () -> a.show(buildTwo(a, lang), false, COLOR, MainActivity.AUDIO);
    }

    private static View buildTwo(final MainActivity a, final String lang) {
        LinearLayout page = page(a, lang.equals("kn") ? "ಎರಡು ಅಕ್ಷರದ ಪದಗಳು" : lang.equals("hi") ? "दो अक्षर वाले शब्द" : "2-letter words");
        LinearLayout langs = new LinearLayout(a);
        langs.setGravity(Gravity.CENTER);
        for (int i = 0; i < LANGS.length; i++) {
            final String l = LANGS[i];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            lp.setMargins(Ui.dp(a, 4), 0, Ui.dp(a, 4), Ui.dp(a, 6));
            langs.addView(chip(a, LANG_NAMES[i], l.equals(lang), () -> a.show(buildTwo(a, l), false, COLOR, MainActivity.AUDIO)), lp);
        }
        page.addView(langs);
        final TextView info = Ui.text(a, "Tap a word to hear it", 16, Color.WHITE, true);
        page.addView(info);
        FlowLayout flow = new FlowLayout(a);
        int p = Ui.dp(a, 12);
        flow.setPadding(p, p, p, p);
        final String[] words = WordsData.two(lang);
        final List<String> all = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            final String[] w = words[i].split("\\|", -1);
            all.add(w[0]);
            LinearLayout t = new LinearLayout(a);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            t.setBackground(Ui.tile(a, Color.WHITE, 16));
            t.setPadding(Ui.dp(a, 14), Ui.dp(a, 6), Ui.dp(a, 14), Ui.dp(a, 12));
            t.setMinimumWidth(Ui.dp(a, 92));
            if (!w[2].isEmpty()) t.addView(Ui.text(a, w[2], 26, Ui.INK, false));
            t.addView(Ui.text(a, w[0], 28, Data.BRIGHT[i % Data.BRIGHT.length], true));
            t.addView(Ui.text(a, w[1], 13, 0xFF78909C, false));
            Ui.click(t, () -> {
                info.setText(w[0] + "  —  " + w[1]);
                String say = lang.equals("en") ? w[0] + ". " + w[1] : w[0];
                if (!a.speaker.say(say, lang)) info.setText(w[0] + "  —  " + w[1] + "   (no " + langWord(lang) + " voice)");
            });
            flow.addView(t);
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(flow);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));
        TextView readAll = Ui.button(a, "▶ Read them all", Color.WHITE, Ui.INK, () -> {
            if (!a.speaker.sayLines(all, lang, 500)) info.setText("No " + langWord(lang) + " voice on this phone");
        });
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.setMargins(Ui.dp(a, 20), Ui.dp(a, 4), Ui.dp(a, 20), Ui.dp(a, 14));
        page.addView(readAll, lp);
        return page;
    }

    // ---- word bank ----

    static Runnable bank(final MainActivity a) {
        return () -> a.show(buildBank(a), false, COLOR, MainActivity.AUDIO);
    }

    private static View buildBank(final MainActivity a) {
        final List<WordsData.Word> all = WordsData.bank(a);
        LinearLayout page = page(a, "Word bank");
        final TextView info = Ui.text(a, "", 15, Color.WHITE, true);
        page.addView(info);

        final EditText search = new EditText(a);
        search.setHint("🔍 Search in English, ಕನ್ನಡ or हिंदी");
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setSingleLine(true);
        search.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        int p = Ui.dp(a, 12);
        search.setPadding(p, Ui.dp(a, 8), p, Ui.dp(a, 8));
        LinearLayout.LayoutParams slp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        slp.setMargins(Ui.dp(a, 12), Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 4));
        page.addView(search, slp);

        final String[] cat = {null};
        final List<WordsData.Word> shown = new ArrayList<>();
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return shown.size();
            }

            @Override
            public Object getItem(int i) {
                return shown.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View convert, ViewGroup parent) {
                final WordsData.Word w = shown.get(i);
                LinearLayout row = new LinearLayout(a);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10), Ui.dp(a, 8));
                row.addView(Ui.text(a, w.pic.isEmpty() ? "•" : w.pic, 30, Ui.INK, false), new LinearLayout.LayoutParams(Ui.dp(a, 52), Ui.WRAP));
                String[] texts = {w.en, w.kn, w.hi};
                for (int k = 0; k < 3; k++) {
                    final String lang = LANGS[k], word = texts[k];
                    TextView t = Ui.text(a, word, k == 0 ? 18 : 20, k == 0 ? Ui.INK : 0xFF4E342E, k == 0);
                    t.setPadding(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
                    t.setBackground(Ui.round(k == 0 ? 0xFFE3F2FD : k == 1 ? 0xFFFCE4EC : 0xFFFFF3E0, Ui.dp(a, 10)));
                    t.setOnClickListener(v -> {
                        Ui.bounce(v);
                        if (!a.speaker.say(word, lang)) info.setText("No " + langWord(lang) + " voice on this phone");
                    });
                    row.addView(t, Ui.lp(a, 0, Ui.WRAP, 1, 3));
                }
                return row;
            }
        };
        final Runnable filter = () -> {
            String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            shown.clear();
            for (WordsData.Word w : all) {
                if (cat[0] != null && !cat[0].equals(w.cat)) continue;
                if (!q.isEmpty() && !w.en.toLowerCase(Locale.ROOT).contains(q) && !w.kn.contains(q) && !w.hi.contains(q)) continue;
                shown.add(w);
            }
            adapter.notifyDataSetChanged();
            info.setText(shown.size() + " of " + all.size() + " words  ·  tap a word to hear it");
        };

        final LinearLayout cats = new LinearLayout(a);
        cats.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 4));
        final List<String> names = new ArrayList<>();
        names.add("All");
        names.addAll(WordsData.categories(a));
        final List<TextView> chips = new ArrayList<>();
        for (final String n : names) {
            final TextView c = Ui.text(a, n, 15, Ui.INK, true);
            c.setPadding(Ui.dp(a, 12), Ui.dp(a, 5), Ui.dp(a, 12), Ui.dp(a, 5));
            Ui.click(c, () -> {
                cat[0] = n.equals("All") ? null : n;
                for (TextView x : chips) x.setBackground(Ui.round(x == c ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 14)));
                filter.run();
            });
            c.setBackground(Ui.round(n.equals("All") ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 14)));
            chips.add(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            lp.setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), 0);
            cats.addView(c, lp);
        }
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(cats);
        page.addView(hs);

        ListView list = new ListView(a);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 20)));
        LinearLayout.LayoutParams llp = Ui.lp(Ui.MATCH, 0, 1);
        llp.setMargins(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 12));
        page.addView(list, llp);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                filter.run();
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
        filter.run();
        return page;
    }

    // ---- tenses ----

    static Runnable tenses(final MainActivity a, final String lang) {
        return () -> a.show(buildTenses(a, lang), false, COLOR, MainActivity.LEARN);
    }

    private static View buildTenses(final MainActivity a, final String lang) {
        LinearLayout page = page(a, lang.equals("kn") ? "ಕಾಲಗಳು  Tenses" : lang.equals("hi") ? "काल  Tenses" : "Tenses");
        LinearLayout langs = new LinearLayout(a);
        langs.setGravity(Gravity.CENTER);
        for (int i = 0; i < LANGS.length; i++) {
            final String l = LANGS[i];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            lp.setMargins(Ui.dp(a, 4), 0, Ui.dp(a, 4), Ui.dp(a, 6));
            langs.addView(chip(a, LANG_NAMES[i], l.equals(lang), () -> a.show(buildTenses(a, l), false, COLOR, MainActivity.LEARN)), lp);
        }
        page.addView(langs);
        final TextView info = Ui.text(a, "Past = yesterday · Present = today · Future = tomorrow", 15, Color.WHITE, true);
        info.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 12), Ui.dp(a, 4));
        page.addView(info);

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 12);
        list.setPadding(p, 0, p, p);
        if (lang.equals("hi")) list.addView(note(a, "Girls say: मैं खाती हूँ, मैं गई, मैं खाऊँगी (ending in ी)."));
        if (lang.equals("kn")) list.addView(note(a, "In everyday Kannada, ತಿನ್ನುತ್ತೇನೆ is used for both today and tomorrow; ತಿನ್ನುವೆನು is the book form of the future."));
        int[] order = {1, 0, 2};
        int[] colors = {0xFFE1F5FE, 0xFFE8F5E9, 0xFFFFF3E0};
        String[] icons = {"⏪", "▶️", "⏩"};
        for (final String[] v : WordsData.TENSES) {
            LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 18)));
            card.setPadding(p, p, p, p);
            TextView head = Ui.text(a, v[0] + "  " + WordsData.verbName(v, lang) + (lang.equals("en") ? "" : "   (" + v[1] + ")"), 21, Ui.INK, true);
            head.setGravity(Gravity.START);
            card.addView(head);
            for (int k = 0; k < 3; k++) {
                final int t = order[k];
                final String s = WordsData.tense(v, lang, t);
                TextView row = Ui.text(a, icons[t == 1 ? 0 : t == 0 ? 1 : 2] + "  " + WordsData.TENSE_NAMES[t] + " · " + WordsData.when(lang, t) + "\n" + s,
                        19, Ui.INK, false);
                row.setGravity(Gravity.START);
                row.setBackground(Ui.round(colors[k], Ui.dp(a, 12)));
                row.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8));
                Ui.click(row, () -> {
                    if (!a.speaker.say(s, lang)) info.setText("No " + langWord(lang) + " voice on this phone");
                });
                LinearLayout.LayoutParams rlp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
                rlp.topMargin = Ui.dp(a, 6);
                card.addView(row, rlp);
            }
            LinearLayout.LayoutParams clp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
            clp.topMargin = Ui.dp(a, 10);
            list.addView(card, clp);
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(list);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));
        int mode = lang.equals("kn") ? QuizScreen.TENSE_KN : lang.equals("hi") ? QuizScreen.TENSE_HI : QuizScreen.TENSE_EN;
        TextView quiz = Ui.button(a, "🏆 Tense quiz", 0xFFFFD54F, Ui.INK, QuizScreen.open(a, mode));
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.setMargins(Ui.dp(a, 20), Ui.dp(a, 4), Ui.dp(a, 20), Ui.dp(a, 14));
        page.addView(quiz, lp);
        return page;
    }

    private static TextView note(MainActivity a, String s) {
        TextView t = Ui.text(a, "ℹ️ " + s, 15, Ui.INK, false);
        t.setBackground(Ui.round(0xFFFFF59D, Ui.dp(a, 12)));
        t.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8));
        return t;
    }
}
