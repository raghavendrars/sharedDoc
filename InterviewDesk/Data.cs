using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading;

namespace InterviewDesk
{
    /// <summary>Minimal CSV reader/writer (quoted fields, embedded commas and line breaks).</summary>
    public static class Csv
    {
        static readonly Encoding Utf8Bom = new UTF8Encoding(true);

        // Cells starting with = + - @ would run as formulas if the file is opened in Excel; a leading ' prevents that.
        static string Guard(string s) => s.Length > 0 && "=+-@\t".IndexOf(s[0]) >= 0 ? "'" + s : s;
        static string Unguard(string s) => s.Length > 1 && s[0] == '\'' && "=+-@\t".IndexOf(s[1]) >= 0 ? s.Substring(1) : s;

        public static string Line(IEnumerable<string> fields)
        {
            var sb = new StringBuilder();
            bool first = true;
            foreach (var raw in fields)
            {
                if (!first) sb.Append(',');
                first = false;
                string f = Guard(raw ?? "");
                if (f.IndexOfAny(new[] { ',', '"', '\n', '\r' }) >= 0) sb.Append('"').Append(f.Replace("\"", "\"\"")).Append('"');
                else sb.Append(f);
            }
            return sb.ToString();
        }

        /// <summary>Returns data rows (the header line is skipped).</summary>
        public static List<string[]> Read(string path)
        {
            var rows = new List<string[]>();
            if (!File.Exists(path)) return rows;
            string text = File.ReadAllText(path, Encoding.UTF8);
            var row = new List<string>();
            var cur = new StringBuilder();
            bool quoted = false, any = false;
            for (int i = 0; i < text.Length; i++)
            {
                char c = text[i];
                if (quoted)
                {
                    if (c == '"')
                    {
                        if (i + 1 < text.Length && text[i + 1] == '"') { cur.Append('"'); i++; }
                        else quoted = false;
                    }
                    else cur.Append(c);
                    continue;
                }
                if (c == '"') { quoted = true; any = true; }
                else if (c == ',') { row.Add(Unguard(cur.ToString())); cur.Clear(); any = true; }
                else if (c == '\n' || c == '\r')
                {
                    if (c == '\r' && i + 1 < text.Length && text[i + 1] == '\n') i++;
                    if (any || cur.Length > 0) { row.Add(Unguard(cur.ToString())); rows.Add(row.ToArray()); }
                    row.Clear(); cur.Clear(); any = false;
                }
                else { cur.Append(c); any = true; }
            }
            if (any || cur.Length > 0) { row.Add(Unguard(cur.ToString())); rows.Add(row.ToArray()); }
            if (rows.Count > 0) rows.RemoveAt(0);
            return rows;
        }

        public static void Write(string path, string[] header, IEnumerable<string[]> rows)
        {
            var sb = new StringBuilder();
            sb.AppendLine(Line(header));
            foreach (var r in rows) sb.AppendLine(Line(r));
            string tmp = path + ".tmp";
            File.WriteAllText(tmp, sb.ToString(), Utf8Bom);
            File.Move(tmp, path, true);
        }

        public static void Append(string path, string[] header, IEnumerable<string[]> rows)
        {
            var sb = new StringBuilder();
            bool isNew = !File.Exists(path);
            if (isNew) sb.AppendLine(Line(header));
            foreach (var r in rows) sb.AppendLine(Line(r));
            File.AppendAllText(path, sb.ToString(), isNew ? Utf8Bom : new UTF8Encoding(false));
        }
    }

    /// <summary>Per-computer setting: where the shared candidate folder lives.</summary>
    public sealed class LocalConfig
    {
        public string DataFolder { get; set; }

        static string Dir => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "InterviewDesk");
        static string FilePath => Path.Combine(Dir, "config.json");

        public static LocalConfig Load()
        {
            try { if (File.Exists(FilePath)) return JsonSerializer.Deserialize<LocalConfig>(File.ReadAllText(FilePath)) ?? new LocalConfig(); }
            catch { }
            return new LocalConfig();
        }

        public void Save()
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(this));
        }
    }

    /// <summary>Settings stored in the shared folder, so every instance behaves the same.</summary>
    public sealed class Settings
    {
        public string AdminHash = "", AdminSalt = "";
        public int QuestionCount = 10, SecondsPerQuestion = 60, RetakeMonths = 6, PassPercent = 60;
        public string Topic = "Computers";

        public bool HasPassword => AdminHash.Length > 0;

        static byte[] Derive(string pw, byte[] salt) => Rfc2898DeriveBytes.Pbkdf2(pw ?? "", salt, 150_000, HashAlgorithmName.SHA256, 32);

        public void SetPassword(string pw)
        {
            var salt = RandomNumberGenerator.GetBytes(16);
            AdminSalt = Convert.ToBase64String(salt);
            AdminHash = Convert.ToBase64String(Derive(pw, salt));
        }

        public bool Verify(string pw)
        {
            try { return CryptographicOperations.FixedTimeEquals(Derive(pw, Convert.FromBase64String(AdminSalt)), Convert.FromBase64String(AdminHash)); }
            catch { return false; }
        }
    }

    public sealed class Candidate
    {
        public string Id = "", Name = "", Phone = "", PhoneKey = "", Email = "", Level = "", Status = Pending, Notes = "";
        public int ExperienceYears, Score, Total;
        public DateTime StartedUtc;
        public DateTime? CompletedUtc, ViewedUtc, DecisionUtc;

        public const string Pending = "Pending", Selected = "Selected", Rejected = "Rejected";

        public int Percent => Total > 0 ? (int)Math.Round(Score * 100.0 / Total) : 0;
        /// <summary>True once an admin has opened the result and recorded Selected or Rejected.</summary>
        public bool Decided => ViewedUtc != null && Status != Pending;

        public static readonly string[] Header =
        {
            "Id", "Name", "Phone", "PhoneKey", "Email", "ExperienceYears", "Level", "StartedUtc", "CompletedUtc",
            "Score", "Total", "Percent", "Status", "ViewedUtc", "DecisionUtc", "Notes",
        };

        static string D(DateTime? d) => d?.ToString("o", CultureInfo.InvariantCulture) ?? "";
        static DateTime? P(string s) => DateTime.TryParse(s, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind, out var d) ? d.ToUniversalTime() : (DateTime?)null;
        static int I(string s) => int.TryParse(s, NumberStyles.Integer, CultureInfo.InvariantCulture, out int v) ? v : 0;

        public string[] ToRow() => new[]
        {
            Id, Name, Phone, PhoneKey, Email, ExperienceYears.ToString(CultureInfo.InvariantCulture), Level, D(StartedUtc), D(CompletedUtc),
            Score.ToString(CultureInfo.InvariantCulture), Total.ToString(CultureInfo.InvariantCulture), Percent.ToString(CultureInfo.InvariantCulture),
            Status, D(ViewedUtc), D(DecisionUtc), Notes,
        };

        public static Candidate FromRow(string[] r)
        {
            string G(int i) => i < r.Length ? r[i] : "";
            var c = new Candidate
            {
                Id = G(0), Name = G(1), Phone = G(2), PhoneKey = G(3), Email = G(4), ExperienceYears = I(G(5)), Level = G(6),
                StartedUtc = P(G(7)) ?? DateTime.MinValue, CompletedUtc = P(G(8)), Score = I(G(9)), Total = I(G(10)),
                Status = G(12), ViewedUtc = P(G(13)), DecisionUtc = P(G(14)), Notes = G(15),
            };
            if (c.Status != Selected && c.Status != Rejected) c.Status = Pending;
            if (c.PhoneKey.Length == 0) c.PhoneKey = MakePhoneKey(c.Phone);
            return c;
        }

        /// <summary>Digits only, last 10 kept, so "+91 98765-43210" and "9876543210" count as the same number.</summary>
        public static string MakePhoneKey(string phone)
        {
            string digits = new string((phone ?? "").Where(char.IsDigit).ToArray());
            return digits.Length > 10 ? digits.Substring(digits.Length - 10) : digits;
        }
    }

    public sealed class AnswerRow
    {
        public string CandidateId = "", Difficulty = "", Question = "", Chosen = "", Correct = "";
        public int No;
        public bool IsCorrect;

        public static readonly string[] Header = { "CandidateId", "No", "Difficulty", "Question", "ChosenAnswer", "CorrectAnswer", "IsCorrect" };
        public string[] ToRow() => new[] { CandidateId, No.ToString(CultureInfo.InvariantCulture), Difficulty, Question, Chosen, Correct, IsCorrect ? "Yes" : "No" };

        public static AnswerRow FromRow(string[] r)
        {
            string G(int i) => i < r.Length ? r[i] : "";
            int.TryParse(G(1), out int no);
            return new AnswerRow { CandidateId = G(0), No = no, Difficulty = G(2), Question = G(3), Chosen = G(4), Correct = G(5), IsCorrect = G(6) == "Yes" };
        }
    }

    public sealed class Question
    {
        public string Topic = "", Difficulty = "", Text = "", Correct = "", Source = "";
        public string[] Wrong = new string[0];

        public static readonly string[] Header = { "Topic", "Difficulty", "Question", "CorrectAnswer", "Wrong1", "Wrong2", "Wrong3", "Source" };
        public string[] ToRow() => new[] { Topic, Difficulty, Text, Correct, W(0), W(1), W(2), Source };
        string W(int i) => i < Wrong.Length ? Wrong[i] : "";

        public static Question FromRow(string[] r)
        {
            string G(int i) => i < r.Length ? r[i].Trim() : "";
            return new Question
            {
                Topic = G(0), Difficulty = G(1).ToLowerInvariant(), Text = G(2), Correct = G(3),
                Wrong = new[] { G(4), G(5), G(6) }.Where(s => s.Length > 0).ToArray(), Source = G(7),
            };
        }

        public bool Valid => Text.Length > 0 && Correct.Length > 0 && Wrong.Length > 0 && (Difficulty == "easy" || Difficulty == "medium" || Difficulty == "hard");
    }

    /// <summary>
    /// All shared data lives as CSV files in one folder. Every read and write happens while holding a lock file,
    /// so several instances (on one PC or over a network share) can use the same folder safely.
    /// </summary>
    public sealed class Store
    {
        public readonly string Dir;
        string CandPath => Path.Combine(Dir, "candidates.csv");
        string AnsPath => Path.Combine(Dir, "answers.csv");
        string QPath => Path.Combine(Dir, "questions.csv");
        string SetPath => Path.Combine(Dir, "settings.csv");
        string LockPath => Path.Combine(Dir, ".lock");

        public Store(string dir) { Dir = dir; }

        T Locked<T>(Func<T> work)
        {
            var sw = Stopwatch.StartNew();
            while (true)
            {
                FileStream fs;
                try { fs = new FileStream(LockPath, FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None); }
                catch (IOException) when (sw.ElapsedMilliseconds < 10_000 && Directory.Exists(Dir)) { Thread.Sleep(120); continue; }
                using (fs) return work();
            }
        }

        // ---------- settings ----------

        public Settings LoadSettings() => Locked(() =>
        {
            var s = new Settings();
            foreach (var r in Csv.Read(SetPath))
            {
                if (r.Length < 2) continue;
                int.TryParse(r[1], out int n);
                switch (r[0])
                {
                    case "AdminHash": s.AdminHash = r[1]; break;
                    case "AdminSalt": s.AdminSalt = r[1]; break;
                    case "QuestionCount": s.QuestionCount = Math.Clamp(n, 3, 30); break;
                    case "SecondsPerQuestion": s.SecondsPerQuestion = Math.Clamp(n, 0, 600); break;
                    case "RetakeMonths": s.RetakeMonths = Math.Clamp(n, 0, 60); break;
                    case "PassPercent": s.PassPercent = Math.Clamp(n, 0, 100); break;
                    case "Topic": s.Topic = r[1]; break;
                }
            }
            return s;
        });

        public void SaveSettings(Settings s) => Locked(() =>
        {
            Csv.Write(SetPath, new[] { "Key", "Value" }, new[]
            {
                new[] { "AdminHash", s.AdminHash }, new[] { "AdminSalt", s.AdminSalt },
                new[] { "QuestionCount", s.QuestionCount.ToString() }, new[] { "SecondsPerQuestion", s.SecondsPerQuestion.ToString() },
                new[] { "RetakeMonths", s.RetakeMonths.ToString() }, new[] { "PassPercent", s.PassPercent.ToString() },
                new[] { "Topic", s.Topic },
            });
            return 0;
        });

        // ---------- candidates ----------

        List<Candidate> ReadCandidates() => Csv.Read(CandPath).Where(r => r.Length > 3).Select(Candidate.FromRow).ToList();
        void WriteCandidates(List<Candidate> list) => Csv.Write(CandPath, Candidate.Header, list.Select(c => c.ToRow()));

        public List<Candidate> LoadCandidates() => Locked(ReadCandidates);

        /// <summary>
        /// The retake rule: a phone number is blocked while an earlier attempt from the last N months
        /// has not yet been viewed and decided (Selected/Rejected) by an admin.
        /// </summary>
        static string Blocked(List<Candidate> all, string phoneKey, int months)
        {
            var cutoff = DateTime.UtcNow.AddMonths(-months);
            var prior = all.Where(c => c.PhoneKey == phoneKey && c.StartedUtc > cutoff && !c.Decided).OrderByDescending(c => c.StartedUtc).FirstOrDefault();
            if (prior == null) return null;
            var taken = prior.StartedUtc.ToLocalTime();
            return $"This phone number already took the interview on {taken:dd MMM yyyy}.\n\n" +
                   "That result is still waiting for the hiring team's decision, so a new interview is not allowed yet.\n" +
                   $"You can try again after a decision has been made, or after {taken.AddMonths(months):dd MMM yyyy}.";
        }

        public string CheckPhone(string phoneKey, int months) => Locked(() => Blocked(ReadCandidates(), phoneKey, months));

        /// <summary>Checks the retake rule again and records the attempt in one locked step. Returns an error message or null.</summary>
        public string Register(Candidate c, int months) => Locked(() =>
        {
            var all = ReadCandidates();
            string err = Blocked(all, c.PhoneKey, months);
            if (err != null) return err;
            all.Add(c);
            WriteCandidates(all);
            return null;
        });

        public Candidate Update(string id, Action<Candidate> change) => Locked(() =>
        {
            var all = ReadCandidates();
            var c = all.FirstOrDefault(x => x.Id == id);
            if (c == null) return null;
            change(c);
            WriteCandidates(all);
            return c;
        });

        public void SaveAnswers(List<AnswerRow> rows) => Locked(() => { Csv.Append(AnsPath, AnswerRow.Header, rows.Select(a => a.ToRow())); return 0; });

        public List<AnswerRow> LoadAnswers(string candidateId) => Locked(() =>
            Csv.Read(AnsPath).Where(r => r.Length > 0 && r[0] == candidateId).Select(AnswerRow.FromRow).OrderBy(a => a.No).ToList());

        // ---------- question bank (downloaded questions are kept here; admins may add rows by hand) ----------

        public List<Question> LoadQuestions() => Locked(() => Csv.Read(QPath).Select(Question.FromRow).Where(q => q.Valid).ToList());

        public List<Question> MergeQuestions(List<Question> fresh) => Locked(() =>
        {
            var all = Csv.Read(QPath).Select(Question.FromRow).Where(q => q.Valid).ToList();
            var seen = new HashSet<string>(all.Select(q => q.Topic + "|" + q.Text), StringComparer.OrdinalIgnoreCase);
            bool changed = false;
            foreach (var q in fresh)
                if (q.Valid && seen.Add(q.Topic + "|" + q.Text)) { all.Add(q); changed = true; }
            if (changed) Csv.Write(QPath, Question.Header, all.Select(q => q.ToRow()));
            return all;
        });
    }

    public static class App
    {
        public static Store Store;
        public static LocalConfig Config;
    }
}
