package com.pocketledger.app;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

final class UiUtil {
    private UiUtil() { }

    static void pickDate(MainActivity a, String currentIso, Consumer<String> onPicked) {
        Calendar c = Fmt.cal(currentIso);
        new DatePickerDialog(a, (view, y, m, d) -> {
            Calendar n = Calendar.getInstance();
            n.set(y, m, d);
            onPicked.accept(Fmt.iso(n));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    static int dp(View v, float dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }
}

// ====================================================================== Add / edit entry

final class EntryPage {
    private final MainActivity a;
    private final TextView title, dateField;
    private final AutoCompleteTextView person, particular;
    private final EditText debit, credit, desc;
    private final Button save, cancel;
    private String date = Fmt.today();
    private long editingId = -1;

    EntryPage(MainActivity a, View v) {
        this.a = a;
        title = v.findViewById(R.id.entryTitle);
        dateField = v.findViewById(R.id.dateField);
        person = v.findViewById(R.id.personField);
        particular = v.findViewById(R.id.particularField);
        debit = v.findViewById(R.id.debitField);
        credit = v.findViewById(R.id.creditField);
        desc = v.findViewById(R.id.descField);
        save = v.findViewById(R.id.saveEntry);
        cancel = v.findViewById(R.id.cancelEdit);

        dateField.setOnClickListener(x -> UiUtil.pickDate(a, date, picked -> { date = picked; showDate(); }));
        v.findViewById(R.id.pickContact).setOnClickListener(x -> {
            // The system contact picker: the user chooses one contact and only that one is shared with the app.
            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            if (!a.open(i, MainActivity.REQ_CONTACT)) a.toast("No contacts app was found on this phone.");
        });
        save.setOnClickListener(x -> trySave());
        cancel.setOnClickListener(x -> reset(true));
        showDate();
    }

    private void showDate() {
        dateField.setText(Fmt.pretty(date) + (date.equals(Fmt.today()) ? "  (today)" : ""));
    }

    void onShow() {
        Db db = Db.get(a);
        person.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_dropdown_item_1line, db.distinct("person")));
        particular.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_dropdown_item_1line, db.distinct("particular")));
    }

    void onContact(Uri uri) {
        try (Cursor c = a.getContentResolver().query(uri, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) {
                person.setText(c.getString(0), false);
                person.setSelection(person.getText().length());
                return;
            }
        } catch (Exception ignored) { }
        a.toast("That contact could not be read.");
    }

    void edit(Txn t) {
        editingId = t.id;
        date = t.date;
        showDate();
        person.setText(t.person, false);
        debit.setText(t.debit == 0 ? "" : Fmt.plain(t.debit));
        credit.setText(t.credit == 0 ? "" : Fmt.plain(t.credit));
        particular.setText(t.particular, false);
        desc.setText(t.description);
        title.setText("Edit entry");
        save.setText("Update entry");
        cancel.setVisibility(View.VISIBLE);
    }

    private void reset(boolean everything) {
        editingId = -1;
        if (everything) { date = Fmt.today(); showDate(); }
        person.setText("", false);
        debit.setText("");
        credit.setText("");
        particular.setText("", false);
        desc.setText("");
        title.setText("New entry");
        save.setText("Save entry");
        cancel.setVisibility(View.GONE);
    }

    private void trySave() {
        final Txn t = new Txn();
        t.id = editingId;
        t.date = date;
        t.person = person.getText().toString().trim();
        t.debit = Fmt.parseMoney(debit.getText().toString());
        t.credit = Fmt.parseMoney(credit.getText().toString());
        t.particular = particular.getText().toString().trim();
        t.description = desc.getText().toString().trim();

        if (t.person.isEmpty()) { person.setError("Who is this entry for?"); person.requestFocus(); return; }
        if (t.debit < 0) { debit.setError("Not a valid amount"); debit.requestFocus(); return; }
        if (t.credit < 0) { credit.setError("Not a valid amount"); credit.requestFocus(); return; }
        if (t.debit == 0 && t.credit == 0) { debit.setError("Enter a debit or a credit amount"); debit.requestFocus(); return; }

        if (Db.get(a).exists(t, editingId)) {
            new AlertDialog.Builder(a)
                    .setTitle("Same entry already saved")
                    .setMessage("An entry with the same date, person, amounts, particular and description is already in the book.\n\nSave it again anyway?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Save anyway", (d, w) -> commit(t))
                    .show();
            return;
        }
        commit(t);
    }

    private void commit(Txn t) {
        Db db = Db.get(a);
        boolean editing = editingId >= 0;
        if (editing) db.update(t); else db.insert(t);
        a.dataChanged();
        a.hideKeyboard();
        a.toast(editing ? "Entry updated." : "Entry saved.");
        reset(editing);   // after a new entry the chosen date is kept, handy when entering several for one day
        onShow();
        if (editing) a.showTab(1);
    }
}

// ====================================================================== Entries list

final class ListPage {
    private final MainActivity a;
    private final ListView listView;
    private final TextView info, empty;
    private final EditText search;
    private List<Txn> rows = new ArrayList<>();
    private final BaseAdapter adapter;

    ListPage(MainActivity a, View v) {
        this.a = a;
        listView = v.findViewById(R.id.txnList);
        info = v.findViewById(R.id.listInfo);
        empty = v.findViewById(R.id.listEmpty);
        search = v.findViewById(R.id.searchField);

        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return rows.get(i).id; }

            @Override
            public View getView(int i, View row, ViewGroup parent) {
                if (row == null) row = LayoutInflater.from(a).inflate(R.layout.row_txn, parent, false);
                Txn t = rows.get(i);
                ((TextView) row.findViewById(R.id.rowPerson)).setText(t.person);
                String detail = Fmt.pretty(t.date) + (t.particular.isEmpty() ? "" : "  |  " + t.particular)
                        + (t.description.isEmpty() ? "" : "\n" + t.description);
                ((TextView) row.findViewById(R.id.rowDetail)).setText(detail);
                TextView d = row.findViewById(R.id.rowDebit), c = row.findViewById(R.id.rowCredit);
                d.setVisibility(t.debit != 0 ? View.VISIBLE : View.GONE);
                c.setVisibility(t.credit != 0 ? View.VISIBLE : View.GONE);
                d.setText("- " + Fmt.money(t.debit));
                c.setText("+ " + Fmt.money(t.credit));
                return row;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, view, pos, id) -> menu(rows.get(pos)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { load(); }
        });
    }

    void onShow() { load(); }

    private void load() {
        Db db = Db.get(a);
        String q = search.getText().toString();
        rows = db.recent(q, 1000);
        adapter.notifyDataSetChanged();
        int total = db.count();
        boolean none = rows.isEmpty();
        listView.setVisibility(none ? View.GONE : View.VISIBLE);
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        empty.setText(total == 0 ? "No entries yet.\nAdd your first one from the Add entry tab." : "Nothing matches your search.");
        info.setText(q.trim().isEmpty()
                ? total + (total == 1 ? " entry" : " entries") + " in the book. Tap one to edit or delete."
                : rows.size() + " of " + total + " entries match.");
    }

    private void menu(final Txn t) {
        new AlertDialog.Builder(a)
                .setTitle(t.person + "  |  " + Fmt.pretty(t.date))
                .setItems(new String[]{"Edit", "Delete"}, (d, which) -> {
                    if (which == 0) {
                        a.showTab(0);
                        a.entry.edit(t);
                    } else {
                        new AlertDialog.Builder(a)
                                .setMessage("Delete this entry? This cannot be undone.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Delete", (d2, w2) -> {
                                    Db.get(a).delete(t.id);
                                    a.dataChanged();
                                    load();
                                })
                                .show();
                    }
                })
                .show();
    }
}

// ====================================================================== Report

final class ReportPage {
    private final MainActivity a;
    private final TextView fromField, toField, range, totalDebit, totalCredit, balance, entryCount;
    private final Spinner personFilter;
    private final View loading, result, totalsCard, personCard, scroll;
    private final LinearLayout personRows;
    private final BarChartView chart;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String from, to;

    // What the last generated report contains (used by export and share).
    private List<Txn> rows = new ArrayList<>();
    private long sumDebit, sumCredit;
    private String rangeLabel = "", personLabel;

    ReportPage(MainActivity a, View v) {
        this.a = a;
        scroll = v;
        fromField = v.findViewById(R.id.fromField);
        toField = v.findViewById(R.id.toField);
        range = v.findViewById(R.id.reportRange);
        totalDebit = v.findViewById(R.id.totalDebit);
        totalCredit = v.findViewById(R.id.totalCredit);
        balance = v.findViewById(R.id.balance);
        entryCount = v.findViewById(R.id.entryCount);
        personFilter = v.findViewById(R.id.personFilter);
        loading = v.findViewById(R.id.reportLoading);
        result = v.findViewById(R.id.reportResult);
        totalsCard = v.findViewById(R.id.totalsCard);
        personCard = v.findViewById(R.id.personCard);
        personRows = v.findViewById(R.id.personRows);
        chart = v.findViewById(R.id.chart);

        thisMonth();
        fromField.setOnClickListener(x -> UiUtil.pickDate(a, from, d -> { from = d; if (from.compareTo(to) > 0) to = from; showRange(); }));
        toField.setOnClickListener(x -> UiUtil.pickDate(a, to, d -> { to = d; if (to.compareTo(from) < 0) from = to; showRange(); }));
        v.findViewById(R.id.chipToday).setOnClickListener(x -> { from = to = Fmt.today(); showRange(); generate(); });
        v.findViewById(R.id.chipMonth).setOnClickListener(x -> { thisMonth(); generate(); });
        v.findViewById(R.id.chipLastMonth).setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, -1);
            c.set(Calendar.DAY_OF_MONTH, 1);
            from = Fmt.iso(c);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            to = Fmt.iso(c);
            showRange();
            generate();
        });
        v.findViewById(R.id.chipYear).setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_YEAR, 1);
            from = Fmt.iso(c);
            to = Fmt.today();
            showRange();
            generate();
        });
        v.findViewById(R.id.chipAll).setOnClickListener(x -> {
            String[] b = Db.get(a).dateBounds();
            if (b == null) { a.toast("There are no entries yet."); return; }
            from = b[0];
            to = b[1].compareTo(Fmt.today()) > 0 ? b[1] : Fmt.today();
            showRange();
            generate();
        });
        v.findViewById(R.id.generate).setOnClickListener(x -> generate());
        v.findViewById(R.id.shareWhatsApp).setOnClickListener(x -> shareDialog());
        v.findViewById(R.id.exportPdf).setOnClickListener(x -> { File f = makePdf(); if (f != null) a.saveAs(f); });
        v.findViewById(R.id.exportCsv).setOnClickListener(x -> { File f = makeCsv(); if (f != null) a.saveAs(f); });
    }

    private void thisMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        from = Fmt.iso(c);
        to = Fmt.today();
        showRange();
    }

    private void showRange() {
        fromField.setText(Fmt.pretty(from));
        toField.setText(Fmt.pretty(to));
    }

    void onShow() {
        Object keep = personFilter.getSelectedItem();
        List<String> items = new ArrayList<>();
        items.add("All persons");
        List<String> people = Db.get(a).distinct("person");
        Collections.sort(people, String.CASE_INSENSITIVE_ORDER);
        items.addAll(people);
        ArrayAdapter<String> ad = new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        personFilter.setAdapter(ad);
        int idx = keep == null ? 0 : items.indexOf(keep.toString());
        personFilter.setSelection(Math.max(0, idx));
    }

    private void generate() {
        a.hideKeyboard();
        handler.removeCallbacksAndMessages(null);
        result.setVisibility(View.GONE);
        loading.setAlpha(0f);
        loading.setVisibility(View.VISIBLE);
        loading.animate().alpha(1f).setDuration(200).start();
        // A short pause so the "working" animation is seen before the numbers roll in.
        handler.postDelayed(this::showResult, 750);
    }

    private void showResult() {
        personLabel = personFilter.getSelectedItemPosition() <= 0 ? null : String.valueOf(personFilter.getSelectedItem());
        rows = Db.get(a).range(from, to, personLabel);
        sumDebit = 0;
        sumCredit = 0;
        Map<String, PersonTotal> byPerson = new LinkedHashMap<>();
        for (Txn t : rows) {
            sumDebit += t.debit;
            sumCredit += t.credit;
            PersonTotal p = byPerson.get(t.person);
            if (p == null) { p = new PersonTotal(); p.person = t.person; byPerson.put(t.person, p); }
            p.debit += t.debit;
            p.credit += t.credit;
            p.count++;
        }
        rangeLabel = from.equals(to) ? Fmt.pretty(from) : Fmt.pretty(from) + " to " + Fmt.pretty(to);
        range.setText(rangeLabel + (personLabel == null ? "" : "   |   " + personLabel));
        entryCount.setText(rows.size() + (rows.size() == 1 ? " entry" : " entries") + " in this range");

        loading.setVisibility(View.GONE);
        result.setVisibility(View.VISIBLE);

        // Card slides up and fades in, totals count up, bars grow, then the person rows arrive one by one.
        totalsCard.setAlpha(0f);
        totalsCard.setTranslationY(UiUtil.dp(totalsCard, 40));
        totalsCard.animate().alpha(1f).translationY(0).setDuration(450).setInterpolator(new DecelerateInterpolator()).start();
        final long bal = sumCredit - sumDebit;
        ValueAnimator count = ValueAnimator.ofFloat(0f, 1f);
        count.setDuration(1000);
        count.setInterpolator(new DecelerateInterpolator(1.6f));
        count.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            totalDebit.setText(Fmt.rupees((long) (sumDebit * (double) f)));
            totalCredit.setText(Fmt.rupees((long) (sumCredit * (double) f)));
            balance.setText("Balance: " + Fmt.rupees((long) (Math.abs(bal) * (double) f)) + (bal >= 0 ? "  (credit is higher)" : "  (debit is higher)"));
        });
        count.start();
        balance.setTextColor(a.getColor(bal >= 0 ? R.color.credit : R.color.debit));
        chart.animateTo(sumDebit, sumCredit);

        personRows.removeAllViews();
        List<PersonTotal> people = new ArrayList<>(byPerson.values());
        Collections.sort(people, (x, y) -> Long.compare(y.debit + y.credit, x.debit + x.credit));
        personCard.setVisibility(people.isEmpty() ? View.GONE : View.VISIBLE);
        int shown = Math.min(people.size(), 60);
        for (int i = 0; i < shown; i++) {
            View row = personRow(people.get(i));
            row.setAlpha(0f);
            row.setTranslationX(UiUtil.dp(row, 60));
            personRows.addView(row);
            row.animate().alpha(1f).translationX(0).setStartDelay(350 + Math.min(i, 12) * 70L).setDuration(350)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        if (scroll instanceof android.widget.ScrollView)
            handler.postDelayed(() -> ((android.widget.ScrollView) scroll).smoothScrollTo(0, result.getTop()), 120);
    }

    private View personRow(PersonTotal p) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiUtil.dp(row, 8);
        row.setPadding(0, pad, 0, pad);

        TextView name = new TextView(a);
        name.setText(p.person + "  (" + p.count + ")");
        name.setTextSize(15);
        name.setTextColor(a.getColor(R.color.text));
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout amounts = new LinearLayout(a);
        amounts.setOrientation(LinearLayout.VERTICAL);
        amounts.setGravity(Gravity.END);
        if (p.debit != 0) amounts.addView(amount("- " + Fmt.money(p.debit), R.color.debit));
        if (p.credit != 0) amounts.addView(amount("+ " + Fmt.money(p.credit), R.color.credit));
        row.addView(amounts, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private TextView amount(String text, int color) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(a.getColor(color));
        t.setGravity(Gravity.END);
        return t;
    }

    private String fileBase() { return "Report_" + from + "_to_" + to; }

    private File makePdf() {
        try {
            return Files.pdf(a, Prefs.bookName(a), rangeLabel, personLabel, rows, sumDebit, sumCredit, fileBase() + ".pdf");
        } catch (Exception e) {
            a.toast("The PDF could not be created.");
            return null;
        }
    }

    private File makeCsv() {
        try {
            return Files.csv(a, rows, fileBase() + ".csv");
        } catch (Exception e) {
            a.toast("The file could not be created.");
            return null;
        }
    }

    private void shareDialog() {
        final String text = Files.summary(Prefs.bookName(a), rangeLabel, personLabel, sumDebit, sumCredit, rows.size());
        new AlertDialog.Builder(a)
                .setTitle("Share on WhatsApp")
                .setItems(new String[]{"PDF report with all entries", "CSV file (opens in Excel)", "Totals as a text message"}, (d, which) -> {
                    if (which == 0) { File f = makePdf(); if (f != null) a.shareWhatsApp(f, text); }
                    else if (which == 1) { File f = makeCsv(); if (f != null) a.shareWhatsApp(f, text); }
                    else a.shareWhatsApp(null, text);
                })
                .show();
    }
}

// ====================================================================== More: name, security, backup, upload

final class MorePage {
    private final MainActivity a;
    private final EditText bookName;
    private final Switch bioSwitch;
    private final TextView backupInfo;

    MorePage(MainActivity a, View v) {
        this.a = a;
        bookName = v.findViewById(R.id.bookNameEdit);
        bioSwitch = v.findViewById(R.id.bioSwitch);
        backupInfo = v.findViewById(R.id.backupInfo);
        ((TextView) v.findViewById(R.id.aboutText)).setText("Your entries are stored on this phone only.  Version 1.0");

        v.findViewById(R.id.saveBookName).setOnClickListener(x -> {
            Prefs.setBookName(a, bookName.getText().toString());
            a.refreshHeader();
            a.hideKeyboard();
            a.dataChanged();
            a.toast("Name saved.");
        });

        boolean bio = Bio.available(a);
        bioSwitch.setEnabled(bio);
        bioSwitch.setChecked(bio && Prefs.biometric(a));
        if (!bio) bioSwitch.setText("Fingerprint (not set up on this phone)");
        bioSwitch.setOnCheckedChangeListener((b, on) -> Prefs.setBiometric(a, on));

        v.findViewById(R.id.changePassword).setOnClickListener(x -> changePassword());
        v.findViewById(R.id.lockNow).setOnClickListener(x -> a.lockNow());
        v.findViewById(R.id.backupNow).setOnClickListener(x -> backupNow());
        v.findViewById(R.id.restoreBackup).setOnClickListener(x -> a.pickUpload());
        v.findViewById(R.id.uploadCsv).setOnClickListener(x -> a.pickUpload());
        v.findViewById(R.id.sampleCsv).setOnClickListener(x -> {
            try { a.share(Files.sample(a), null, null); } catch (Exception e) { a.toast("The sample could not be created."); }
        });
    }

    void onShow() {
        bookName.setText(Prefs.bookName(a));
        long last = Prefs.lastBackup(a);
        backupInfo.setText(
                "Automatic: this book is part of the phone's Google backup, kept in the Google Drive of the account on this phone. "
                + "Android runs it by itself, usually once a day on Wi-Fi while charging. Check it is on in Settings > Google > Backup.\n\n"
                + "Manual: the button below saves a backup file to Google Drive right away. Restoring adds back any entries that are missing "
                + "and skips the ones already here.\n\n"
                + "Entries in the book: " + Db.get(a).count() + "\n"
                + "Last manual backup: " + (last == 0 ? "never" : new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date(last))));
    }

    private void backupNow() {
        List<Txn> all = Db.get(a).range(null, null, null);
        if (all.isEmpty()) { a.toast("There is nothing to back up yet."); return; }
        File f;
        try {
            f = Files.csv(a, all, "ExpenseBackup_" + Files.stamp() + ".csv");
        } catch (Exception e) {
            a.toast("The backup file could not be created.");
            return;
        }
        // Straight to the Google Drive app when it is installed; otherwise the system "save to" screen (which also lists Drive).
        if (!a.share(f, null, "com.google.android.apps.docs")) a.saveAs(f);
        Prefs.setLastBackup(a, System.currentTimeMillis());
        onShow();
    }

    private void changePassword() {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = UiUtil.dp(box, 22);
        box.setPadding(pad, UiUtil.dp(box, 8), pad, 0);
        final EditText current = field(box, "Current password"), fresh = field(box, "New password"), again = field(box, "Repeat new password");

        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle("Change password")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Change", null)
                .create();
        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            String n = fresh.getText().toString();
            if (!Prefs.checkPassword(a, current.getText().toString())) { current.setError("Wrong password"); return; }
            if (n.length() < 4) { fresh.setError("Use at least 4 characters"); return; }
            if (!n.equals(again.getText().toString())) { again.setError("Does not match"); return; }
            if (!Prefs.setPassword(a, n)) { a.toast("The password could not be saved."); return; }
            a.dataChanged();
            a.toast("Password changed.");
            dlg.dismiss();
        }));
        dlg.show();
    }

    private EditText field(LinearLayout box, String hint) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setSingleLine(true);
        box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }
}
