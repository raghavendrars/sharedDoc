package com.pocketledger.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Result of checking an uploaded file before anything is written to the database. */
class ImportResult {
    int rows;                                   // data rows found in the file
    final List<Txn> fresh = new ArrayList<>();  // valid rows that are not in the book yet
    int alreadyInBook, repeatedInFile, invalid;
    final List<String> problems = new ArrayList<>();
    String fatal;                               // set when the file cannot be used at all
}

/** CSV / PDF export and CSV upload. */
final class Files {
    private Files() { }

    static final String[] HEADER = {"Date", "Person", "Debit", "Credit", "Particular", "Description"};

    static String stamp() { return new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()); }

    // ------------------------------------------------------------------ CSV writing

    private static String cell(String s) {
        if (s == null) s = "";
        // A leading apostrophe stops spreadsheet apps from running a cell that starts with = + - @ as a formula.
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private static String line(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(cells[i]));
        }
        return sb.append("\r\n").toString();
    }

    static File csv(Context c, List<Txn> rows, String fileName) throws Exception {
        File f = new File(ShareProvider.dir(c), fileName);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write('﻿'); // lets Excel recognise UTF-8
            w.write(line(HEADER));
            for (Txn t : rows)
                w.write(line(t.date, t.person, t.debit == 0 ? "" : Fmt.plain(t.debit), t.credit == 0 ? "" : Fmt.plain(t.credit), t.particular, t.description));
        }
        return f;
    }

    static File sample(Context c) throws Exception {
        List<Txn> rows = new ArrayList<>();
        rows.add(make(Fmt.today(), "Ramesh Kumar", 45000, 0, "Groceries", "Monthly vegetables and rice"));
        rows.add(make(Fmt.today(), "Office", 0, 2500000, "Salary", "Salary credited"));
        rows.add(make(Fmt.today(), "Sunita", 120000, 0, "Rent", ""));
        return csv(c, rows, "sample_transactions.csv");
    }

    private static Txn make(String date, String person, long debit, long credit, String particular, String description) {
        Txn t = new Txn();
        t.date = date; t.person = person; t.debit = debit; t.credit = credit; t.particular = particular; t.description = description;
        return t;
    }

    // ------------------------------------------------------------------ text summary

    static String summary(String book, String range, String person, long debit, long credit, int count) {
        long bal = credit - debit;
        return "*" + book + "*\n" + range + (person == null ? "" : "\nPerson: " + person) + "\n\n"
                + "Total debit: " + Fmt.rupees(debit) + "\n"
                + "Total credit: " + Fmt.rupees(credit) + "\n"
                + "Balance: " + Fmt.rupees(Math.abs(bal)) + (bal >= 0 ? " (credit)" : " (debit)") + "\n"
                + "Entries: " + count;
    }

    // ------------------------------------------------------------------ PDF

    static File pdf(Context c, String book, String range, String person, List<Txn> rows, long debit, long credit, String fileName) throws Exception {
        final int W = 595, H = 842, M = 36;         // A4 in points
        final float[] colX = {M, M + 62, M + 190, M + 372, M + 448};
        final float right = W - M, rowH = 17;
        PdfDocument doc = new PdfDocument();
        TextPaint normal = new TextPaint(Paint.ANTI_ALIAS_FLAG), bold = new TextPaint(Paint.ANTI_ALIAS_FLAG), dim = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        normal.setTextSize(9.5f); normal.setColor(0xFF1B2430);
        bold.setTextSize(9.5f); bold.setColor(0xFF1B2430); bold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        dim.setTextSize(9f); dim.setColor(0xFF667085);
        Paint line = new Paint(); line.setColor(0xFFD8DEE6); line.setStrokeWidth(0.7f);
        Paint band = new Paint(); band.setColor(0xFF00695C);

        int pageNo = 0, i = 0;
        do {
            pageNo++;
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
            Canvas cv = page.getCanvas();
            float y = M;
            if (pageNo == 1) {
                cv.drawRect(0, 0, W, 78, band);
                TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                title.setColor(0xFFFFFFFF); title.setTextSize(20); title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                cv.drawText(clip(book, title, W - 2 * M), M, 36, title);
                TextPaint sub = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                sub.setColor(0xFFE0F2F1); sub.setTextSize(11);
                cv.drawText(range + (person == null ? "" : "   |   " + person), M, 58, sub);
                y = 104;
                TextPaint big = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                big.setTextSize(13); big.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                long bal = credit - debit;
                cv.drawText("Total debit", M, y, dim); cv.drawText("Total credit", M + 180, y, dim); cv.drawText("Balance", M + 360, y, dim);
                y += 17;
                big.setColor(0xFFC62828); cv.drawText("Rs " + Fmt.money(debit), M, y, big);
                big.setColor(0xFF2E7D32); cv.drawText("Rs " + Fmt.money(credit), M + 180, y, big);
                big.setColor(0xFF1B2430); cv.drawText("Rs " + Fmt.money(Math.abs(bal)) + (bal >= 0 ? " Cr" : " Dr"), M + 360, y, big);
                y += 26;
            }
            // table header
            cv.drawText("Date", colX[0], y, bold); cv.drawText("Person", colX[1], y, bold); cv.drawText("Particular / description", colX[2], y, bold);
            rightText(cv, "Debit", colX[4] - 8, y, bold); rightText(cv, "Credit", right, y, bold);
            y += 6; cv.drawLine(M, y, right, y, line); y += rowH - 4;

            while (i < rows.size() && y < H - M - 20) {
                Txn t = rows.get(i++);
                cv.drawText(new SimpleDateFormat("dd-MM-yy", Locale.US).format(Fmt.cal(t.date).getTime()), colX[0], y, normal);
                cv.drawText(clip(t.person, normal, colX[2] - colX[1] - 6), colX[1], y, normal);
                String what = t.particular + (t.description.isEmpty() ? "" : (t.particular.isEmpty() ? "" : " - ") + t.description);
                cv.drawText(clip(what.replace('\n', ' '), normal, colX[3] - colX[2] - 6), colX[2], y, normal);
                if (t.debit != 0) rightText(cv, Fmt.money(t.debit), colX[4] - 8, y, normal);
                if (t.credit != 0) rightText(cv, Fmt.money(t.credit), right, y, normal);
                y += 5; cv.drawLine(M, y, right, y, line); y += rowH - 5;
            }
            if (i >= rows.size() && y < H - M - 20) {
                y += 4;
                cv.drawText("Total (" + rows.size() + " entries)", colX[1], y, bold);
                rightText(cv, Fmt.money(debit), colX[4] - 8, y, bold);
                rightText(cv, Fmt.money(credit), right, y, bold);
                i = rows.size() + 1; // totals written
            }
            cv.drawText("Page " + pageNo, right - 40, H - 18, dim);
            doc.finishPage(page);
        } while (i <= rows.size());

        File f = new File(ShareProvider.dir(c), fileName);
        try (FileOutputStream out = new FileOutputStream(f)) { doc.writeTo(out); }
        doc.close();
        return f;
    }

    private static String clip(String s, TextPaint p, float width) {
        return TextUtils.ellipsize(s == null ? "" : s, p, width, TextUtils.TruncateAt.END).toString();
    }

    private static void rightText(Canvas cv, String s, float rightX, float y, Paint p) {
        cv.drawText(s, rightX - p.measureText(s), y, p);
    }

    // ------------------------------------------------------------------ CSV reading (upload / restore)

    static List<String[]> parseCsv(String text, char sep) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false, any = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(ch);
                continue;
            }
            if (ch == '"') { quoted = true; any = true; }
            else if (ch == sep) { row.add(cur.toString()); cur.setLength(0); any = true; }
            else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (any || cur.length() > 0) { row.add(cur.toString()); rows.add(row.toArray(new String[0])); }
                row.clear(); cur.setLength(0); any = false;
            } else { cur.append(ch); any = true; }
        }
        if (any || cur.length() > 0) { row.add(cur.toString()); rows.add(row.toArray(new String[0])); }
        return rows;
    }

    private static String clean(String s) {
        s = s == null ? "" : s.trim();
        if (s.length() > 1 && s.charAt(0) == '\'' && "=+-@".indexOf(s.charAt(1)) >= 0) s = s.substring(1);
        return s;
    }

    private static int find(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().toLowerCase(Locale.ROOT);
            for (String n : names) if (h.equals(n) || h.startsWith(n)) return i;
        }
        return -1;
    }

    /** Reads and validates an uploaded file. Nothing is saved here. */
    static ImportResult check(InputStream in, Db db) {
        ImportResult r = new ImportResult();
        String text;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n, total = 0;
            while ((n = in.read(chunk)) > 0) {
                total += n;
                if (total > 8 * 1024 * 1024) { r.fatal = "The file is larger than 8 MB."; return r; }
                buf.write(chunk, 0, n);
            }
            byte[] bytes = buf.toByteArray();
            if (bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K') {
                r.fatal = "This looks like an Excel (.xlsx) file. In Excel choose Save As > CSV and upload that file.";
                return r;
            }
            text = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            r.fatal = "The file could not be read.";
            return r;
        }
        if (text.startsWith("﻿")) text = text.substring(1);
        int firstBreak = text.indexOf('\n');
        String firstLine = firstBreak < 0 ? text : text.substring(0, firstBreak);
        char sep = firstLine.indexOf(',') >= 0 ? ',' : firstLine.indexOf(';') >= 0 ? ';' : firstLine.indexOf('\t') >= 0 ? '\t' : ',';
        List<String[]> rows = parseCsv(text, sep);
        if (rows.isEmpty()) { r.fatal = "The file is empty."; return r; }

        int cDate = 0, cPerson = 1, cDebit = 2, cCredit = 3, cPart = 4, cDesc = 5, start = 0;
        String[] first = rows.get(0);
        if (Fmt.parseDate(first[0]) == null) {
            // First line is a heading: find the columns by name.
            cDate = find(first, "date");
            cPerson = find(first, "person", "name", "party");
            cDebit = find(first, "debit", "dr", "paid", "expense");
            cCredit = find(first, "credit", "cr", "received", "income");
            cPart = find(first, "particular", "category", "item");
            cDesc = find(first, "description", "desc", "note", "remark");
            start = 1;
            if (cDate < 0 || cPerson < 0 || (cDebit < 0 && cCredit < 0)) {
                r.fatal = "The first line must name the columns: Date, Person, Debit, Credit, Particular, Description.";
                return r;
            }
        }

        Set<String> seen = new HashSet<>();
        for (int i = start; i < rows.size(); i++) {
            String[] row = rows.get(i);
            boolean blank = true;
            for (String s : row) if (!s.trim().isEmpty()) { blank = false; break; }
            if (blank) continue;
            r.rows++;
            int lineNo = i + 1;
            Txn t = new Txn();
            String rawDate = get(row, cDate);
            t.date = Fmt.parseDate(rawDate);
            t.person = get(row, cPerson);
            t.debit = Fmt.parseMoney(get(row, cDebit));
            t.credit = Fmt.parseMoney(get(row, cCredit));
            t.particular = get(row, cPart);
            t.description = get(row, cDesc);

            String problem = null;
            if (t.date == null) problem = "date \"" + rawDate + "\" is not understood (use DD-MM-YYYY or YYYY-MM-DD)";
            else if (t.date.compareTo(Fmt.today()) > 0 && t.date.compareTo("2100-01-01") > 0) problem = "date is out of range";
            else if (t.person.isEmpty()) problem = "person is missing";
            else if (t.debit < 0) problem = "debit \"" + get(row, cDebit) + "\" is not a valid amount";
            else if (t.credit < 0) problem = "credit \"" + get(row, cCredit) + "\" is not a valid amount";
            else if (t.debit == 0 && t.credit == 0) problem = "both debit and credit are empty";
            else if (t.person.length() > 60 || t.particular.length() > 60 || t.description.length() > 300) problem = "a text value is too long";
            if (problem != null) {
                r.invalid++;
                if (r.problems.size() < 8) r.problems.add("Line " + lineNo + ": " + problem);
                continue;
            }
            if (!seen.add(t.key())) { r.repeatedInFile++; continue; }
            if (db.exists(t, -1)) { r.alreadyInBook++; continue; }
            r.fresh.add(t);
        }
        if (r.rows == 0) r.fatal = "The file has a heading but no transactions.";
        return r;
    }

    private static String get(String[] row, int col) {
        return col >= 0 && col < row.length ? clean(row[col]) : "";
    }
}
