using System;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace Blockhaven
{
    static class Ui
    {
        public static readonly Color Bg = Color.FromArgb(28, 34, 44);
        public static readonly Color Panel = Color.FromArgb(40, 48, 62);
        public static readonly Color Fg = Color.FromArgb(232, 236, 242);
        public static readonly Color Dim = Color.FromArgb(160, 170, 185);
        public static readonly Color Accent = Color.FromArgb(76, 175, 80);

        public static void Style(Form f, string title, int w, int h)
        {
            f.Text = title;
            f.ClientSize = new Size(w, h);
            f.BackColor = Bg;
            f.ForeColor = Fg;
            f.Font = new Font("Segoe UI", 10f);
            f.FormBorderStyle = FormBorderStyle.FixedDialog;
            f.MaximizeBox = false;
            f.MinimizeBox = false;
            f.StartPosition = FormStartPosition.CenterScreen;
            f.AutoScaleMode = AutoScaleMode.Dpi;
        }

        public static Label Label(Control parent, string text, int x, int y, int w = 0, bool dim = false, float size = 0, bool bold = false)
        {
            var l = new Label { Text = text, Left = x, Top = y, AutoSize = w == 0, ForeColor = dim ? Dim : Fg, BackColor = Color.Transparent };
            if (w > 0) { l.Width = w; l.Height = 22; }
            if (size > 0 || bold) l.Font = new Font("Segoe UI", size > 0 ? size : 10f, bold ? FontStyle.Bold : FontStyle.Regular);
            parent.Controls.Add(l);
            return l;
        }

        public static Button Button(Control parent, string text, int x, int y, int w, int h, bool accent = false)
        {
            var b = new Button
            {
                Text = text, Left = x, Top = y, Width = w, Height = h, FlatStyle = FlatStyle.Flat,
                BackColor = accent ? Accent : Panel, ForeColor = Color.White, Cursor = Cursors.Hand,
            };
            b.FlatAppearance.BorderColor = accent ? Accent : Color.FromArgb(70, 82, 100);
            if (accent) b.Font = new Font("Segoe UI", 12f, FontStyle.Bold);
            parent.Controls.Add(b);
            return b;
        }
    }

    /// <summary>Asks for the parent password. Wrong guesses are slowed down.</summary>
    sealed class PasswordPrompt : Form
    {
        static int failures;
        static DateTime lockedUntil;
        readonly TextBox box;
        readonly Label status;
        readonly ParentalStore store;

        PasswordPrompt(ParentalStore store, string message)
        {
            this.store = store;
            Ui.Style(this, "Parent password", 380, 170);
            Ui.Label(this, message, 20, 18, 340);
            box = new TextBox { Left = 20, Top = 52, Width = 340, UseSystemPasswordChar = true };
            Controls.Add(box);
            status = Ui.Label(this, "", 20, 84, 340, true);
            status.ForeColor = Color.FromArgb(255, 138, 128);
            var ok = Ui.Button(this, "OK", 170, 120, 90, 32, true);
            ok.Font = Font;
            var cancel = Ui.Button(this, "Cancel", 270, 120, 90, 32);
            ok.Click += (s, e) => Try();
            cancel.Click += (s, e) => { DialogResult = DialogResult.Cancel; };
            AcceptButton = ok;
            CancelButton = cancel;
        }

        void Try()
        {
            if (DateTime.UtcNow < lockedUntil)
            {
                status.Text = $"Too many tries. Wait {(int)Math.Ceiling((lockedUntil - DateTime.UtcNow).TotalSeconds)} s.";
                return;
            }
            if (store.Verify(box.Text))
            {
                failures = 0;
                DialogResult = DialogResult.OK;
                return;
            }
            failures++;
            if (failures >= 3) lockedUntil = DateTime.UtcNow.AddSeconds(Math.Min(300, 10 * (failures - 2)));
            status.Text = "Wrong password.";
            box.SelectAll();
            box.Focus();
        }

        public static bool Ask(IWin32Window owner, ParentalStore store, string message)
        {
            if (!store.HasPassword) return true;
            using var p = new PasswordPrompt(store, message);
            return p.ShowDialog(owner) == DialogResult.OK;
        }
    }

    sealed class TextPrompt : Form
    {
        readonly TextBox box;
        TextPrompt(string title, string message, string initial)
        {
            Ui.Style(this, title, 380, 150);
            Ui.Label(this, message, 20, 18, 340);
            box = new TextBox { Left = 20, Top = 52, Width = 340, Text = initial, MaxLength = 30 };
            Controls.Add(box);
            var ok = Ui.Button(this, "OK", 170, 100, 90, 32, true);
            ok.Font = Font;
            var cancel = Ui.Button(this, "Cancel", 270, 100, 90, 32);
            ok.Click += (s, e) => DialogResult = DialogResult.OK;
            cancel.Click += (s, e) => DialogResult = DialogResult.Cancel;
            AcceptButton = ok; CancelButton = cancel;
        }

        public static string Ask(IWin32Window owner, string title, string message, string initial)
        {
            using var p = new TextPrompt(title, message, initial);
            return p.ShowDialog(owner) == DialogResult.OK ? p.box.Text.Trim() : null;
        }
    }

    /// <summary>Password, daily time limit and allowed-hours settings.</summary>
    sealed class ParentSettingsForm : Form
    {
        readonly ParentalStore store;
        readonly TextBox pw1, pw2;
        readonly CheckBox requirePw, limitOn, windowOn;
        readonly NumericUpDown weekday, weekend;
        readonly DateTimePicker from, to;
        readonly Label usage;

        public ParentSettingsForm(ParentalStore store)
        {
            this.store = store;
            var d = store.D;
            Ui.Style(this, "Blockhaven - Parent controls", 470, 560);

            int y = 16;
            Ui.Label(this, "Password", 20, y, 0, false, 12, true); y += 30;
            Ui.Label(this, store.HasPassword ? "A password is set. Fill in both boxes only to change it." : "Set a password so only you can change these settings.", 20, y, 430, true); y += 28;
            Ui.Label(this, "New password", 20, y + 3, 130);
            pw1 = new TextBox { Left = 160, Top = y, Width = 290, UseSystemPasswordChar = true }; Controls.Add(pw1); y += 32;
            Ui.Label(this, "Repeat password", 20, y + 3, 130);
            pw2 = new TextBox { Left = 160, Top = y, Width = 290, UseSystemPasswordChar = true }; Controls.Add(pw2); y += 36;
            requirePw = Check("Ask for the password every time the game starts", 20, y, d.RequirePasswordToPlay); y += 40;

            Ui.Label(this, "Play time", 20, y, 0, false, 12, true); y += 30;
            limitOn = Check("Limit play time per day", 20, y, d.LimitEnabled); y += 30;
            Ui.Label(this, "School days (Mon-Fri), minutes", 40, y + 3, 230);
            weekday = Num(280, y, d.WeekdayMinutes); y += 32;
            Ui.Label(this, "Weekends (Sat-Sun), minutes", 40, y + 3, 230);
            weekend = Num(280, y, d.WeekendMinutes); y += 38;

            windowOn = Check("Only allow play between these times", 20, y, d.WindowEnabled); y += 30;
            Ui.Label(this, "From", 40, y + 3, 50);
            from = Time(95, y, d.WindowStartMin);
            Ui.Label(this, "to", 215, y + 3, 25);
            to = Time(245, y, d.WindowEndMin); y += 42;

            Ui.Label(this, "Today", 20, y, 0, false, 12, true); y += 30;
            usage = Ui.Label(this, "", 20, y, 430, true); y += 28;
            var bonus = Ui.Button(this, "Give 15 more minutes today", 20, y, 210, 32);
            var reset = Ui.Button(this, "Reset today's play time", 240, y, 210, 32);
            bonus.Click += (s, e) => { store.GrantBonus(15); store.Save(); RefreshUsage(); };
            reset.Click += (s, e) => { store.D.UsedSeconds = 0; store.Save(); RefreshUsage(); };
            y += 50;

            var remove = Ui.Button(this, "Remove password", 20, y, 150, 34);
            remove.Enabled = store.HasPassword;
            remove.Click += (s, e) =>
            {
                if (MessageBox.Show(this, "Remove the password? Anyone will be able to change these settings.", "Blockhaven", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes) return;
                store.ClearPassword();
                store.Save();
                requirePw.Checked = false;
                remove.Enabled = false;
            };
            var save = Ui.Button(this, "Save", 250, y, 95, 34, true);
            save.Font = Font;
            var cancel = Ui.Button(this, "Cancel", 355, y, 95, 34);
            save.Click += (s, e) => SaveClicked();
            cancel.Click += (s, e) => DialogResult = DialogResult.Cancel;
            CancelButton = cancel;
            RefreshUsage();
        }

        CheckBox Check(string text, int x, int y, bool on)
        {
            var c = new CheckBox { Text = text, Left = x, Top = y, Width = 430, Height = 24, Checked = on, ForeColor = Ui.Fg };
            Controls.Add(c);
            return c;
        }

        NumericUpDown Num(int x, int y, int v)
        {
            var n = new NumericUpDown { Left = x, Top = y, Width = 80, Minimum = 5, Maximum = 1440, Increment = 5, Value = Math.Clamp(v, 5, 1440) };
            Controls.Add(n);
            return n;
        }

        DateTimePicker Time(int x, int y, int minutes)
        {
            var t = new DateTimePicker { Left = x, Top = y, Width = 110, Format = DateTimePickerFormat.Custom, CustomFormat = "hh:mm tt", ShowUpDown = true, Value = DateTime.Today.AddMinutes(Math.Clamp(minutes, 0, 1439)) };
            Controls.Add(t);
            return t;
        }

        void RefreshUsage()
        {
            store.Normalize();
            string s = $"Played today: {(int)(store.D.UsedSeconds / 60)} min";
            if (store.D.BonusMinutes > 0) s += $"   |   Extra time given: {store.D.BonusMinutes} min";
            usage.Text = s;
        }

        void SaveClicked()
        {
            bool newPw = pw1.Text.Length > 0 || pw2.Text.Length > 0;
            if (newPw)
            {
                if (pw1.Text != pw2.Text) { Warn("The two passwords do not match."); return; }
                if (pw1.Text.Length < 4) { Warn("Please use at least 4 characters."); return; }
            }
            bool wantsProtection = requirePw.Checked || limitOn.Checked || windowOn.Checked;
            if (wantsProtection && !store.HasPassword && !newPw)
            {
                Warn("Set a password first, otherwise anyone could switch these limits off.");
                return;
            }
            if (newPw) store.SetPassword(pw1.Text);
            var d = store.D;
            d.RequirePasswordToPlay = requirePw.Checked;
            d.LimitEnabled = limitOn.Checked;
            d.WeekdayMinutes = (int)weekday.Value;
            d.WeekendMinutes = (int)weekend.Value;
            d.WindowEnabled = windowOn.Checked;
            d.WindowStartMin = (int)from.Value.TimeOfDay.TotalMinutes;
            d.WindowEndMin = (int)to.Value.TimeOfDay.TotalMinutes;
            store.Save();
            DialogResult = DialogResult.OK;
        }

        void Warn(string m) => MessageBox.Show(this, m, "Blockhaven", MessageBoxButtons.OK, MessageBoxIcon.Information);
    }

    /// <summary>Start screen: pick a world, mode and graphics level; shows the remaining play time.</summary>
    sealed class LauncherForm : Form
    {
        readonly ParentalStore store;
        readonly AppConfig cfg;
        readonly SystemProfile sys;
        readonly ComboBox worlds, quality, mode;
        readonly Label timeLeft;
        readonly Timer timer;

        public string WorldName => worlds.Text;
        public bool Creative => mode.SelectedIndex == 1;
        public bool AutoQuality => quality.SelectedIndex == 0;
        public Quality ChosenQuality => quality.SelectedIndex == 0 ? sys.Recommended : (Quality)(quality.SelectedIndex - 1);

        public LauncherForm(ParentalStore store, AppConfig cfg, SystemProfile sys, string notice)
        {
            this.store = store; this.cfg = cfg; this.sys = sys;
            Ui.Style(this, "Blockhaven", 520, 500);
            MinimizeBox = true;

            Ui.Label(this, "BLOCKHAVEN", 24, 16, 0, false, 24, true).ForeColor = Ui.Accent;
            Ui.Label(this, "Dig, build and explore a world made of blocks.", 28, 62, 0, true);

            int y = 104;
            Ui.Label(this, "World", 28, y + 4, 90);
            worlds = Combo(120, y, 250);
            var newWorld = Ui.Button(this, "New world", 380, y - 1, 112, 30);
            newWorld.Click += (s, e) => NewWorld();
            y += 42;
            Ui.Label(this, "Mode", 28, y + 4, 90);
            mode = Combo(120, y, 372);
            mode.Items.AddRange(new object[] { "Survival - gather blocks, craft, watch your health", "Creative - unlimited blocks and flying" });
            mode.SelectedIndex = cfg.Creative ? 1 : 0;
            y += 42;
            Ui.Label(this, "Graphics", 28, y + 4, 90);
            quality = Combo(120, y, 372);
            quality.Items.AddRange(new object[] { $"Auto (best for this PC: {sys.Recommended})", "Low", "Medium", "High", "Ultra" });
            int qi = Array.IndexOf(new[] { "Auto", "Low", "Medium", "High", "Ultra" }, cfg.Quality);
            quality.SelectedIndex = qi < 0 ? 0 : qi;
            y += 38;
            Ui.Label(this, sys.Summary, 28, y, 470, true, 8.5f); y += 20;
            Ui.Label(this, $"Auto picked {sys.Recommended} because of: {sys.Reason}.", 28, y, 470, true, 8.5f); y += 20;
            if (sys.DiskTooLowToSave)
                Ui.Label(this, "Disk is almost full: worlds will NOT be saved.", 28, y, 470, false, 8.5f).ForeColor = Color.FromArgb(255, 138, 128);
            y += 26;

            timeLeft = Ui.Label(this, "", 28, y, 470, false, 11, true); timeLeft.Height = 26; y += 30;
            var note = Ui.Label(this, notice ?? "", 28, y, 470, false);
            note.ForeColor = Color.FromArgb(255, 213, 79);
            y += 34;

            var play = Ui.Button(this, "PLAY", 28, y, 464, 52, true);
            play.Click += (s, e) => PlayClicked();
            y += 66;
            var parent = Ui.Button(this, "Parent controls...", 28, y, 226, 34);
            parent.Click += (s, e) => OpenParent();
            var quit = Ui.Button(this, "Quit", 266, y, 226, 34);
            quit.Click += (s, e) => DialogResult = DialogResult.Cancel;
            AcceptButton = play;

            LoadWorlds(cfg.LastWorld);
            timer = new Timer { Interval = 1000 };
            timer.Tick += (s, e) => RefreshTime();
            timer.Start();
            RefreshTime();
            FormClosed += (s, e) => timer.Dispose();
        }

        ComboBox Combo(int x, int y, int w)
        {
            var c = new ComboBox { Left = x, Top = y, Width = w, DropDownStyle = ComboBoxStyle.DropDownList, FlatStyle = FlatStyle.Flat, BackColor = Ui.Panel, ForeColor = Ui.Fg };
            Controls.Add(c);
            return c;
        }

        void LoadWorlds(string select)
        {
            worlds.Items.Clear();
            try
            {
                Directory.CreateDirectory(AppPaths.Worlds);
                foreach (var d in Directory.GetDirectories(AppPaths.Worlds).Select(Path.GetFileName).OrderBy(n => n))
                    worlds.Items.Add(d);
            }
            catch { }
            if (worlds.Items.Count == 0) worlds.Items.Add("My World");
            int i = select == null ? -1 : worlds.Items.IndexOf(select);
            worlds.SelectedIndex = i < 0 ? 0 : i;
        }

        void NewWorld()
        {
            string name = TextPrompt.Ask(this, "New world", "Name your new world:", "World " + (worlds.Items.Count + 1));
            if (string.IsNullOrWhiteSpace(name)) return;
            foreach (char c in Path.GetInvalidFileNameChars()) name = name.Replace(c, '_');
            name = name.Trim().TrimEnd('.');
            if (name.Length == 0) return;
            if (!worlds.Items.Contains(name)) worlds.Items.Add(name);
            worlds.SelectedItem = name;
        }

        void RefreshTime()
        {
            double rem = store.RemainingSeconds();
            if (!store.AnyRestriction) { timeLeft.Text = "No play-time limit is set."; timeLeft.ForeColor = Ui.Dim; }
            else if (rem <= 0) { timeLeft.Text = "No play time left right now."; timeLeft.ForeColor = Color.FromArgb(255, 138, 128); }
            else
            {
                timeLeft.Text = double.IsInfinity(rem) ? "Play time is open right now." : $"Play time left: {Fmt(rem)}";
                timeLeft.ForeColor = Ui.Fg;
            }
        }

        static string Fmt(double sec)
        {
            int m = (int)Math.Ceiling(sec / 60);
            return m >= 60 ? $"{m / 60} h {m % 60} min" : $"{m} min";
        }

        void OpenParent()
        {
            if (!PasswordPrompt.Ask(this, store, "Enter the parent password to open the controls.")) return;
            using var f = new ParentSettingsForm(store);
            f.ShowDialog(this);
            RefreshTime();
        }

        void PlayClicked()
        {
            if (!store.CanPlayNow(out string reason))
            {
                MessageBox.Show(this, reason + "\n\nA parent can add extra time in Parent controls.", "Blockhaven", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            if (store.D.RequirePasswordToPlay && !PasswordPrompt.Ask(this, store, "Enter the parent password to start the game.")) return;
            cfg.LastWorld = WorldName;
            cfg.Creative = Creative;
            cfg.Quality = new[] { "Auto", "Low", "Medium", "High", "Ultra" }[quality.SelectedIndex];
            cfg.Save();
            DialogResult = DialogResult.OK;
        }
    }
}
