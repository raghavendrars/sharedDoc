package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Sanskrit slokas for daily use, read along line by line in Devanagari, Kannada or English letters. */
final class SlokaScreen {
    static final int COLOR = 0xFFBF360C;
    static final int DEV = 0, KAN = 1, LAT = 2;

    /** When, picture, title, Devanagari, English letters, meaning. */
    static final String[][] SLOKAS = {
            {"morning", "🌅", "On waking up · ಪ್ರಾತಃಸ್ಮರಣ ಶ್ಲೋಕ",
                    "कराग्रे वसते लक्ष्मीः करमध्ये सरस्वती ।\nकरमूले तु गोविंदः प्रभाते करदर्शनम् ॥",
                    "Karāgre vasate Lakṣmīḥ karamadhye Sarasvatī\nKaramūle tu Govindaḥ prabhāte karadarśanam",
                    "At the tips of my hands lives Goddess Lakshmi (wealth), in the middle lives Goddess Saraswati (knowledge), and at the base lives Lord Govinda (strength and right action). So we look at our palms in the morning."},
            {"morning", "🌍", "Before stepping on the floor",
                    "समुद्रवसने देवि पर्वतस्तनमण्डले ।\nविष्णुपत्नि नमस्तुभ्यं पादस्पर्शं क्षमस्व मे ॥",
                    "Samudra-vasane devi, parvata-stana-mandale\nVishnupatni namastubhyam, padasparsham kshamasva me",
                    "O Mother Earth, dressed in the oceans and the mountains, I bow to you. Please forgive me for touching you with my feet."},
            {"bath", "🛁", "At bath time",
                    "गङ्गे च यमुने चैव गोदावरि सरस्वति ।\nनर्मदे सिन्धु कावेरि जलेऽस्मिन् सन्निधिं कुरु ॥",
                    "Gange cha Yamune chaiva, Godavari Saraswati\nNarmade Sindhu Kaveri, jale'smin sannidhim kuru",
                    "O holy rivers Ganga, Yamuna, Godavari, Saraswati, Narmada, Sindhu and Kaveri — please be present in this water."},
            {"start", "🐘", "Before starting anything · ಗಣೇಶ ಶ್ಲೋಕ",
                    "वक्रतुंड महाकाय सूर्यकोटि समप्रभ ।\nनिर्विघ्नं कुरु मे देव सर्वकार्येषु सर्वदा ॥",
                    "Vakratuṇḍa mahākāya sūryakoṭi samaprabha\nNirvighnaṁ kuru me deva sarvakāryeṣu sarvadā",
                    "O Lord with a curved trunk and a mighty form, shining like a million suns, please make all my work free from obstacles, always."},
            {"study", "📚", "Before studying · ಸರಸ್ವತಿ ಶ್ಲೋಕ",
                    "सरस्वति नमस्तुभ्यं वरदे कामरूपिणि ।\nविद्यारंभं करिष्यामि सिद्धिर्भवतु मे सदा ॥",
                    "Sarasvati namastubhyaṁ varade kāmarūpiṇi\nVidyārambhaṁ kariṣyāmi siddhirbhavatu me sadā",
                    "Salutations to you, O Goddess Saraswati, giver of blessings who fulfils our wishes. As I begin my studies, may I always be blessed with success and wisdom."},
            {"study", "👨‍🏫", "Respecting teachers · ಗುರು ಶ್ಲೋಕ",
                    "गुरुर्ब्रह्मा गुरुर्विष्णुः गुरुर्देवो महेश्वरः ।\nगुरुः साक्षात् परब्रह्म तस्मै श्रीगुरवे नमः ॥",
                    "Gururbrahmā gururviṣṇuḥ gururdevo maheśvaraḥ\nGuruḥ sākṣāt parabrahma tasmai śrīgurave namaḥ",
                    "The Guru is Brahma (the creator), the Guru is Vishnu (the protector), and the Guru is Maheshwara (who removes ignorance). The Guru is the Supreme itself; salutations to that revered Guru."},
            {"study", "🤝", "Learning together",
                    "ॐ सह नाववतु । सह नौ भुनक्तु ।\nसह वीर्यं करवावहै ।\nतेजस्वि नावधीतमस्तु मा विद्विषावहै ॥\nॐ शान्तिः शान्तिः शान्तिः ॥",
                    "Om saha navavatu, saha nau bhunaktu\nSaha viryam karavavahai\nTejasvi navadhitam astu, ma vidvishavahai\nOm shantih shantih shantih",
                    "May we be protected and nourished together. May we work together with energy. May our learning shine, and may we never dislike each other. Peace, peace, peace."},
            {"meal", "🍛", "Before meals · ಭೋಜನ ಮಂತ್ರ",
                    "अन्नपूर्णे सदापूर्णे शंकरप्राणवल्लभे ।\nज्ञानवैराग्यसिद्ध्यर्थं भिक्षां देहि च पार्वति ॥",
                    "Annapūrṇe sadāpūrṇe śaṅkaraprāṇavallabhe\nJñānavairāgyasiddhyarthaṁ bhikṣāṁ dehi ca pārvati",
                    "O Goddess Annapurna, always full, dear to Lord Shiva — please give us food, along with knowledge and a calm, simple heart."},
            {"meal", "🙏", "Before meals (from the Gita)",
                    "ब्रह्मार्पणं ब्रह्म हविर्ब्रह्माग्नौ ब्रह्मणा हुतम् ।\nब्रह्मैव तेन गन्तव्यं ब्रह्मकर्मसमाधिना ॥",
                    "Brahmarpanam Brahma havir, Brahmagnau Brahmana hutam\nBrahmaiva tena gantavyam, Brahma-karma-samadhina",
                    "The food, the one who offers it and the act of eating are all a gift of God. We eat with thanks."},
            {"evening", "🪔", "Lighting the evening lamp · ದೀಪ ಶ್ಲೋಕ",
                    "शुभं करोति कल्याणं आरोग्यं धनसंपदः ।\nशत्रुबुद्धि विनाशाय दीपज्योतिर्नमोऽस्तु ते ॥",
                    "Śubhaṁ karoti kalyāṇaṁ ārogyaṁ dhanasampadaḥ\nŚatrubuddhi vināśāya dīpajyotirnamo'stu te",
                    "Salutations to the light of the lamp, which brings goodness, well-being, good health and plenty, and takes away unkind thoughts."},
            {"any", "🌈", "Peace for everyone · ಶಾಂತಿ ಮಂತ್ರ",
                    "सर्वे भवंतु सुखिनः सर्वे संतु निरामयाः ।\nसर्वे भद्राणि पश्यंतु मा कश्चिद्दुःखभाग्भवेत् ॥\nॐ शांतिः शांतिः शांतिः ॥",
                    "Sarve bhavantu sukhinaḥ sarve santu nirāmayāḥ\nSarve bhadrāṇi paśyantu mā kaścid duḥkhabhāg bhavet\nOm śāntiḥ śāntiḥ śāntiḥ",
                    "May everyone be happy. May everyone be healthy. May everyone see good things, and may no one be sad. Peace, peace, peace. (Said at the end of prayers and gatherings.)"},
            {"any", "✨", "From darkness to light",
                    "असतो मा सद्गमय ।\nतमसो मा ज्योतिर्गमय ।\nमृत्योर्मा अमृतं गमय ॥",
                    "Asato ma sad gamaya\nTamaso ma jyotir gamaya\nMrityor ma amritam gamaya",
                    "Lead me from untruth to truth, from darkness to light, from death to immortality."},
            {"night", "🌙", "Before sleeping",
                    "करचरणकृतं वाक्कायजं कर्मजं वा\nश्रवणनयनजं वा मानसं वापराधम् ।\nविहितमविहितं वा सर्वमेतत्क्षमस्व\nजय जय करुणाब्धे श्रीमहादेव शम्भो ॥",
                    "Kara-charana-kritam vak-kayajam karmajam va\nShravana-nayanajam va manasam va'paradham\nVihitam-avihitam va sarvam-etat kshamasva\nJaya jaya karunabdhe Shri Mahadeva Shambho",
                    "O Lord Shiva, ocean of kindness, please forgive any mistakes I made today with my hands, feet, words, ears, eyes or mind."},
    };

    private final MainActivity a;
    private int idx, script = DEV, run, line = -1;
    private boolean repeat, starGiven;
    private String speaking;
    private LinearLayout lines;
    private TextView pic, title, status, meaning;
    private final List<TextView> scriptChips = new ArrayList<>();

    private SlokaScreen(MainActivity a, int idx) {
        this.a = a;
        this.idx = idx;
    }

    /** Devanagari → Kannada script (the two Unicode blocks line up letter for letter). */
    static String toKannada(String dev) {
        StringBuilder b = new StringBuilder();
        for (char ch : dev.toCharArray()) {
            if (ch == 'ॐ') b.append("ಓಂ");
            else if (ch == '।' || ch == '॥') b.append(ch);
            else if (ch >= 0x0900 && ch <= 0x097F) b.append((char) (ch + 0x380));
            else b.append(ch);
        }
        return b.toString();
    }

    /** The sloka that suits the time of day. */
    static int forNow() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String when = h >= 4 && h < 8 ? "morning" : h < 9 ? "bath" : h < 12 ? "study" : h < 14 ? "meal" : h < 17 ? "study" : h < 20 ? "evening" : "night";
        if (h < 4) when = "night";
        for (int i = 0; i < SLOKAS.length; i++) if (SLOKAS[i][0].equals(when)) return i;
        return 0;
    }

    static Runnable shelf(final MainActivity a) {
        String[] labels = new String[SLOKAS.length + 1];
        Runnable[] go = new Runnable[SLOKAS.length + 1];
        final int now = forNow();
        labels[0] = "⏰  Right now: " + SLOKAS[now][2];
        go[0] = open(a, now);
        for (int i = 0; i < SLOKAS.length; i++) {
            labels[i + 1] = SLOKAS[i][1] + "  " + SLOKAS[i][2];
            go[i + 1] = open(a, i);
        }
        return HomeScreen.menu(a, "🕉️ Daily slokas", COLOR, labels, go);
    }

    static Runnable open(final MainActivity a, final int i) {
        return () -> a.show(new SlokaScreen(a, i).make(), false, COLOR, MainActivity.AUDIO);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Daily slokas"));

        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);
        pic = Ui.text(a, "", 40, Color.WHITE, false);
        head.addView(pic);
        title = Ui.text(a, "", 21, Color.WHITE, true);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        title.setPadding(Ui.dp(a, 10), 0, 0, 0);
        head.addView(title, Ui.lp(0, Ui.WRAP, 1));
        page.addView(head);

        LinearLayout scripts = new LinearLayout(a);
        scripts.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 4));
        String[] names = {"देवनागरी", "ಕನ್ನಡ", "English"};
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            TextView c = Ui.text(a, names[i], 16, Ui.INK, true);
            c.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
            Ui.click(c, () -> {
                script = k;
                stop();
                fill();
            });
            scriptChips.add(c);
            scripts.addView(c, Ui.lp(a, 0, Ui.WRAP, 1, 3));
        }
        page.addView(scripts);

        ScrollView sv = new ScrollView(a);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.round(0xFFFFF8E1, Ui.dp(a, 24)));
        int p = Ui.dp(a, 16);
        box.setPadding(p, p, p, p);
        lines = new LinearLayout(a);
        lines.setOrientation(LinearLayout.VERTICAL);
        box.addView(lines);
        meaning = Ui.text(a, "", 17, 0xFF5D4037, false);
        meaning.setPadding(0, Ui.dp(a, 14), 0, 0);
        meaning.setLineSpacing(0, 1.2f);
        box.addView(meaning);
        sv.addView(box);
        LinearLayout.LayoutParams slp = Ui.lp(Ui.MATCH, 0, 1);
        slp.setMargins(Ui.dp(a, 12), Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 6));
        page.addView(sv, slp);

        status = Ui.text(a, "", 17, 0xFFFFE0B2, true);
        page.addView(status);

        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 6), Ui.dp(a, 6), Ui.dp(a, 6), 0);
        row.addView(tool("◀", () -> go(idx - 1)), Ui.lp(a, 0, Ui.dp(a, 50), 0.6f, 3));
        row.addView(tool("▶ Listen", () -> start(false)), Ui.lp(a, 0, Ui.dp(a, 50), 1.1f, 3));
        row.addView(tool("🔁 Repeat after me", () -> start(true)), Ui.lp(a, 0, Ui.dp(a, 50), 1.6f, 3));
        row.addView(tool("▶", () -> go(idx + 1)), Ui.lp(a, 0, Ui.dp(a, 50), 0.6f, 3));
        page.addView(row);
        LinearLayout row2 = new LinearLayout(a);
        row2.setPadding(Ui.dp(a, 6), 0, Ui.dp(a, 6), Ui.dp(a, 14));
        row2.addView(tool("⏹ Stop", this::stop), Ui.lp(a, 0, Ui.dp(a, 46), 1, 3));
        row2.addView(tool("💬 Meaning", () -> meaning.setVisibility(meaning.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)),
                Ui.lp(a, 0, Ui.dp(a, 46), 1, 3));
        page.addView(row2);

        final Speaker.Track tracker = new Speaker.Track() {
            @Override
            public void range(String id, int start, int end) {
            }

            @Override
            public void done(String id) {
                if (id.equals(speaking)) {
                    speaking = null;
                    afterLine(line, run);
                }
            }
        };
        page.post(() -> {
            a.speaker.track = tracker;
            fill();
        });
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 15, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private String[] devLines() {
        return SLOKAS[idx][3].split("\n");
    }

    private String shown(int i) {
        if (script == LAT) return SLOKAS[idx][4].split("\n")[i];
        String d = devLines()[i];
        return script == KAN ? toKannada(d) : d;
    }

    private void go(int i) {
        if (i < 0 || i >= SLOKAS.length) return;
        stop();
        idx = i;
        starGiven = false;
        fill();
    }

    private void fill() {
        for (int i = 0; i < scriptChips.size(); i++)
            scriptChips.get(i).setBackground(Ui.round(i == script ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 14)));
        pic.setText(SLOKAS[idx][1]);
        title.setText(SLOKAS[idx][2] + "   (" + (idx + 1) + "/" + SLOKAS.length + ")");
        lines.removeAllViews();
        int n = devLines().length;
        for (int i = 0; i < n; i++) {
            final int k = i;
            TextView t = Ui.text(a, shown(i), script == LAT ? 20 : 24, 0xFF3E2723, true);
            t.setLineSpacing(0, 1.25f);
            t.setPadding(Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8));
            Ui.click(t, () -> {
                stop();
                mark(k, 0xFFFFE082);
                speakLine(k);
            });
            lines.addView(t);
        }
        meaning.setText("💬 " + SLOKAS[idx][5]);
        status.setText("Tap a line to hear it, or press Listen");
    }

    private void mark(int k, int bg) {
        for (int i = 0; i < lines.getChildCount(); i++)
            lines.getChildAt(i).setBackground(i == k ? Ui.round(bg, Ui.dp(a, 12)) : null);
    }

    /** Speaks one line; returns false when no voice can read it. */
    private boolean speakLine(int k) {
        String dev = devLines()[k].replace("॥", "").replace("।", "").trim();
        String kan = toKannada(dev);
        if (script == KAN) {
            speaking = a.speaker.sayTracked(kan, "kn");
            if (speaking == null) speaking = a.speaker.sayTracked(dev, "hi");
        } else {
            speaking = a.speaker.sayTracked(dev, "hi");
            if (speaking == null) speaking = a.speaker.sayTracked(kan, "kn");
        }
        return speaking != null;
    }

    private void start(boolean rep) {
        stop();
        repeat = rep;
        playLine(0, run);
    }

    private void stop() {
        run++;
        line = -1;
        speaking = null;
        a.speaker.stop();
        if (lines != null) mark(-1, 0);
    }

    private void playLine(final int i, final int r) {
        if (r != run) return;
        int n = devLines().length;
        if (i >= n) {
            mark(-1, 0);
            status.setText(repeat ? "🎉 Well done! You said the whole sloka." : "🙏 Now try “Repeat after me”");
            if (!starGiven) {
                starGiven = true;
                a.addStars(repeat ? 2 : 1);
                a.confetti();
            }
            return;
        }
        line = i;
        mark(i, 0xFFFFE082);
        status.setText("🔊 Listen… line " + (i + 1) + " of " + n);
        if (!speakLine(i)) {
            status.setText("📖 Read along… line " + (i + 1) + " of " + n + "  (no voice on this phone)");
            a.later(devLines()[i].length() * 90L + 600, () -> afterLine(i, r));
        }
    }

    private void afterLine(final int i, final int r) {
        if (r != run || i != line) return;
        if (repeat) {
            mark(i, 0xFFC5E1A5);
            status.setText("🗣️ Your turn — say line " + (i + 1) + "!");
            a.later(Math.max(2500, devLines()[i].length() * 110L), () -> playLine(i + 1, r));
        } else a.later(400, () -> playLine(i + 1, r));
    }
}
