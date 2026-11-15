package com.pocketledger.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** One ledger entry. Amounts are kept in paise (1/100) so sums are exact. */
class Txn {
    long id;
    String date = "";        // yyyy-MM-dd
    String person = "";
    long debit, credit;      // paise
    String particular = "";
    String description = "";

    /** Two entries with the same key are treated as the same record (duplicate check). */
    String key() {
        return date + "|" + norm(person) + "|" + debit + "|" + credit + "|" + norm(particular) + "|" + norm(description);
    }

    static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}

class PersonTotal {
    String person;
    long debit, credit;
    int count;
}

/** Money and date helpers. */
final class Fmt {
    private Fmt() { }

    static final String RUPEE = "₹";

    static String money(long paise) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(paise / 100.0);
    }

    static String rupees(long paise) { return RUPEE + " " + money(paise); }

    /** Plain number for files, e.g. 1250.50 */
    static String plain(long paise) {
        return BigDecimal.valueOf(paise, 2).toPlainString();
    }

    /** Parses "1,250.50" into paise. Empty text is 0. Returns -1 when it is not a valid non-negative amount. */
    static long parseMoney(String s) {
        if (s == null) return 0;
        s = s.trim().replace(",", "").replace(RUPEE, "").trim();
        if (s.isEmpty()) return 0;
        try {
            BigDecimal d = new BigDecimal(s);
            if (d.signum() < 0 || d.compareTo(new BigDecimal("9999999999")) > 0) return -1;
            return d.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    static String iso(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    static String today() { return iso(Calendar.getInstance()); }

    static Calendar cal(String iso) {
        Calendar c = Calendar.getInstance();
        try {
            Date d = strict("yyyy-MM-dd").parse(iso);
            if (d != null) c.setTime(d);
        } catch (ParseException ignored) { }
        return c;
    }

    /** yyyy-MM-dd shown as 05 Nov 2026 */
    static String pretty(String iso) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(cal(iso).getTime());
    }

    private static SimpleDateFormat strict(String p) {
        SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
        f.setLenient(false);
        return f;
    }

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy/MM/dd", "dd.MM.yyyy", "dd MMM yyyy", "dd-MMM-yyyy",
    };

    /** Accepts the usual date spellings in uploaded files; returns yyyy-MM-dd or null. */
    static String parseDate(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        for (String p : DATE_PATTERNS) {
            // The year position must match the pattern, otherwise 05-11-2026 would be read as year 5.
            boolean yearFirst = p.startsWith("yyyy");
            boolean textYearFirst = s.length() >= 4 && Character.isDigit(s.charAt(0)) && Character.isDigit(s.charAt(1))
                    && Character.isDigit(s.charAt(2)) && Character.isDigit(s.charAt(3));
            if (yearFirst != textYearFirst) continue;
            try {
                java.text.ParsePosition pos = new java.text.ParsePosition(0);
                Date d = strict(p).parse(s, pos);
                if (d == null || pos.getIndex() != s.length()) continue;
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                int y = c.get(Calendar.YEAR);
                if (y < 1990 || y > 2100) continue;
                return iso(c);
            } catch (Exception ignored) { }
        }
        return null;
    }
}

/** The on-phone SQLite database. */
final class Db extends SQLiteOpenHelper {
    static final String FILE = "expenses.db";
    private static Db instance;

    static synchronized Db get(Context c) {
        if (instance == null) instance = new Db(c.getApplicationContext());
        return instance;
    }

    private Db(Context c) { super(c, FILE, null, 1); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE txn (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, person TEXT NOT NULL, "
                + "debit INTEGER NOT NULL DEFAULT 0, credit INTEGER NOT NULL DEFAULT 0, particular TEXT NOT NULL DEFAULT '', "
                + "description TEXT NOT NULL DEFAULT '', dkey TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_txn_date ON txn(date)");
        db.execSQL("CREATE INDEX idx_txn_dkey ON txn(dkey)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) { }

    private static ContentValues values(Txn t) {
        ContentValues v = new ContentValues();
        v.put("date", t.date);
        v.put("person", t.person.trim());
        v.put("debit", t.debit);
        v.put("credit", t.credit);
        v.put("particular", t.particular.trim());
        v.put("description", t.description.trim());
        v.put("dkey", t.key());
        return v;
    }

    long insert(Txn t) {
        ContentValues v = values(t);
        v.put("created", System.currentTimeMillis());
        return getWritableDatabase().insert("txn", null, v);
    }

    /** Inserts many rows in one transaction; returns how many were written. */
    int insertAll(List<Txn> list) {
        SQLiteDatabase db = getWritableDatabase();
        int n = 0;
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            for (Txn t : list) {
                ContentValues v = values(t);
                v.put("created", now);
                if (db.insert("txn", null, v) >= 0) n++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return n;
    }

    void update(Txn t) {
        getWritableDatabase().update("txn", values(t), "id=?", new String[]{String.valueOf(t.id)});
    }

    void delete(long id) {
        getWritableDatabase().delete("txn", "id=?", new String[]{String.valueOf(id)});
    }

    /** True when another entry (other than excludeId) has exactly the same details. */
    boolean exists(Txn t, long excludeId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM txn WHERE dkey=? AND id<>? LIMIT 1",
                new String[]{t.key(), String.valueOf(excludeId)})) {
            return c.moveToFirst();
        }
    }

    private static Txn read(Cursor c) {
        Txn t = new Txn();
        t.id = c.getLong(0);
        t.date = c.getString(1);
        t.person = c.getString(2);
        t.debit = c.getLong(3);
        t.credit = c.getLong(4);
        t.particular = c.getString(5);
        t.description = c.getString(6);
        return t;
    }

    private static final String COLS = "id,date,person,debit,credit,particular,description";

    /** Entries in a date range (either bound may be null), optionally for one person, oldest first. */
    List<Txn> range(String from, String to, String person) {
        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();
        if (from != null) { where.append(" AND date>=?"); args.add(from); }
        if (to != null) { where.append(" AND date<=?"); args.add(to); }
        if (person != null) { where.append(" AND person=?"); args.add(person); }
        List<Txn> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM txn WHERE " + where + " ORDER BY date, id",
                args.toArray(new String[0]))) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    /** Newest entries first, optionally filtered by a search word. */
    List<Txn> recent(String search, int limit) {
        List<Txn> out = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM txn";
        String[] args = new String[0];
        if (search != null && !search.trim().isEmpty()) {
            String like = "%" + search.trim().replace("%", "").replace("_", "") + "%";
            sql += " WHERE person LIKE ? OR particular LIKE ? OR description LIKE ?";
            args = new String[]{like, like, like};
        }
        sql += " ORDER BY date DESC, id DESC LIMIT " + limit;
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    Txn byId(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM txn WHERE id=?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    /** Earliest and latest entry dates, or null when the book is empty. */
    String[] dateBounds() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT MIN(date), MAX(date) FROM txn", null)) {
            if (c.moveToFirst() && c.getString(0) != null) return new String[]{c.getString(0), c.getString(1)};
        }
        return null;
    }

    int count() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM txn", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Distinct values of a text column (person or particular), most used first. */
    List<String> distinct(String column) {
        List<String> out = new ArrayList<>();
        String col = "particular".equals(column) ? "particular" : "person";
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + col + ", COUNT(*) n FROM txn WHERE " + col + "<>'' GROUP BY " + col + " ORDER BY n DESC, " + col + " LIMIT 500", null)) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }
}

/** App settings plus the password (stored only as a salted PBKDF2 hash). */
final class Prefs {
    private Prefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static boolean hasPassword(Context c) { return sp(c).getString("pw_hash", null) != null; }

    static String bookName(Context c) {
        String n = sp(c).getString("book_name", "");
        return n == null || n.trim().isEmpty() ? c.getString(R.string.app_name) : n;
    }

    static void setBookName(Context c, String name) { sp(c).edit().putString("book_name", name.trim()).apply(); }

    static boolean biometric(Context c) { return sp(c).getBoolean("biometric", false); }

    static void setBiometric(Context c, boolean on) { sp(c).edit().putBoolean("biometric", on).apply(); }

    static long lastBackup(Context c) { return sp(c).getLong("last_backup", 0); }

    static void setLastBackup(Context c, long when) { sp(c).edit().putLong("last_backup", when).apply(); }

    private static byte[] derive(String pw, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 60000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    static boolean setPassword(Context c, String pw) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            sp(c).edit()
                    .putString("pw_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("pw_hash", Base64.encodeToString(derive(pw, salt), Base64.NO_WRAP))
                    .commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean checkPassword(Context c, String pw) {
        try {
            String salt = sp(c).getString("pw_salt", null), hash = sp(c).getString("pw_hash", null);
            if (salt == null || hash == null) return false;
            return MessageDigest.isEqual(derive(pw, Base64.decode(salt, Base64.NO_WRAP)), Base64.decode(hash, Base64.NO_WRAP));
        } catch (Exception e) {
            return false;
        }
    }
}

/** Whether the app is currently locked. It locks again whenever it is sent to the background. */
final class Session {
    private Session() { }

    static boolean locked = true;
    static boolean lockShowing;
    /** Set just before opening a system screen (contact picker, file picker, share sheet) so that does not lock the app. */
    private static long externalAt;

    static void goingExternal() { externalAt = System.currentTimeMillis(); }

    static boolean consumeExternal() {
        boolean recent = externalAt != 0 && System.currentTimeMillis() - externalAt < 5 * 60_000L;
        return recent;
    }

    static void clearExternal() { externalAt = 0; }
}
