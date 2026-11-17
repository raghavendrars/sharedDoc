package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Scavenger hunts: a picture checklist of things to find at home or outside. Tap a card when you find it. */
final class HuntScreen {
    static final int COLOR = 0xFF2E7D32;

    /** First row: title, icon, tip for grown-ups. Then: picture, what to find. */
    private static final String[][][] HUNTS = {
            {{"Around the house", "🏠", "Walk around the house together and read each card aloud."},
                    {"🥄", "a spoon"}, {"🧦", "a sock"}, {"🧸", "something soft"}, {"📕", "a book"},
                    {"⚪", "something round"}, {"🔑", "a key"}, {"🔔", "something that makes a sound"}, {"☕", "a cup"}},
            {{"Nature walk", "🌳", "Go with a grown-up. Look at bugs, but don't touch them. Wash hands afterwards."},
                    {"🍃", "a leaf"}, {"🪨", "a smooth rock"}, {"🌸", "a flower"}, {"🪶", "a feather"},
                    {"🪵", "a stick"}, {"🟢", "something green"}, {"🐜", "an ant (just look!)"}, {"☁️", "a cloud with a funny shape"}},
            {{"Colour hunt", "🌈", "Find one thing of each colour. Say the colour out loud!"},
                    {"🔴", "something red"}, {"🟠", "something orange"}, {"🟡", "something yellow"}, {"🟢", "something green"},
                    {"🔵", "something blue"}, {"🟣", "something purple"}, {"⚪", "something white"}, {"⚫", "something black"}},
            {{"Shape hunt", "🔺", "Look at windows, plates, clocks, boxes and toys."},
                    {"⚫", "something round like a circle"}, {"🟦", "something square"}, {"🔺", "something with a triangle"},
                    {"▬", "something shaped like a rectangle"}, {"⭐", "something with a star"}, {"❤️", "something with a heart"}},
            {{"Letter hunt", "🔤", "Say the first sound of each thing you find: b-b-ball!"},
                    {"🅱️", "something that starts with b (buh)"}, {"🐱", "something that starts with c (kuh)"},
                    {"☀️", "something that starts with s (sss)"}, {"🐷", "something that starts with p (puh)"},
                    {"🌳", "something that starts with t (tuh)"}, {"🌙", "something that starts with m (muh)"}},
    };

    private HuntScreen() {
    }

    static Runnable shelf(MainActivity a) {
        String[] labels = new String[HUNTS.length];
        Runnable[] go = new Runnable[HUNTS.length];
        for (int i = 0; i < HUNTS.length; i++) {
            final int k = i;
            labels[i] = HUNTS[i][0][1] + "  " + HUNTS[i][0][0];
            go[i] = () -> a.show(build(a, HUNTS[k]), false, COLOR, MainActivity.LEARN);
        }
        return HomeScreen.menu(a, "Scavenger hunts", COLOR, labels, go);
    }

    private static View build(final MainActivity a, final String[][] hunt) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, hunt[0][1] + " " + hunt[0][0]));
        final int total = hunt.length - 1;
        final boolean[] found = new boolean[total];
        final TextView info = Ui.text(a, "Found 0 of " + total, 20, Color.WHITE, true);
        page.addView(info);
        TextView tip = Ui.text(a, "👪 " + hunt[0][2], 15, 0xFFC8E6C9, false);
        tip.setPadding(Ui.dp(a, 16), Ui.dp(a, 2), Ui.dp(a, 16), Ui.dp(a, 4));
        page.addView(tip);

        LinearLayout grid = new LinearLayout(a);
        grid.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 10);
        grid.setPadding(p, 0, p, p);
        final boolean[] done = {false};
        LinearLayout row = null;
        for (int i = 0; i < total; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(a);
                grid.addView(row, Ui.lp(Ui.MATCH, Ui.dp(a, 150), 0));
            }
            final int k = i;
            final String[] item = hunt[i + 1];
            final LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackground(Ui.tile(a, Color.WHITE, 20));
            card.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 6));
            final TextView pic = Ui.text(a, item[0], 44, Ui.INK, false);
            final TextView label = Ui.text(a, "Find " + item[1], 16, Ui.INK, true);
            card.addView(pic);
            card.addView(label);
            Ui.click(card, () -> {
                found[k] = !found[k];
                card.setBackground(Ui.tile(a, found[k] ? 0xFFA5D6A7 : Color.WHITE, 20));
                pic.setText(found[k] ? "✅" : item[0]);
                int n = 0;
                for (boolean f : found) if (f) n++;
                info.setText("Found " + n + " of " + total);
                if (found[k]) {
                    a.speaker.good();
                    a.speaker.say("You found " + item[1].replace("(just look!)", "") + "!");
                }
                if (n == total && !done[0]) {
                    done[0] = true;
                    info.setText("🎉 You found everything!  +3 ⭐");
                    a.addStars(3);
                    a.confetti();
                    a.speaker.say("Amazing! You found everything!");
                }
            });
            row.addView(card, Ui.lp(a, 0, Ui.MATCH, 1, 5));
            Ui.popIn(card, 40L * i);
        }
        if (total % 2 == 1) row.addView(new View(a), Ui.lp(a, 0, Ui.MATCH, 1, 5));
        ScrollView sv = new ScrollView(a);
        sv.addView(grid);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));

        TextView read = Ui.button(a, "🔊 Read the list", Color.WHITE, Ui.INK, () -> {
            StringBuilder b = new StringBuilder("Can you find ");
            for (int i = 1; i <= total; i++) b.append(hunt[i][1].replace("(just look!)", "")).append(i < total ? ", " : "?");
            a.speaker.say(b.toString());
        });
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.setMargins(Ui.dp(a, 20), Ui.dp(a, 4), Ui.dp(a, 20), Ui.dp(a, 14));
        page.addView(read, lp);
        return page;
    }
}
