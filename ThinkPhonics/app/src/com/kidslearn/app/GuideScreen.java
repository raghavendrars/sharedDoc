package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Step-by-step guides: home science experiments (ask, guess, test, learn) and art & craft projects. */
final class GuideScreen {
    static final int SCIENCE = 0xFF0277BD, CRAFT = 0xFFE65100;

    static final class Guide {
        final String icon, title, need, safety, question, result;
        final String[] options;
        final String[][] steps;

        Guide(String icon, String title, String need, String safety, String question, String[] options, String[][] steps, String result) {
            this.icon = icon;
            this.title = title;
            this.need = need;
            this.safety = safety;
            this.question = question;
            this.options = options;
            this.steps = steps;
            this.result = result;
        }
    }

    static final Guide[] EXPERIMENTS = {
            new Guide("🧊", "Melting ice race", "2 ice cubes, 2 plates, salt",
                    "Don't put salt in your mouth or eyes.",
                    "Which ice cube will melt faster?", new String[]{"The one with salt", "The one without salt", "Both the same"},
                    new String[][]{{"🍽️", "Put one ice cube on each plate."}, {"🧂", "Sprinkle a spoon of salt on one ice cube."},
                            {"⏱️", "Watch them for 10 minutes."}, {"👀", "Which ice cube is smaller now?"}},
                    "The salty ice melts faster! Salt makes it harder for water to stay frozen, so the ice turns into water sooner. "
                            + "That is why people put salt on icy roads in cold countries."),
            new Guide("🛁", "Sink or float", "a tub of water, a coin, a leaf, a spoon, a plastic ball, an apple, a key",
                    "Mop up spills so nobody slips.",
                    "Will an apple sink or float?", new String[]{"Sink", "Float"},
                    new String[][]{{"🛁", "Fill the tub with water."}, {"🤔", "Hold up each thing and guess: sink or float?"},
                            {"💧", "Drop them in one at a time."}, {"📋", "Make two groups: floaters and sinkers."}},
                    "Things that are light for their size, like the apple, leaf and ball, float. The coin, key and spoon are heavy for "
                            + "their size, so they sink. An apple floats because it has lots of air inside!"),
            new Guide("🥚", "The bouncy egg", "1 raw egg, a glass, white vinegar, a tray",
                    "Do this with a grown-up. Wash your hands after touching raw egg. Don't eat the egg.",
                    "What will vinegar do to the egg shell?", new String[]{"Make it disappear", "Turn it blue", "Nothing"},
                    new String[][]{{"🥚", "Gently put the egg in the glass."}, {"🫗", "Pour in vinegar until the egg is covered."},
                            {"🫧", "Look closely: tiny bubbles appear on the shell!"}, {"📅", "Leave it for 2 days."},
                            {"🚰", "Lift it out and rinse it gently. The shell rubs away."},
                            {"🏀", "Hold it just above the tray and drop it. Boing!"}},
                    "The vinegar dissolved the hard shell. The bubbles were a gas called carbon dioxide. A soft, bendy skin is left, "
                            + "so the egg can bounce a little. Drop it from too high and it will still break!"),
            new Guide("🌈", "Walking water", "3 clear glasses, water, red and blue food colour, 2 paper towels",
                    "Food colour can stain clothes — wear old ones.",
                    "What will happen to the empty middle glass?", new String[]{"It fills with purple water", "It stays empty", "It fills with red water"},
                    new String[][]{{"🥛", "Put the 3 glasses in a row."}, {"💧", "Fill the first and last glass with water. Leave the middle one empty."},
                            {"🎨", "Add red colour to one full glass and blue to the other."},
                            {"🧻", "Fold each paper towel into a long strip."},
                            {"🌉", "Put one strip from the red glass into the middle glass, and one from the blue glass into the middle glass."},
                            {"⏳", "Wait about one hour and watch."}},
                    "Water climbs up the tiny gaps in the paper towel and walks into the middle glass. Red and blue mix to make purple! "
                            + "Plants drink water up their stems in a similar way."),
            new Guide("🍋", "Lemon battery", "4 lemons, 4 zinc-coated (galvanised) nails, 4 copper coins or pieces of copper wire, 5 wires with clips, 1 small red LED",
                    "Only with a grown-up. Never use wall sockets or big batteries. Throw the lemons away afterwards — don't eat them.",
                    "Can lemons make a light glow?", new String[]{"Yes", "No"},
                    new String[][]{{"🍋", "Roll each lemon on the table to make it juicy."},
                            {"📌", "Push a nail and a copper piece into each lemon, a little apart."},
                            {"🔗", "Join the copper of one lemon to the nail of the next lemon with a wire. Do this until all 4 are in a chain."},
                            {"💡", "Clip the free copper to the LED's long leg and the free nail to the short leg."},
                            {"🌑", "Turn off the room lights and look closely at the LED."}},
                    "Yes! Lemon juice, zinc and copper together make a tiny bit of electricity. One lemon is too weak, but four "
                            + "lemons in a chain can make a small LED glow. If it doesn't light, check the LED legs are the right way round."),
    };

    static final Guide[] CRAFTS = {
            new Guide("⛵", "Paper boat", "one rectangle sheet of paper", null, null, null,
                    new String[][]{{"📄", "Fold the paper in half, top to bottom."},
                            {"🔺", "Fold the two top corners down to the middle to make a triangle."},
                            {"⬆️", "Fold the bottom strip up on the front. Turn it over and fold the other strip up."},
                            {"🔷", "Open the middle from the bottom and press it flat into a square."},
                            {"🔺", "Fold the bottom corner up on each side to make a triangle again."},
                            {"🔷", "Open the middle once more and press it flat into a square."},
                            {"⛵", "Gently pull the two top corners apart. Your boat is ready!"}},
                    "Float your boat in a tub of water. Can it carry a coin?"),
            new Guide("🚗", "Painting with toy cars", "old toy cars, washable paint on plates, big paper, tape",
                    "Use washable paint and cover the table.", null, null,
                    new String[][]{{"📄", "Tape the big paper to the table or floor."}, {"🎨", "Pour a little paint onto each plate."},
                            {"🚗", "Roll the car's wheels through the paint."}, {"〰️", "Drive the car across the paper. Vroom!"},
                            {"🌈", "Try other cars and colours. Do the tracks look different?"}, {"🧼", "Wash the cars with soap and water."}},
                    "Let your track painting dry and give it a name."),
            new Guide("🦁", "Paper plate lion", "a paper plate, yellow crayon, orange paper, glue, a black pen",
                    "Ask a grown-up to help with scissors.", null, null,
                    new String[][]{{"🟡", "Colour the paper plate yellow."}, {"✂️", "Cut orange paper into long strips."},
                            {"🧴", "Glue the strips around the edge of the plate. This is the mane."},
                            {"✏️", "Draw two eyes, a nose, a smile and whiskers."}},
                    "Roar! Hold your lion up and tell a lion story."),
            new Guide("🌳", "Handprint tree", "paper, brown, green and red paint",
                    "Use washable paint. Wash your hands afterwards.", null, null,
                    new String[][]{{"🖐️", "Paint your hand and wrist brown."},
                            {"📄", "Press them on the paper. The wrist is the trunk and the fingers are the branches."},
                            {"🧼", "Wash your hand."}, {"🟢", "Dip a fingertip in green paint and dot leaves on the branches."},
                            {"🔴", "Add red dots for fruits."}},
                    "Write your name and the date on your tree."),
            new Guide("🍂", "Leaf art", "fallen leaves, a heavy book, paper, glue, a pen", null, null, null,
                    new String[][]{{"🍂", "Collect fallen leaves of different shapes and colours."},
                            {"📚", "Press them flat inside a heavy book for one day."},
                            {"🧩", "Arrange the leaves on paper to make an animal or a person."},
                            {"🧴", "Stick each leaf down with a little glue."}, {"✏️", "Draw eyes, legs and other details."}},
                    "Give your leaf picture a name!"),
            new Guide("🔗", "Paper chain", "coloured paper, scissors, glue or tape",
                    "Ask a grown-up to help with scissors.", null, null,
                    new String[][]{{"✂️", "Cut the paper into long strips."}, {"⭕", "Bend one strip into a ring and stick the ends."},
                            {"🔗", "Put the next strip through the ring, then stick its ends too."},
                            {"🌈", "Keep adding rings in different colours."}},
                    "Hang your chain up to decorate for a festival!"),
    };

    private final MainActivity a;
    private final Guide g;
    private final int color;
    private final boolean science;
    private int page;
    private String guess;
    private boolean starGiven;
    private LinearLayout inner;
    private ScrollView scroll;
    private TextView stage, next;

    private GuideScreen(MainActivity a, Guide g, int color) {
        this.a = a;
        this.g = g;
        this.color = color;
        this.science = g.question != null;
    }

    static Runnable shelf(final MainActivity a, final boolean science) {
        final Guide[] list = science ? EXPERIMENTS : CRAFTS;
        final int color = science ? SCIENCE : CRAFT;
        String[] labels = new String[list.length];
        Runnable[] go = new Runnable[list.length];
        for (int i = 0; i < list.length; i++) {
            final Guide g = list[i];
            labels[i] = g.icon + "  " + g.title;
            go[i] = () -> a.show(new GuideScreen(a, g, color).make(), false, color, MainActivity.LEARN);
        }
        return HomeScreen.menu(a, science ? "Science lab" : "Crafts", color, labels, go);
    }

    private int pages() {
        return 1 + (science ? 1 : 0) + g.steps.length + 1;
    }

    private View make() {
        LinearLayout p = new LinearLayout(a);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(color);
        p.addView(Ui.topBar(a, g.icon + " " + g.title));
        stage = Ui.text(a, "", 16, Color.WHITE, true);
        p.addView(stage);
        scroll = new ScrollView(a);
        scroll.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 26)));
        inner = new LinearLayout(a);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(a, 20);
        inner.setPadding(pad, pad, pad, pad);
        scroll.addView(inner);
        LinearLayout.LayoutParams slp = Ui.lp(Ui.MATCH, 0, 1);
        slp.setMargins(Ui.dp(a, 14), Ui.dp(a, 6), Ui.dp(a, 14), Ui.dp(a, 8));
        p.addView(scroll, slp);
        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 16));
        TextView prev = Ui.circleButton(a, "◀", 24, () -> go(page - 1));
        TextView speak = Ui.circleButton(a, "🔊", 24, this::speak);
        next = Ui.circleButton(a, "▶", 24, () -> go(page + 1));
        for (TextView b : new TextView[]{prev, speak, next}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 64), Ui.dp(a, 64));
            lp.setMargins(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);
            nav.addView(b, lp);
        }
        p.addView(nav);
        fill();
        return p;
    }

    private void go(int n) {
        if (n < 0 || n >= pages()) return;
        if (science && page == 1 && n > page && guess == null) {
            a.speaker.say("Make a guess first!");
            Ui.shake(inner);
            return;
        }
        page = n;
        fill();
    }

    private TextView add(String s, float sp, int color, boolean bold) {
        TextView t = Ui.text(a, s, sp, color, bold);
        t.setLineSpacing(0, 1.2f);
        t.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        inner.addView(t);
        return t;
    }

    private void box(String s, int bg) {
        TextView t = Ui.text(a, s, 18, Ui.INK, true);
        t.setBackground(Ui.round(bg, Ui.dp(a, 14)));
        int p = Ui.dp(a, 12);
        t.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.topMargin = Ui.dp(a, 10);
        inner.addView(t, lp);
    }

    private void fill() {
        inner.removeAllViews();
        int stepStart = science ? 2 : 1, last = pages() - 1;
        if (science) {
            String[] names = {"1 Ask", "2 Guess", "3 Test", "4 Learn"};
            int s = page == 0 ? 0 : page == 1 ? 1 : page == last ? 3 : 2;
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 4; i++) b.append(i == s ? "【" + names[i] + "】" : names[i]).append(i < 3 ? "  ·  " : "");
            stage.setText(b.toString());
        } else stage.setText((page + 1) + " / " + pages());

        if (page == 0) {
            add(g.icon, 80, Ui.INK, false);
            add(g.title, 28, Ui.INK, true);
            add("You need: " + g.need, 19, 0xFF546E7A, false);
            if (g.safety != null) box("⚠️ " + g.safety, 0xFFFFF59D);
            if (science) box("❓ " + g.question, 0xFFB3E5FC);
        } else if (science && page == 1) {
            add("🤔", 70, Ui.INK, false);
            add("What do you think will happen?", 24, Ui.INK, true);
            add(g.question, 20, 0xFF546E7A, false);
            for (final String o : g.options) {
                final TextView b = Ui.button(a, o, o.equals(guess) ? 0xFFFFD54F : 0xFFE3F2FD, Ui.INK, () -> {
                    guess = o;
                    a.speaker.say("Good guess! Let's find out.");
                    fill();
                });
                b.setTextSize(19);
                LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
                lp.topMargin = Ui.dp(a, 10);
                inner.addView(b, lp);
            }
            if (guess != null) add("Your guess: " + guess + "  →  press ▶", 17, 0xFF2E7D32, true);
        } else if (page < last) {
            int i = page - stepStart;
            add("Step " + (i + 1) + " of " + g.steps.length, 17, 0xFF78909C, true);
            add(g.steps[i][0], 80, Ui.INK, false);
            add(g.steps[i][1], 24, Ui.INK, true);
        } else {
            add(science ? "🔬" : "🎉", 70, Ui.INK, false);
            add(science ? "What happened?" : "All done!", 26, Ui.INK, true);
            add(g.result, 20, Ui.INK, false);
            if (science && guess != null) box("Your guess was: " + guess, 0xFFE1F5FE);
            if (!starGiven) {
                starGiven = true;
                a.addStars(2);
                a.confetti();
            }
        }
        next.setAlpha(page == last ? 0.3f : 1f);
        scroll.scrollTo(0, 0);
        Ui.popIn(inner, 0);
        speak();
    }

    private void speak() {
        int stepStart = science ? 2 : 1, last = pages() - 1;
        String s;
        if (page == 0) s = g.title + ". You need: " + g.need + (g.safety != null ? ". " + g.safety : "") + (science ? ". " + g.question : "");
        else if (science && page == 1) s = "What do you think will happen? " + g.question;
        else if (page < last) s = "Step " + (page - stepStart + 1) + ". " + g.steps[page - stepStart][1];
        else s = (science ? "What happened? " : "All done! ") + g.result;
        a.speaker.say(s);
    }
}
