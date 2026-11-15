using System;
using System.IO;
using System.Threading;
using System.Windows.Forms;

namespace Blockhaven
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            Application.SetHighDpiMode(HighDpiMode.SystemAware);
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            // Developer check: "--selftest <folder> [Low|Medium|High|Ultra]" renders a few frames to PNG and exits.
            if (args.Length >= 2 && args[0] == "--selftest")
            {
                Directory.CreateDirectory(args[1]);
                try
                {
                    var q = args.Length >= 3 ? Enum.Parse<Quality>(args[2], true) : Quality.Medium;
                    var o = new GameOptions
                    {
                        Graphics = GraphicsSettings.For(q, Environment.ProcessorCount),
                        WorldDir = Path.Combine(args[1], "world"),
                        Creative = args.Length >= 4 && args[3] == "creative",
                        SelfTestDir = args[1],
                    };
                    using var g = new Game(o);
                    g.Run();
                }
                catch (Exception ex) { File.WriteAllText(Path.Combine(args[1], "error.txt"), ex.ToString()); }
                return;
            }

            using var single = new Mutex(true, "Blockhaven.SingleInstance", out bool first);
            if (!first)
            {
                MessageBox.Show("Blockhaven is already running.", "Blockhaven", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            Directory.CreateDirectory(AppPaths.Worlds);
            var parental = ParentalStore.Load();
            var cfg = AppConfig.Load();
            string notice = null;

            while (true)
            {
                var sys = SystemProfile.Detect(AppPaths.Root);
                GameOptions opt;
                using (var launcher = new LauncherForm(parental, cfg, sys, notice))
                {
                    if (launcher.ShowDialog() != DialogResult.OK) break;
                    opt = new GameOptions
                    {
                        Graphics = GraphicsSettings.For(launcher.ChosenQuality, sys.Cores),
                        AutoQuality = launcher.AutoQuality,
                        Parental = parental,
                        WorldDir = Path.Combine(AppPaths.Worlds, launcher.WorldName),
                        Creative = launcher.Creative,
                        SavingEnabled = !sys.DiskTooLowToSave,
                    };
                }

                notice = null;
                try
                {
                    using var game = new Game(opt);
                    game.Run();
                    if (game.TimeUp) notice = "Time's up! Your world was saved.";
                }
                catch (Exception ex)
                {
                    try { File.WriteAllText(Path.Combine(AppPaths.Root, "crash.txt"), ex.ToString()); } catch { }
                    MessageBox.Show("The game had to stop:\n\n" + ex.Message + "\n\nBlockhaven needs a graphics driver with OpenGL 3.3 or newer.",
                        "Blockhaven", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
                parental.Save();
            }
        }
    }
}
