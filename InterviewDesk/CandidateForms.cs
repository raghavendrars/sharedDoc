using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace InterviewDesk
{
    /// <summary>Home screen: the candidate enters their details and starts the interview.</summary>
    sealed class MainForm : Form
    {
        readonly TextBox name, phone, email;
        readonly NumericUpDown years;
        readonly Button start;
        readonly Label status, folder;

        public MainForm()
        {
            Ui.Style(this, "Interview Desk", 560, 560, false);
            FormBorderStyle = FormBorderStyle.FixedSingle;
            MaximizeBox = false;

            Ui.Label(this, "Interview Desk", 32, 22, 0, 24, 22f, true, Ui.Accent);
            Ui.Label(this, "Fill in your details to begin. Questions are chosen to match your experience.", 34, 70, 500, 24, 9.5f, false, Ui.Dim);

            int y = 116;
            Ui.Label(this, "Full name", 34, y + 3, 150); name = Ui.Text(this, 200, y, 326); y += 48;
            Ui.Label(this, "Phone number", 34, y + 3, 150); phone = Ui.Text(this, 200, y, 326); y += 48;
            Ui.Label(this, "Email", 34, y + 3, 150); email = Ui.Text(this, 200, y, 326); y += 48;
            Ui.Label(this, "Total experience", 34, y + 3, 150);
            years = new NumericUpDown { Left = 200, Top = y, Width = 90, Minimum = 0, Maximum = 50, Value = 0 };
            Controls.Add(years);
            Ui.Label(this, "years", 298, y + 3, 80, 24, 10f, false, Ui.Dim);
            y += 62;

            start = Ui.Button(this, "Start interview", 34, y, 492, 50, true);
            start.Font = new Font("Segoe UI", 12f, FontStyle.Bold);
            start.Click += async (s, e) => await StartInterview();
            y += 64;
            status = Ui.Label(this, "", 34, y, 492, 60, 9.5f, false, Ui.Dim);

            var admin = Ui.Button(this, "Admin panel", 34, 504, 130, 34);
            admin.Click += (s, e) => OpenAdmin();
            folder = Ui.Label(this, "", 176, 512, 360, 22, 8.5f, false, Ui.Dim);
            folder.AutoEllipsis = true;
            AcceptButton = start;
            ShowFolder();
        }

        void ShowFolder() => folder.Text = "Data folder: " + App.Store.Dir;

        async Task StartInterview()
        {
            string n = name.Text.Trim(), p = phone.Text.Trim(), em = email.Text.Trim();
            string key = Candidate.MakePhoneKey(p);
            int digits = p.Count(char.IsDigit);
            if (n.Length < 2) { Ui.Error(this, "Please enter your full name."); name.Focus(); return; }
            if (digits < 7 || digits > 15 || !Regex.IsMatch(p, @"^[0-9+\-() ]+$")) { Ui.Error(this, "Please enter a valid phone number."); phone.Focus(); return; }
            if (!Regex.IsMatch(em, @"^[^@\s]+@[^@\s]+\.[^@\s]+$")) { Ui.Error(this, "Please enter a valid email address."); email.Focus(); return; }

            start.Enabled = false;
            try
            {
                var store = App.Store;
                var settings = await Task.Run(() => store.LoadSettings());
                string blocked = await Task.Run(() => store.CheckPhone(key, settings.RetakeMonths));
                if (blocked != null) { Ui.Info(this, blocked); return; }

                status.Text = "Getting questions from the internet...";
                var set = await QuestionService.GetAsync(store, settings, (int)years.Value);
                if (set.Questions.Count < 3)
                {
                    Ui.Error(this, "Questions could not be downloaded and none are saved yet.\n\nPlease check the internet connection and try again.");
                    return;
                }

                var cand = new Candidate
                {
                    Id = Guid.NewGuid().ToString("N").Substring(0, 12), Name = n, Phone = p, PhoneKey = key, Email = em,
                    ExperienceYears = (int)years.Value, Level = set.Level, StartedUtc = DateTime.UtcNow, Total = set.Questions.Count,
                };
                blocked = await Task.Run(() => store.Register(cand, settings.RetakeMonths));
                if (blocked != null) { Ui.Info(this, blocked); return; }

                status.Text = "";
                List<AnswerRow> answers;
                Hide();
                using (var quiz = new QuizForm(cand, set.Questions, settings))
                {
                    quiz.ShowDialog();
                    answers = quiz.Answers;
                }
                Show();

                int score = answers.Count(a => a.IsCorrect);
                await Task.Run(() =>
                {
                    store.SaveAnswers(answers);
                    store.Update(cand.Id, c => { c.Score = score; c.Total = answers.Count; c.CompletedUtc = DateTime.UtcNow; });
                });
                cand.Score = score; cand.Total = answers.Count;
                using (var r = new ResultDialog(cand, settings)) r.ShowDialog(this);
                name.Clear(); phone.Clear(); email.Clear(); years.Value = 0;
                name.Focus();
            }
            catch (Exception ex)
            {
                Show();
                Ui.Error(this, "Something went wrong while using the candidate folder:\n\n" + ex.Message);
            }
            finally
            {
                status.Text = "";
                start.Enabled = true;
            }
        }

        void OpenAdmin()
        {
            try
            {
                var settings = App.Store.LoadSettings();
                if (!PasswordDialog.Login(this, settings)) return;
                using var a = new AdminForm();
                a.ShowDialog(this);
                ShowFolder();
            }
            catch (Exception ex) { Ui.Error(this, "The candidate folder cannot be reached:\n\n" + ex.Message); }
        }
    }

    /// <summary>One question at a time, with an optional countdown per question.</summary>
    sealed class QuizForm : Form
    {
        readonly Candidate cand;
        readonly List<Question> questions;
        readonly Settings settings;
        readonly Label progress, timerLabel, question;
        readonly OptionButton[] options = new OptionButton[4];
        readonly Button next;
        List<string> choices = new List<string>();
        int selected = -1;
        readonly ProgressBar bar;
        readonly Timer timer = new Timer { Interval = 1000 };
        int index = -1, secondsLeft;
        bool finished;

        public readonly List<AnswerRow> Answers = new List<AnswerRow>();

        public QuizForm(Candidate cand, List<Question> questions, Settings settings)
        {
            this.cand = cand; this.questions = questions; this.settings = settings;
            Ui.Style(this, "Interview - " + cand.Name, 820, 560);
            ShowInTaskbar = true;
            ControlBox = true;

            progress = Ui.Label(this, "", 32, 22, 400, 24, 10f, true, Ui.Dim);
            timerLabel = Ui.Label(this, "", 588, 22, 200, 24, 10f, true, Ui.Dim);
            timerLabel.TextAlign = ContentAlignment.MiddleRight;
            bar = new ProgressBar { Left = 32, Top = 52, Width = 756, Height = 6, Maximum = questions.Count, Style = ProgressBarStyle.Continuous };
            Controls.Add(bar);
            question = Ui.Label(this, "", 32, 78, 756, 110, 13f, true);

            for (int i = 0; i < 4; i++)
            {
                int k = i;
                var ob = new OptionButton { Left = 32, Top = 200 + i * 62, Width = 756, Height = 54 };
                ob.Click += (s, e) => Choose(k);
                options[i] = ob;
                Controls.Add(ob);
            }
            next = Ui.Button(this, "Next", 628, 484, 160, 46, true);
            next.Click += (s, e) => Advance();
            Ui.Label(this, "Click an answer or press 1-4, then Next (Enter). You cannot go back.", 32, 496, 560, 24, 9f, false, Ui.Dim);
            AcceptButton = next;
            KeyPreview = true;
            KeyPress += (s, e) =>
            {
                int k = e.KeyChar - '1';
                if (k >= 0 && k < choices.Count) { Choose(k); e.Handled = true; }
            };

            timer.Tick += (s, e) =>
            {
                if (--secondsLeft <= 0) Advance();
                else ShowTimer();
            };
            FormClosing += OnClosing;
            FormClosed += (s, e) => timer.Dispose();
            Shown += (s, e) => Advance();
        }

        internal void Choose(int k)
        {
            selected = k;
            for (int i = 0; i < 4; i++) options[i].SetChosen(i == k);
            next.Enabled = k >= 0;
        }

        void ShowTimer()
        {
            timerLabel.Text = settings.SecondsPerQuestion > 0 ? $"Time left: {secondsLeft} s" : "";
            timerLabel.ForeColor = secondsLeft <= 10 ? Ui.Bad : Ui.Dim;
        }

        void Record(Question q, string chosen)
        {
            Answers.Add(new AnswerRow
            {
                CandidateId = cand.Id, No = Answers.Count + 1, Difficulty = q.Difficulty, Question = q.Text,
                Chosen = chosen ?? "(no answer)", Correct = q.Correct, IsCorrect = chosen == q.Correct,
            });
        }

        internal void Advance()
        {
            timer.Stop();
            if (index >= 0 && index < questions.Count && Answers.Count == index)
                Record(questions[index], selected >= 0 && selected < choices.Count ? choices[selected] : null);
            index++;
            if (index >= questions.Count)
            {
                finished = true;
                Close();
                return;
            }

            var q = questions[index];
            choices = q.Wrong.Take(3).Append(q.Correct).OrderBy(_ => Random.Shared.Next()).ToList();
            progress.Text = $"Question {index + 1} of {questions.Count}";
            bar.Value = index;
            question.Text = q.Text;
            for (int i = 0; i < 4; i++)
            {
                options[i].Visible = i < choices.Count;
                options[i].Text = i < choices.Count ? $"{i + 1}.   {choices[i]}" : "";
            }
            Choose(-1);
            next.Text = index == questions.Count - 1 ? "Finish" : "Next";
            if (settings.SecondsPerQuestion > 0)
            {
                secondsLeft = settings.SecondsPerQuestion;
                ShowTimer();
                timer.Start();
            }
        }

        void OnClosing(object sender, FormClosingEventArgs e)
        {
            if (finished) return;
            if (e.CloseReason == CloseReason.UserClosing)
            {
                timer.Stop();
                var r = MessageBox.Show(this, "End the interview now?\n\nQuestions you have not answered will count as wrong, and you cannot start again.",
                    "Interview Desk", MessageBoxButtons.YesNo, MessageBoxIcon.Warning, MessageBoxDefaultButton.Button2);
                if (r != DialogResult.Yes)
                {
                    e.Cancel = true;
                    if (settings.SecondsPerQuestion > 0) timer.Start();
                    return;
                }
            }
            // Record the question on screen (unanswered) and everything after it.
            for (int i = Answers.Count; i < questions.Count; i++) Record(questions[i], null);
            finished = true;
        }
    }

    /// <summary>An answer choice. It never takes keyboard focus, so nothing is pre-selected by accident.</summary>
    sealed class OptionButton : Button
    {
        public OptionButton()
        {
            SetStyle(ControlStyles.Selectable, false);
            FlatStyle = FlatStyle.Flat;
            TextAlign = ContentAlignment.MiddleLeft;
            Padding = new Padding(14, 0, 8, 0);
            Font = new Font("Segoe UI", 11f);
            Cursor = Cursors.Hand;
            UseMnemonic = false;
            SetChosen(false);
        }

        public void SetChosen(bool on)
        {
            BackColor = on ? Color.FromArgb(219, 232, 255) : Ui.Card;
            ForeColor = Ui.Fg;
            FlatAppearance.BorderColor = on ? Ui.Accent : Color.FromArgb(205, 210, 220);
            FlatAppearance.BorderSize = on ? 2 : 1;
        }
    }

    /// <summary>What the candidate sees at the end.</summary>
    sealed class ResultDialog : Form
    {
        public ResultDialog(Candidate c, Settings s)
        {
            Ui.Style(this, "Your result", 480, 360);
            bool pass = c.Percent >= s.PassPercent;
            Ui.Label(this, "Thank you, " + c.Name, 30, 24, 420, 30, 14f, true).AutoEllipsis = true;
            Ui.Label(this, "Level: " + c.Level, 30, 62, 420, 24, 10f, false, Ui.Dim);

            var score = Ui.Label(this, $"{c.Score} / {c.Total}", 30, 100, 420, 64, 34f, true, pass ? Ui.Good : Ui.Bad);
            score.TextAlign = ContentAlignment.MiddleCenter;
            var pct = Ui.Label(this, $"{c.Percent}% correct   (pass mark {s.PassPercent}%)", 30, 168, 420, 26, 11f, false, Ui.Fg);
            pct.TextAlign = ContentAlignment.MiddleCenter;
            var verdict = Ui.Label(this, pass ? "You reached the pass mark." : "You did not reach the pass mark this time.", 30, 200, 420, 26, 11f, true, pass ? Ui.Good : Ui.Bad);
            verdict.TextAlign = ContentAlignment.MiddleCenter;
            var note = Ui.Label(this, "Your result has been saved. The hiring team will review it and make the final decision.", 30, 236, 420, 44, 9.5f, false, Ui.Dim);
            note.TextAlign = ContentAlignment.MiddleCenter;
            var ok = Ui.Button(this, "Done", 160, 296, 160, 40, true);
            ok.Click += (s2, e) => DialogResult = DialogResult.OK;
            AcceptButton = ok;
        }
    }
}
