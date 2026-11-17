package com.kidslearn.app;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Shows the mouth shape for each sound, listens to the child say a word, and can show the front camera as a mirror. */
@SuppressWarnings("deprecation")
final class LipsScreen {
    static final int COLOR = 0xFFAD1457;
    private static final String[][] PAIRS = {
            {"sh", "shh", "ship", "🚢"}, {"ch", "chuh", "chair", "🪑"}, {"th", "thuh", "thumb", "👍"}, {"oo", "ooh", "moon", "🌙"}, {"ee", "eee", "bee", "🐝"}};

    private final MainActivity a;
    private final List<String[]> sounds = new ArrayList<>();
    private final List<TextView> chips = new ArrayList<>();
    private int sel;
    private FaceView face;
    private FrameLayout mirror;
    private TextureView tex;
    private Camera cam;
    private TextView letter, info, tip, feedback, mirrorBtn;

    private LipsScreen(MainActivity a) {
        this.a = a;
        sounds.addAll(Arrays.asList(Data.LETTER_SOUND));
        sounds.addAll(Arrays.asList(PAIRS));
    }

    static View build(MainActivity a) {
        return new LipsScreen(a).make();
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Speak & Lips"));

        LinearLayout top = new LinearLayout(a);
        face = new FaceView(a);
        top.addView(face, Ui.lp(0, Ui.MATCH, 1));
        mirror = new FrameLayout(a);
        mirror.setBackground(Ui.round(Color.BLACK, Ui.dp(a, 16)));
        mirror.setVisibility(View.GONE);
        LinearLayout.LayoutParams mlp = Ui.lp(0, Ui.MATCH, 1);
        mlp.setMargins(Ui.dp(a, 6), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
        top.addView(mirror, mlp);
        page.addView(top, Ui.lp(Ui.MATCH, Ui.dp(a, 210), 0));

        letter = Ui.text(a, "", 46, Color.WHITE, true);
        page.addView(letter);
        info = Ui.text(a, "", 19, Color.WHITE, true);
        page.addView(info);
        tip = Ui.text(a, "", 17, 0xFFFCE4EC, false);
        tip.setPadding(Ui.dp(a, 16), Ui.dp(a, 4), Ui.dp(a, 16), 0);
        page.addView(tip);
        feedback = Ui.text(a, "", 17, 0xFFFFF59D, true);
        feedback.setPadding(Ui.dp(a, 16), Ui.dp(a, 4), Ui.dp(a, 16), 0);
        page.addView(feedback);

        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 4));
        row.addView(tool("🔊 Hear", this::hear), Ui.lp(a, 0, Ui.dp(a, 50), 1, 4));
        row.addView(tool("🎤 Your turn", this::yourTurn), Ui.lp(a, 0, Ui.dp(a, 50), 1, 4));
        mirrorBtn = tool("🪞 Mirror", this::toggleMirror);
        row.addView(mirrorBtn, Ui.lp(a, 0, Ui.dp(a, 50), 1, 4));
        page.addView(row);

        FlowLayout grid = new FlowLayout(a);
        int p = Ui.dp(a, 12);
        grid.setPadding(p, p, p, p);
        for (int i = 0; i < sounds.size(); i++) {
            final int k = i;
            TextView c = Ui.text(a, sounds.get(i)[0], 22, Ui.INK, true);
            c.setMinWidth(Ui.dp(a, 50));
            c.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
            c.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 12)));
            Ui.click(c, () -> select(k, true));
            chips.add(c);
            grid.addView(c);
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(grid);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));

        a.cleanup = this::stopMirror;
        select(0, false);
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private void select(int i, boolean speak) {
        chips.get(sel).setBackground(Ui.round(Color.WHITE, Ui.dp(a, 12)));
        sel = i;
        chips.get(sel).setBackground(Ui.round(0xFFFFF176, Ui.dp(a, 12)));
        String[] s = sounds.get(i);
        int v = FaceView.viseme(s[0]);
        face.show(v);
        letter.setText(s[0]);
        info.setText("says “" + s[1] + "” as in " + s[2] + " " + s[3]);
        tip.setText("👄 " + FaceView.TIP[v]);
        feedback.setText("");
        if (speak) hear();
    }

    private void hear() {
        String[] s = sounds.get(sel);
        face.show(FaceView.viseme(s[0]));
        a.speaker.sayLines(Arrays.asList(s[1], s[1], s[2]), "en", 300);
    }

    private void yourTurn() {
        final String[] s = sounds.get(sel);
        a.listen("en-IN", "Say: " + s[2], results -> {
            boolean ok = false;
            for (String r : results) if (r.toLowerCase().contains(s[2])) ok = true;
            if (ok) {
                feedback.setText("✅ Great! You said “" + s[2] + "”");
                a.speaker.good();
                a.speaker.say("Well done!");
                a.addStars(1);
                a.confetti();
            } else {
                feedback.setText("I heard “" + results.get(0) + "”. Look at the lips and try again!");
                a.speaker.bad();
            }
        });
    }

    private void toggleMirror() {
        if (mirror.getVisibility() == View.VISIBLE) stopMirror();
        else a.withPermission(Manifest.permission.CAMERA, this::startMirror);
    }

    private void startMirror() {
        mirror.setVisibility(View.VISIBLE);
        mirrorBtn.setText("🪞 Hide");
        if (tex == null) {
            tex = new TextureView(a);
            tex.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                    openCamera(st);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                    closeCamera();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture st) {
                }
            });
            mirror.addView(tex, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        } else if (tex.isAvailable()) openCamera(tex.getSurfaceTexture());
    }

    private void openCamera(SurfaceTexture st) {
        if (cam != null || mirror.getVisibility() != View.VISIBLE) return;
        int id = -1;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                id = i;
                break;
            }
        }
        if (id < 0) {
            feedback.setText("This phone has no front camera");
            return;
        }
        try {
            cam = Camera.open(id);
            cam.setDisplayOrientation((360 - info.orientation % 360) % 360);
            Camera.Size ps = cam.getParameters().getPreviewSize();
            cam.setPreviewTexture(st);
            cam.startPreview();
            // fill the frame without stretching (the preview is landscape, the phone is portrait)
            float vw = tex.getWidth(), vh = tex.getHeight();
            if (ps != null && vw > 0 && vh > 0) {
                float content = ps.height / (float) ps.width, view = vw / vh;
                Matrix m = new Matrix();
                if (view > content) m.setScale(1f, view / content, vw / 2, vh / 2);
                else m.setScale(content / view, 1f, vw / 2, vh / 2);
                tex.setTransform(m);
            }
        } catch (Exception e) {
            closeCamera();
            feedback.setText("The camera could not be opened");
        }
    }

    private void closeCamera() {
        if (cam == null) return;
        try {
            cam.stopPreview();
        } catch (RuntimeException ignored) {
        }
        cam.release();
        cam = null;
    }

    private void stopMirror() {
        closeCamera();
        mirror.setVisibility(View.GONE);
        if (mirrorBtn != null) mirrorBtn.setText("🪞 Mirror");
    }
}
