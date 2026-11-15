using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace InterviewDesk
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            if (args.Length >= 2 && args[0] == "--selftest") { SelfTest(args[1]); return; }
            if (args.Length >= 3 && args[0] == "--uishot") { UiShots(args[1], args[2]); return; }

            Application.SetHighDpiMode(HighDpiMode.SystemAware);
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            App.Config = LocalConfig.Load();
            // "--data <folder>" uses an already set-up folder for this run only (handy for shortcuts).
            if (args.Length >= 2 && args[0] == "--data" && Directory.Exists(args[1]))
            {
                App.Store = new Store(Path.GetFullPath(args[1]));
                Application.Run(new MainForm());
                return;
            }
            string dir = App.Config.DataFolder;
            bool reachable = false;
            if (!string.IsNullOrWhiteSpace(dir))
            {
                try { reachable = Directory.Exists(dir); } catch { }
                if (!reachable)
                    MessageBox.Show("The candidate data folder cannot be reached:\n\n" + dir + "\n\nChoose the folder again (admin password needed).",
                        "Interview Desk", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }

            if (reachable) App.Store = new Store(dir);
            else
            {
                // New instance on this computer: the admin points it at the shared candidate folder.
                string suggestion = string.IsNullOrWhiteSpace(dir)
                    ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "InterviewDesk Data")
                    : dir;
                var store = FolderDialog.Choose(null, suggestion, true);
                if (store == null) return;
                App.Store = store;
                App.Config.DataFolder = store.Dir;
                try { App.Config.Save(); } catch { }
            }

            Application.Run(new MainForm());
        }

        /// <summary>
        /// Developer check of the screens: renders each form to a PNG without ever showing a window or taking focus,
        /// and runs one whole interview through the real quiz form.
        /// </summary>
        [System.Runtime.InteropServices.DllImport("user32.dll")]
        static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        static void UiShots(string dataDir, string outDir)
        {
            Application.SetHighDpiMode(HighDpiMode.SystemAware);
            Application.EnableVisualStyles();
            Directory.CreateDirectory(outDir);
            var log = new StringBuilder();
            try
            {
                App.Config = new LocalConfig();
                App.Store = new Store(dataDir);
                var store = App.Store;
                var settings = store.LoadSettings();

                // Shows the form far off-screen without activating it, so it paints but never appears or takes focus.
                void Prep(Form f)
                {
                    if (f.Visible) return;
                    f.StartPosition = FormStartPosition.Manual;
                    f.Location = new System.Drawing.Point(-20000, -20000);
                    f.ShowInTaskbar = false;
                    f.CreateControl();
                    ShowWindow(f.Handle, 4 /* SW_SHOWNOACTIVATE */);
                }

                void Snap(Form f, string name)
                {
                    Prep(f);
                    f.PerformLayout(); f.Refresh();
                    using var bmp = new System.Drawing.Bitmap(f.Width, f.Height);
                    f.DrawToBitmap(bmp, new System.Drawing.Rectangle(0, 0, f.Width, f.Height));
                    bmp.Save(Path.Combine(outDir, name));
                }

                using (var m = new MainForm()) Snap(m, "1_main.png");
                using (var fd = new FolderDialog(@"\\server\share\Interviews", true)) Snap(fd, "0_folder.png");

                var set = QuestionService.GetAsync(store, settings, 5).GetAwaiter().GetResult();
                var cand = new Candidate
                {
                    Id = Guid.NewGuid().ToString("N").Substring(0, 12), Name = "Asha Verma", Phone = "+91 98123 45678", PhoneKey = Candidate.MakePhoneKey("+91 98123 45678"),
                    Email = "asha@example.com", ExperienceYears = 5, Level = set.Level, StartedUtc = DateTime.UtcNow, Total = set.Questions.Count,
                };
                log.AppendLine("register: " + (store.Register(cand, settings.RetakeMonths) ?? "ok"));
                using (var quiz = new QuizForm(cand, set.Questions, settings))
                {
                    Prep(quiz);
                    quiz.Advance();
                    quiz.Choose(1);
                    Snap(quiz, "2_quiz.png");
                    for (int i = 0; i < set.Questions.Count; i++) { if (i > 0) quiz.Choose(i % 4); quiz.Advance(); }
                    int score = quiz.Answers.Count(a => a.IsCorrect);
                    log.AppendLine($"quiz: {quiz.Answers.Count} answers recorded, {score} correct, none blank = {quiz.Answers.All(a => a.Chosen != "(no answer)")}");
                    store.SaveAnswers(quiz.Answers);
                    store.Update(cand.Id, c => { c.Score = score; c.Total = quiz.Answers.Count; c.CompletedUtc = DateTime.UtcNow; });
                    cand.Score = score;
                }
                using (var r = new ResultDialog(cand, settings)) Snap(r, "3_result.png");
                log.AppendLine("retake now: " + (store.CheckPhone(cand.PhoneKey, settings.RetakeMonths) == null ? "ALLOWED (wrong)" : "blocked (correct)"));

                using (var a = new AdminForm()) { Prep(a); a.Reload(); Snap(a, "4_admin.png"); }
                using (var d = new DetailForm(cand.Id)) { Prep(d); d.Decide(Candidate.Selected); Snap(d, "5_detail.png"); }
                var after = store.LoadCandidates().First(c => c.Id == cand.Id);
                log.AppendLine($"after admin: status={after.Status}, viewed={after.ViewedUtc != null}, decided={after.Decided}");
                log.AppendLine("retake after decision: " + (store.CheckPhone(cand.PhoneKey, settings.RetakeMonths) == null ? "allowed (correct)" : "BLOCKED (wrong)"));
                using (var s = new SettingsForm()) Snap(s, "6_settings.png");
            }
            catch (Exception ex) { log.AppendLine("ERROR " + ex); }
            File.WriteAllText(Path.Combine(outDir, "ui_report.txt"), log.ToString());
        }

        /// <summary>Developer check of the data rules without the UI: writes a report to the given folder.</summary>
        static void SelfTest(string dir)
        {
            Directory.CreateDirectory(dir);
            var log = new StringBuilder();
            void Check(string what, bool ok) => log.AppendLine((ok ? "PASS  " : "FAIL  ") + what);
            try
            {
                var store = new Store(Path.Combine(dir, "data"));
                Directory.CreateDirectory(store.Dir);
                var s = store.LoadSettings();
                s.SetPassword("secret1");
                store.SaveSettings(s);
                s = store.LoadSettings();
                Check("admin password verifies", s.Verify("secret1") && !s.Verify("wrong"));

                foreach (int years in new[] { 0, 3, 6, 12 })
                {
                    var set = QuestionService.GetAsync(store, s, years).GetAwaiter().GetResult();
                    string mix = string.Join(", ", set.Questions.GroupBy(q => q.Difficulty).Select(g => g.Key + "=" + g.Count()));
                    log.AppendLine($"      {years} yrs -> {set.Level}: {set.Questions.Count} questions ({mix}), internet={set.FromInternet}");
                    Check($"question set for {years} yrs", set.Questions.Count == s.QuestionCount && set.Questions.Select(q => q.Text).Distinct().Count() == s.QuestionCount);
                    if (years == 0) log.AppendLine("      sample: " + set.Questions[0].Text + " -> " + set.Questions[0].Correct);
                }
                Check("question bank saved for offline use", store.LoadQuestions().Count >= 10);

                Candidate New(string phone) => new Candidate
                {
                    Id = Guid.NewGuid().ToString("N").Substring(0, 12), Name = "Test, \"Quoted\" Person", Phone = phone,
                    PhoneKey = Candidate.MakePhoneKey(phone), Email = "t@example.com", ExperienceYears = 3, Level = "Junior", StartedUtc = DateTime.UtcNow, Total = 10,
                };
                var a = New("+91 98765-43210");
                Check("first attempt allowed", store.Register(a, 6) == null);
                Check("same number blocked while pending", store.Register(New("9876543210"), 6) != null);
                Check("different number allowed", store.Register(New("9000000001"), 6) == null);

                store.SaveAnswers(new List<AnswerRow> { new AnswerRow { CandidateId = a.Id, No = 1, Difficulty = "easy", Question = "Line1\nLine2, with comma", Chosen = "=A1", Correct = "=A1", IsCorrect = true } });
                var ans = store.LoadAnswers(a.Id);
                Check("answers round-trip through CSV", ans.Count == 1 && ans[0].Question == "Line1\nLine2, with comma" && ans[0].Chosen == "=A1" && ans[0].IsCorrect);
                var back = store.LoadCandidates().First(c => c.Id == a.Id);
                Check("candidate round-trips through CSV", back.Name == a.Name && back.Phone == a.Phone && back.Status == Candidate.Pending);

                store.Update(a.Id, c => c.ViewedUtc = DateTime.UtcNow);
                Check("viewed but undecided still blocked", store.CheckPhone(a.PhoneKey, 6) != null);
                store.Update(a.Id, c => { c.Status = Candidate.Rejected; c.DecisionUtc = DateTime.UtcNow; });
                Check("allowed again after decision", store.CheckPhone(a.PhoneKey, 6) == null);

                var old = New("9111111111");
                old.StartedUtc = DateTime.UtcNow.AddMonths(-7);
                store.Register(old, 6);
                Check("undecided attempt older than 6 months no longer blocks", store.CheckPhone(old.PhoneKey, 6) == null);
            }
            catch (Exception ex) { log.AppendLine("ERROR " + ex); }
            File.WriteAllText(Path.Combine(dir, "report.txt"), log.ToString());
        }
    }
}
