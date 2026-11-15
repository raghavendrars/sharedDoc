package com.pocketledger.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** The one main screen: four tabs (add entry, entries, report, more). */
public class MainActivity extends Activity {
    static final int REQ_CONTACT = 1, REQ_UPLOAD = 2, REQ_SAVE = 3;

    private View root;
    private View[] pages;
    private TextView[] tabs;
    private File pendingSave;

    EntryPage entry;
    ListPage list;
    ReportPage report;
    MorePage more;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root);
        pages = new View[]{findViewById(R.id.pageEntry), findViewById(R.id.pageList), findViewById(R.id.pageReport), findViewById(R.id.pageMore)};
        tabs = new TextView[]{findViewById(R.id.tabEntry), findViewById(R.id.tabList), findViewById(R.id.tabReport), findViewById(R.id.tabMore)};
        for (int i = 0; i < tabs.length; i++) {
            final int k = i;
            tabs[i].setOnClickListener(v -> showTab(k));
        }
        entry = new EntryPage(this, pages[0]);
        list = new ListPage(this, pages[1]);
        report = new ReportPage(this, pages[2]);
        more = new MorePage(this, pages[3]);
        refreshHeader();
        showTab(0);
    }

    void refreshHeader() {
        ((TextView) findViewById(R.id.headerTitle)).setText(Prefs.bookName(this));
    }

    void showTab(int index) {
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
            tabs[i].setTextColor(getColor(i == index ? R.color.primary : R.color.dim));
            tabs[i].setTypeface(Typeface.DEFAULT, i == index ? Typeface.BOLD : Typeface.NORMAL);
        }
        hideKeyboard();
        if (index == 0) entry.onShow();
        else if (index == 1) list.onShow();
        else if (index == 2) report.onShow();
        else more.onShow();
    }

    void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    /** Call after any change so Android schedules the Google backup. */
    void dataChanged() {
        try { new BackupManager(this).dataChanged(); } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ app lock

    @Override
    protected void onStart() {
        super.onStart();
        Session.lockShowing = false;
        Session.clearExternal();
        if (Session.locked) {
            root.setVisibility(View.INVISIBLE);
            Session.lockShowing = true;
            startActivity(new Intent(this, LockActivity.class));
        } else {
            root.setVisibility(View.VISIBLE);
            refreshHeader();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Leaving the app locks it, except while the lock screen itself or a system picker we opened is on top.
        if (!isChangingConfigurations() && !Session.lockShowing && !Session.consumeExternal()) Session.locked = true;
    }

    void lockNow() {
        Session.locked = true;
        root.setVisibility(View.INVISIBLE);
        Session.lockShowing = true;
        startActivity(new Intent(this, LockActivity.class));
    }

    // ------------------------------------------------------------------ talking to other apps

    boolean open(Intent intent, int request) {
        Session.goingExternal();
        try {
            if (request > 0) startActivityForResult(intent, request);
            else startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Session.clearExternal();
            return false;
        }
    }

    /** Lets the user choose where to save a file (phone storage, Google Drive, ...). */
    void saveAs(File f) {
        pendingSave = f;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(ShareProvider.mime(f.getName()));
        i.putExtra(Intent.EXTRA_TITLE, f.getName());
        if (!open(i, REQ_SAVE)) toast("No app on this phone can save files.");
    }

    /** Sends a file (and optional message) to one app, or shows the share sheet when pkg is null. */
    boolean share(File f, String text, String pkg) {
        Intent i = new Intent(Intent.ACTION_SEND);
        if (f != null) {
            Uri uri = ShareProvider.uriFor(f);
            i.setType(ShareProvider.mime(f.getName()));
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.setClipData(ClipData.newRawUri(f.getName(), uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            i.setType("text/plain");
        }
        if (text != null) i.putExtra(Intent.EXTRA_TEXT, text);
        if (pkg != null) {
            i.setPackage(pkg);
            return open(i, 0);
        }
        return open(Intent.createChooser(i, "Share with"), 0);
    }

    void shareWhatsApp(File f, String text) {
        if (share(f, text, "com.whatsapp") || share(f, text, "com.whatsapp.w4b")) return;
        toast("WhatsApp is not installed. Choose another app.");
        share(f, text, null);
    }

    void pickUpload() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv", "application/vnd.ms-excel", "application/octet-stream"});
        if (!open(i, REQ_UPLOAD)) toast("No file picker is available on this phone.");
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        Session.clearExternal();
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == REQ_CONTACT) entry.onContact(uri);
        else if (request == REQ_UPLOAD) importFrom(uri);
        else if (request == REQ_SAVE) writePending(uri);
    }

    private void writePending(Uri uri) {
        File f = pendingSave;
        pendingSave = null;
        if (f == null || !f.isFile()) { toast("The file is no longer available. Please export again."); return; }
        try (InputStream in = new FileInputStream(f); OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception("no stream");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            toast("Saved " + f.getName());
        } catch (Exception e) {
            toast("The file could not be saved there.");
        }
    }

    // ------------------------------------------------------------------ upload with duplicate check

    private void importFrom(final Uri uri) {
        final AlertDialog wait = new AlertDialog.Builder(this).setMessage("Checking the file...").setCancelable(false).show();
        new Thread(() -> {
            ImportResult r;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("no stream");
                r = Files.check(in, Db.get(this));
            } catch (Exception e) {
                r = new ImportResult();
                r.fatal = "The file could not be opened.";
            }
            final ImportResult res = r;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                try { wait.dismiss(); } catch (Exception ignored) { }
                showImportResult(res);
            });
        }).start();
    }

    private void showImportResult(final ImportResult r) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (r.fatal != null) {
            b.setTitle("File not accepted").setMessage(r.fatal).setPositiveButton("OK", null).show();
            return;
        }
        StringBuilder m = new StringBuilder();
        m.append("Rows in the file: ").append(r.rows).append('\n');
        m.append("New entries: ").append(r.fresh.size()).append('\n');
        m.append("Already in the book (skipped): ").append(r.alreadyInBook).append('\n');
        m.append("Repeated inside the file (skipped): ").append(r.repeatedInFile).append('\n');
        m.append("Rows with problems (skipped): ").append(r.invalid);
        if (!r.problems.isEmpty()) {
            m.append("\n");
            for (String p : r.problems) m.append('\n').append(p);
            if (r.invalid > r.problems.size()) m.append("\n... and ").append(r.invalid - r.problems.size()).append(" more");
        }
        b.setTitle("Upload check").setMessage(m.toString());
        if (r.fresh.isEmpty()) {
            b.setPositiveButton("OK", null);
        } else {
            b.setNegativeButton("Cancel", null);
            b.setPositiveButton("Add " + r.fresh.size() + (r.fresh.size() == 1 ? " entry" : " entries"), (d, w) -> {
                int n = Db.get(this).insertAll(r.fresh);
                dataChanged();
                toast(n + (n == 1 ? " entry added." : " entries added."));
                entry.onShow();
                list.onShow();
                more.onShow();
            });
        }
        b.show();
    }
}
