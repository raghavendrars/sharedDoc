using System;
using System.Drawing;
using System.IO;
using System.Windows.Forms;

namespace InterviewDesk
{
    static class Ui
    {
        public static readonly Color Bg = Color.FromArgb(246, 247, 250);
        public static readonly Color Card = Color.White;
        public static readonly Color Fg = Color.FromArgb(24, 32, 44);
        public static readonly Color Dim = Color.FromArgb(100, 110, 125);
        public static readonly Color Accent = Color.FromArgb(37, 99, 235);
        public static readonly Color Good = Color.FromArgb(22, 135, 72);
        public static readonly Color Bad = Color.FromArgb(200, 50, 50);

        public static void Style(Form f, string title, int w, int h, bool dialog = true)
        {
            f.Text = title;
            f.Font = new Font("Segoe UI", 10f);
            f.AutoScaleMode = AutoScaleMode.Dpi;
            f.ClientSize = new Size(w, h);
            f.BackColor = Bg;
            f.ForeColor = Fg;
            f.StartPosition = FormStartPosition.CenterScreen;
            if (dialog)
            {
                f.FormBorderStyle = FormBorderStyle.FixedDialog;
                f.MaximizeBox = false;
                f.MinimizeBox = false;
                f.ShowInTaskbar = false;
            }
        }

        public static Label Label(Control p, string text, int x, int y, int w = 0, int h = 24, float size = 10f, bool bold = false, Color? color = null)
        {
            var l = new Label { Text = text, Left = x, Top = y, AutoSize = w == 0, ForeColor = color ?? Fg, BackColor = Color.Transparent, Font = new Font("Segoe UI", size, bold ? FontStyle.Bold : FontStyle.Regular) };
            if (w > 0) { l.Width = w; l.Height = h; }
            p.Controls.Add(l);
            return l;
        }

        public static Button Button(Control p, string text, int x, int y, int w, int h = 36, bool primary = false)
        {
            var b = new Button { Text = text, Left = x, Top = y, Width = w, Height = h, FlatStyle = FlatStyle.Flat, BackColor = primary ? Accent : Card, ForeColor = primary ? Color.White : Fg, Cursor = Cursors.Hand };
            b.FlatAppearance.BorderColor = primary ? Accent : Color.FromArgb(205, 210, 220);
            if (primary) b.Font = new Font("Segoe UI", 10f, FontStyle.Bold);
            p.Controls.Add(b);
            return b;
        }

        public static TextBox Text(Control p, int x, int y, int w, bool password = false)
        {
            var t = new TextBox { Left = x, Top = y, Width = w, UseSystemPasswordChar = password, BorderStyle = BorderStyle.FixedSingle };
            p.Controls.Add(t);
            return t;
        }

        public static void Info(IWin32Window o, string m) => MessageBox.Show(o, m, "Interview Desk", MessageBoxButtons.OK, MessageBoxIcon.Information);
        public static void Error(IWin32Window o, string m) => MessageBox.Show(o, m, "Interview Desk", MessageBoxButtons.OK, MessageBoxIcon.Warning);
    }

    /// <summary>Asks for a password; with confirm=true it asks twice (for creating one).</summary>
    sealed class PasswordDialog : Form
    {
        readonly TextBox a, b;
        public string Value => a.Text;

        PasswordDialog(string title, string message, bool confirm)
        {
            Ui.Style(this, title, 420, confirm ? 240 : 190);
            Ui.Label(this, message, 20, 16, 380, 46, 10f, false, Ui.Dim);
            Ui.Label(this, "Password", 20, 72, 110);
            a = Ui.Text(this, 140, 70, 260, true);
            if (confirm)
            {
                Ui.Label(this, "Repeat", 20, 110, 110);
                b = Ui.Text(this, 140, 108, 260, true);
            }
            int y = confirm ? 178 : 132;
            var ok = Ui.Button(this, "OK", 200, y, 95, 36, true);
            var cancel = Ui.Button(this, "Cancel", 305, y, 95);
            ok.Click += (s, e) =>
            {
                if (confirm)
                {
                    if (a.Text.Length < 4) { Ui.Error(this, "Use at least 4 characters."); return; }
                    if (a.Text != b.Text) { Ui.Error(this, "The two passwords do not match."); return; }
                }
                DialogResult = DialogResult.OK;
            };
            cancel.Click += (s, e) => DialogResult = DialogResult.Cancel;
            AcceptButton = ok; CancelButton = cancel;
        }

        public static string Ask(IWin32Window owner, string title, string message, bool confirm = false)
        {
            using var d = new PasswordDialog(title, message, confirm);
            return d.ShowDialog(owner) == DialogResult.OK ? d.Value : null;
        }

        /// <summary>Admin login for the current data folder. Returns true when the password is right.</summary>
        public static bool Login(IWin32Window owner, Settings s)
        {
            for (int tries = 0; tries < 3; tries++)
            {
                string pw = Ask(owner, "Admin login", "Enter the admin password for this candidate folder.");
                if (pw == null) return false;
                if (s.Verify(pw)) return true;
                System.Threading.Thread.Sleep(800 * (tries + 1));
                Ui.Error(owner, "Wrong password.");
            }
            return false;
        }
    }

    /// <summary>Picks the shared candidate folder. Shown the first time the app runs on a computer, and from Admin settings.</summary>
    sealed class FolderDialog : Form
    {
        readonly TextBox path;
        public string Chosen => path.Text.Trim();

        internal FolderDialog(string current, bool firstRun)
        {
            Ui.Style(this, "Candidate data folder", 600, 250);
            Ui.Label(this, firstRun ? "Welcome - choose the candidate data folder" : "Change the candidate data folder", 20, 16, 0, 24, 13f, true);
            Ui.Label(this,
                "All candidates, answers and settings are kept as CSV files in this one folder. Point every computer " +
                "at the same shared folder (for example a network drive) so they all see the same candidates.",
                20, 52, 560, 62, 9.5f, false, Ui.Dim);
            path = Ui.Text(this, 20, 122, 450);
            path.Text = current ?? "";
            var browse = Ui.Button(this, "Browse...", 480, 119, 100, 30);
            browse.Click += (s, e) =>
            {
                using var fb = new FolderBrowserDialog { Description = "Choose the shared candidate folder", UseDescriptionForTitle = true, ShowNewFolderButton = true };
                if (Directory.Exists(path.Text)) fb.SelectedPath = path.Text;
                if (fb.ShowDialog(this) == DialogResult.OK) path.Text = fb.SelectedPath;
            };
            var ok = Ui.Button(this, "Use this folder", 300, 190, 160, 38, true);
            var cancel = Ui.Button(this, firstRun ? "Exit" : "Cancel", 470, 190, 110, 38);
            ok.Click += (s, e) => { if (Chosen.Length > 0) DialogResult = DialogResult.OK; };
            cancel.Click += (s, e) => DialogResult = DialogResult.Cancel;
            AcceptButton = ok; CancelButton = cancel;
        }

        /// <summary>
        /// Lets the user pick a folder and connects to it. A folder that already has an admin password asks for it;
        /// a new folder asks to create one. Returns the connected store, or null if cancelled.
        /// </summary>
        public static Store Choose(IWin32Window owner, string current, bool firstRun)
        {
            while (true)
            {
                string dir;
                using (var d = new FolderDialog(current, firstRun))
                {
                    if (d.ShowDialog(owner) != DialogResult.OK) return null;
                    dir = d.Chosen;
                }
                current = dir;
                try
                {
                    dir = Path.GetFullPath(dir);
                    Directory.CreateDirectory(dir);
                    var store = new Store(dir);
                    var settings = store.LoadSettings();
                    if (settings.HasPassword)
                    {
                        if (!PasswordDialog.Login(owner, settings)) continue;
                    }
                    else
                    {
                        string pw = PasswordDialog.Ask(owner, "Create admin password",
                            "This folder is new. Create the admin password that protects results and settings.", true);
                        if (pw == null) continue;
                        settings.SetPassword(pw);
                        store.SaveSettings(settings);
                    }
                    return store;
                }
                catch (Exception ex)
                {
                    Ui.Error(owner, "That folder cannot be used:\n\n" + ex.Message);
                }
            }
        }
    }
}
