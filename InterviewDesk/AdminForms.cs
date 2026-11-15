using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;

namespace InterviewDesk
{
    /// <summary>Admin panel: every candidate in the shared folder, with search and status filter.</summary>
    sealed class AdminForm : Form
    {
        readonly DataGridView grid;
        readonly TextBox search;
        readonly ComboBox filter;
        readonly Label summary;
        List<Candidate> all = new List<Candidate>();

        public AdminForm()
        {
            Ui.Style(this, "Interview Desk - Admin panel", 1180, 660, false);
            MinimumSize = new Size(900, 480);
            ShowInTaskbar = false;

            Ui.Label(this, "Search", 20, 22, 60);
            search = Ui.Text(this, 82, 19, 240);
            search.PlaceholderText = "name, phone or email";
            search.TextChanged += (s, e) => Fill();
            filter = new ComboBox { Left = 336, Top = 18, Width = 190, DropDownStyle = ComboBoxStyle.DropDownList };
            filter.Items.AddRange(new object[] { "All candidates", "Waiting for decision", "Selected", "Rejected" });
            filter.SelectedIndex = 0;
            filter.SelectedIndexChanged += (s, e) => Fill();
            Controls.Add(filter);
            var refresh = Ui.Button(this, "Refresh", 540, 15, 100, 32);
            refresh.Click += (s, e) => Reload();
            var view = Ui.Button(this, "View result", 650, 15, 130, 32, true);
            view.Click += (s, e) => OpenSelected();

            var settings = Ui.Button(this, "Settings", 1060, 15, 100, 32);
            settings.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            settings.Click += (s, e) => { using var f = new SettingsForm(); f.ShowDialog(this); Reload(); };
            var open = Ui.Button(this, "Open folder", 940, 15, 110, 32);
            open.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            open.Click += (s, e) => { try { Process.Start(new ProcessStartInfo("explorer.exe", "\"" + App.Store.Dir + "\"")); } catch { } };

            grid = new DataGridView
            {
                Left = 20, Top = 62, Width = 1140, Height = 550, Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right,
                ReadOnly = true, AllowUserToAddRows = false, AllowUserToDeleteRows = false, AllowUserToResizeRows = false, MultiSelect = false,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect, RowHeadersVisible = false, BackgroundColor = Ui.Card, BorderStyle = BorderStyle.FixedSingle,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill, ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.DisableResizing,
                ColumnHeadersHeight = 34,
            };
            grid.RowTemplate.Height = 30;
            grid.ColumnHeadersDefaultCellStyle.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
            void Col(string title, int weight) => grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = title, FillWeight = weight, SortMode = DataGridViewColumnSortMode.Automatic });
            Col("Name", 150); Col("Phone", 100); Col("Email", 160); Col("Exp (yrs)", 55); Col("Level", 110); Col("Interview date", 110);
            Col("Score", 60); Col("%", 45); Col("Status", 80); Col("Viewed", 55);
            grid.CellDoubleClick += (s, e) => { if (e.RowIndex >= 0) OpenSelected(); };
            Controls.Add(grid);

            summary = Ui.Label(this, "", 20, 622, 900, 24, 9.5f, false, Ui.Dim);
            summary.Width = 1140;
            summary.AutoEllipsis = true;
            summary.Anchor = AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Shown += (s, e) => Reload();
        }

        internal void Reload()
        {
            try { all = App.Store.LoadCandidates(); }
            catch (Exception ex) { Ui.Error(this, "The candidate folder cannot be read:\n\n" + ex.Message); return; }
            Fill();
        }

        void Fill()
        {
            string q = search.Text.Trim();
            var rows = all.AsEnumerable();
            if (q.Length > 0)
                rows = rows.Where(c => c.Name.Contains(q, StringComparison.OrdinalIgnoreCase) || c.Phone.Contains(q, StringComparison.OrdinalIgnoreCase) || c.Email.Contains(q, StringComparison.OrdinalIgnoreCase));
            if (filter.SelectedIndex == 1) rows = rows.Where(c => !c.Decided);
            else if (filter.SelectedIndex == 2) rows = rows.Where(c => c.Status == Candidate.Selected);
            else if (filter.SelectedIndex == 3) rows = rows.Where(c => c.Status == Candidate.Rejected);

            grid.Rows.Clear();
            foreach (var c in rows.OrderByDescending(c => c.StartedUtc))
            {
                bool done = c.CompletedUtc != null;
                int i = grid.Rows.Add(c.Name, c.Phone, c.Email, c.ExperienceYears, c.Level, c.StartedUtc.ToLocalTime().ToString("dd MMM yyyy HH:mm"),
                    done ? $"{c.Score}/{c.Total}" : "Incomplete", done ? (object)c.Percent : "", c.Status, c.ViewedUtc != null ? "Yes" : "No");
                var row = grid.Rows[i];
                row.Tag = c.Id;
                var cell = row.Cells[8];
                cell.Style.ForeColor = c.Status == Candidate.Selected ? Ui.Good : c.Status == Candidate.Rejected ? Ui.Bad : Ui.Fg;
                cell.Style.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
            }
            summary.Text = $"{all.Count} candidates   |   {all.Count(c => !c.Decided)} waiting for decision   |   " +
                           $"{all.Count(c => c.Status == Candidate.Selected)} selected   |   {all.Count(c => c.Status == Candidate.Rejected)} rejected   |   {App.Store.Dir}";
        }

        void OpenSelected()
        {
            if (grid.CurrentRow?.Tag is not string id) { Ui.Info(this, "Choose a candidate first."); return; }
            using (var d = new DetailForm(id)) d.ShowDialog(this);
            Reload();
        }
    }

    /// <summary>One candidate's result, answer by answer. Opening it marks the result as viewed; the decision is made here.</summary>
    sealed class DetailForm : Form
    {
        readonly string id;
        readonly TextBox notes;
        readonly Label statusLabel;

        public DetailForm(string id)
        {
            this.id = id;
            Ui.Style(this, "Candidate result", 1040, 700);
            var store = App.Store;
            var c = store.Update(id, x => { if (x.ViewedUtc == null) x.ViewedUtc = DateTime.UtcNow; });
            if (c == null) { Load += (s, e) => { Ui.Error(this, "This candidate no longer exists in the folder."); Close(); }; return; }
            var answers = store.LoadAnswers(id);
            var settings = store.LoadSettings();

            Ui.Label(this, c.Name, 24, 18, 640, 34, 16f, true).AutoEllipsis = true;
            Ui.Label(this, $"{c.Phone}   |   {c.Email}", 24, 56, 640, 24, 10f, false, Ui.Dim);
            Ui.Label(this, $"{c.ExperienceYears} years experience   |   {c.Level}   |   Interviewed {c.StartedUtc.ToLocalTime():dd MMM yyyy HH:mm}", 24, 80, 760, 24, 10f, false, Ui.Dim);

            bool done = c.CompletedUtc != null;
            bool pass = c.Percent >= settings.PassPercent;
            var score = Ui.Label(this, done ? $"{c.Score}/{c.Total}  ({c.Percent}%)" : "Incomplete", 700, 18, 316, 40, 18f, true, done && pass ? Ui.Good : Ui.Bad);
            score.TextAlign = ContentAlignment.MiddleRight;
            string split = string.Join("    ", new[] { "easy", "medium", "hard" }
                .Where(d => answers.Any(a => a.Difficulty == d))
                .Select(d => $"{char.ToUpper(d[0]) + d.Substring(1)} {answers.Count(a => a.Difficulty == d && a.IsCorrect)}/{answers.Count(a => a.Difficulty == d)}"));
            var sub = Ui.Label(this, done ? $"Pass mark {settings.PassPercent}%    {split}" : "The candidate did not finish the interview.", 560, 62, 456, 24, 9.5f, false, Ui.Dim);
            sub.TextAlign = ContentAlignment.MiddleRight;

            var grid = new DataGridView
            {
                Left = 24, Top = 118, Width = 992, Height = 400, ReadOnly = true, AllowUserToAddRows = false, AllowUserToDeleteRows = false, AllowUserToResizeRows = false,
                RowHeadersVisible = false, BackgroundColor = Ui.Card, BorderStyle = BorderStyle.FixedSingle, SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill, AutoSizeRowsMode = DataGridViewAutoSizeRowsMode.AllCells,
                ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.DisableResizing, ColumnHeadersHeight = 34,
            };
            grid.DefaultCellStyle.WrapMode = DataGridViewTriState.True;
            grid.DefaultCellStyle.Padding = new Padding(2, 4, 2, 4);
            grid.DefaultCellStyle.SelectionBackColor = Color.FromArgb(225, 235, 252);
            grid.DefaultCellStyle.SelectionForeColor = Ui.Fg;
            grid.ColumnHeadersDefaultCellStyle.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
            void Col(string t, int w) => grid.Columns.Add(new DataGridViewTextBoxColumn { HeaderText = t, FillWeight = w, SortMode = DataGridViewColumnSortMode.NotSortable });
            Col("#", 22); Col("Difficulty", 50); Col("Question", 260); Col("Candidate's answer", 120); Col("Correct answer", 120); Col("Result", 45);
            foreach (var a in answers)
            {
                int i = grid.Rows.Add(a.No, a.Difficulty, a.Question, a.Chosen, a.Correct, a.IsCorrect ? "Correct" : "Wrong");
                grid.Rows[i].Cells[5].Style.ForeColor = a.IsCorrect ? Ui.Good : Ui.Bad;
                grid.Rows[i].Cells[5].Style.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
            }
            Controls.Add(grid);

            Ui.Label(this, "Notes", 24, 532, 100);
            notes = new TextBox { Left = 24, Top = 558, Width = 560, Height = 118, Multiline = true, ScrollBars = ScrollBars.Vertical, BorderStyle = BorderStyle.FixedSingle, Text = c.Notes };
            Controls.Add(notes);

            Ui.Label(this, "Decision", 620, 532, 100);
            statusLabel = Ui.Label(this, "", 620, 558, 396, 30, 13f, true);
            ShowStatus(c);
            var select = Ui.Button(this, "Mark as Selected", 620, 600, 192, 42);
            select.BackColor = Ui.Good; select.ForeColor = Color.White; select.FlatAppearance.BorderColor = Ui.Good;
            var reject = Ui.Button(this, "Mark as Rejected", 824, 600, 192, 42);
            reject.BackColor = Ui.Bad; reject.ForeColor = Color.White; reject.FlatAppearance.BorderColor = Ui.Bad;
            select.Click += (s, e) => Decide(Candidate.Selected);
            reject.Click += (s, e) => Decide(Candidate.Rejected);
            var close = Ui.Button(this, "Close", 916, 652, 100, 32);
            close.Click += (s, e) => Close();
            CancelButton = close;
            FormClosing += (s, e) =>
            {
                // Keep notes typed without pressing a decision button.
                try { if (notes.Text != c.Notes) App.Store.Update(id, x => x.Notes = notes.Text); } catch { }
            };
        }

        void ShowStatus(Candidate c)
        {
            statusLabel.Text = c.Status == Candidate.Pending ? "Waiting for decision"
                : $"{c.Status}" + (c.DecisionUtc != null ? $"  ({c.DecisionUtc.Value.ToLocalTime():dd MMM yyyy})" : "");
            statusLabel.ForeColor = c.Status == Candidate.Selected ? Ui.Good : c.Status == Candidate.Rejected ? Ui.Bad : Ui.Dim;
        }

        internal void Decide(string status)
        {
            try
            {
                var c = App.Store.Update(id, x =>
                {
                    x.Status = status;
                    x.DecisionUtc = DateTime.UtcNow;
                    x.ViewedUtc ??= DateTime.UtcNow;
                    x.Notes = notes.Text;
                });
                if (c != null) ShowStatus(c);
            }
            catch (Exception ex) { Ui.Error(this, "The decision could not be saved:\n\n" + ex.Message); }
        }
    }

    /// <summary>Shared interview settings, the admin password, and this computer's data folder.</summary>
    sealed class SettingsForm : Form
    {
        readonly Label folder;
        readonly NumericUpDown count, seconds, months, pass;
        readonly ComboBox topic;
        readonly TextBox pw1, pw2;
        Settings settings;

        public SettingsForm()
        {
            Ui.Style(this, "Settings", 600, 560);
            settings = App.Store.LoadSettings();

            Ui.Label(this, "Candidate data folder (this computer)", 24, 18, 0, 24, 11f, true);
            folder = Ui.Label(this, App.Store.Dir, 24, 48, 430, 24, 9.5f, false, Ui.Dim);
            folder.AutoEllipsis = true;
            var change = Ui.Button(this, "Change...", 466, 42, 110, 32);
            change.Click += (s, e) => ChangeFolder();

            Ui.Label(this, "Interview (shared by every computer using this folder)", 24, 98, 0, 24, 11f, true);
            int y = 134;
            Ui.Label(this, "Question topic", 24, y + 3, 300);
            topic = new ComboBox { Left = 336, Top = y, Width = 240, DropDownStyle = ComboBoxStyle.DropDownList };
            foreach (var t in QuestionService.Topics) topic.Items.Add(t.name);
            Controls.Add(topic);
            y += 40;
            count = Num("Questions per interview", ref y, 3, 30);
            seconds = Num("Seconds per question (0 = no timer)", ref y, 0, 600);
            months = Num("Retake block while undecided (months)", ref y, 0, 60);
            pass = Num("Pass mark (%)", ref y, 0, 100);

            y += 14;
            Ui.Label(this, "Change admin password (leave empty to keep it)", 24, y, 0, 24, 11f, true); y += 36;
            Ui.Label(this, "New password", 24, y + 3, 300); pw1 = Ui.Text(this, 336, y, 240, true); y += 38;
            Ui.Label(this, "Repeat", 24, y + 3, 300); pw2 = Ui.Text(this, 336, y, 240, true);

            var save = Ui.Button(this, "Save", 366, 500, 100, 38, true);
            var cancel = Ui.Button(this, "Cancel", 476, 500, 100, 38);
            save.Click += (s, e) => Save();
            cancel.Click += (s, e) => DialogResult = DialogResult.Cancel;
            CancelButton = cancel;
            ShowValues();
        }

        NumericUpDown Num(string label, ref int y, int min, int max)
        {
            Ui.Label(this, label, 24, y + 3, 300);
            var n = new NumericUpDown { Left = 336, Top = y, Width = 100, Minimum = min, Maximum = max };
            Controls.Add(n);
            y += 40;
            return n;
        }

        void ShowValues()
        {
            folder.Text = App.Store.Dir;
            int ti = topic.Items.IndexOf(settings.Topic);
            topic.SelectedIndex = ti < 0 ? 0 : ti;
            count.Value = settings.QuestionCount;
            seconds.Value = settings.SecondsPerQuestion;
            months.Value = settings.RetakeMonths;
            pass.Value = settings.PassPercent;
        }

        void ChangeFolder()
        {
            var store = FolderDialog.Choose(this, App.Store.Dir, false);
            if (store == null) return;
            App.Store = store;
            App.Config.DataFolder = store.Dir;
            try { App.Config.Save(); } catch (Exception ex) { Ui.Error(this, "The folder choice could not be remembered:\n\n" + ex.Message); }
            settings = store.LoadSettings();
            ShowValues();
        }

        void Save()
        {
            if (pw1.Text.Length > 0 || pw2.Text.Length > 0)
            {
                if (pw1.Text.Length < 4) { Ui.Error(this, "Use at least 4 characters for the password."); return; }
                if (pw1.Text != pw2.Text) { Ui.Error(this, "The two passwords do not match."); return; }
            }
            try
            {
                // Re-read first so a password changed from another computer is not overwritten.
                var s = App.Store.LoadSettings();
                s.Topic = topic.Text;
                s.QuestionCount = (int)count.Value;
                s.SecondsPerQuestion = (int)seconds.Value;
                s.RetakeMonths = (int)months.Value;
                s.PassPercent = (int)pass.Value;
                if (pw1.Text.Length > 0) s.SetPassword(pw1.Text);
                App.Store.SaveSettings(s);
                DialogResult = DialogResult.OK;
            }
            catch (Exception ex) { Ui.Error(this, "Settings could not be saved:\n\n" + ex.Message); }
        }
    }
}
