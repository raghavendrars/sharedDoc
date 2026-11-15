using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Win32;

namespace Blockhaven
{
    public sealed class ParentalData
    {
        public int Revision { get; set; }
        public string PwHash { get; set; }
        public string PwSalt { get; set; }
        public bool RequirePasswordToPlay { get; set; }
        public bool LimitEnabled { get; set; }
        public int WeekdayMinutes { get; set; } = 60;
        public int WeekendMinutes { get; set; } = 120;
        public bool WindowEnabled { get; set; }
        public int WindowStartMin { get; set; } = 9 * 60;
        public int WindowEndMin { get; set; } = 19 * 60;
        public string UsageDay { get; set; }
        public double UsedSeconds { get; set; }
        public int BonusMinutes { get; set; }
        public long OverrideUntilTicks { get; set; }
    }

    /// <summary>
    /// Parental settings and today's play time. Stored encrypted (Windows DPAPI) in both a file and the
    /// registry, so deleting one copy does not reset the limits.
    /// </summary>
    public sealed class ParentalStore
    {
        public ParentalData D = new ParentalData();

        static readonly byte[] Entropy = Encoding.UTF8.GetBytes("Blockhaven.Parental.v1");
        const string RegPath = @"Software\Blockhaven";
        static string FilePath => Path.Combine(AppPaths.Root, "parental.bin");

        public static ParentalStore Load()
        {
            var s = new ParentalStore();
            ParentalData a = null, b = null;
            try { if (File.Exists(FilePath)) a = Decode(File.ReadAllBytes(FilePath)); } catch { }
            try
            {
                using var k = Registry.CurrentUser.OpenSubKey(RegPath);
                if (k?.GetValue("P") is byte[] raw) b = Decode(raw);
            }
            catch { }
            if (a != null && b != null) s.D = a.Revision >= b.Revision ? a : b;
            else s.D = a ?? b ?? new ParentalData();
            s.Normalize();
            return s;
        }

        static ParentalData Decode(byte[] raw)
        {
            var json = ProtectedData.Unprotect(raw, Entropy, DataProtectionScope.CurrentUser);
            return JsonSerializer.Deserialize<ParentalData>(json);
        }

        public void Save()
        {
            D.Revision++;
            byte[] raw;
            try { raw = ProtectedData.Protect(JsonSerializer.SerializeToUtf8Bytes(D), Entropy, DataProtectionScope.CurrentUser); }
            catch { return; }
            try
            {
                Directory.CreateDirectory(AppPaths.Root);
                File.WriteAllBytes(FilePath, raw);
            }
            catch { }
            try
            {
                using var k = Registry.CurrentUser.CreateSubKey(RegPath);
                k?.SetValue("P", raw, RegistryValueKind.Binary);
            }
            catch { }
        }

        // ---------- password ----------

        public bool HasPassword => !string.IsNullOrEmpty(D.PwHash);

        static byte[] Derive(string pw, byte[] salt) =>
            Rfc2898DeriveBytes.Pbkdf2(pw ?? "", salt, 150_000, HashAlgorithmName.SHA256, 32);

        public void SetPassword(string pw)
        {
            var salt = RandomNumberGenerator.GetBytes(16);
            D.PwSalt = Convert.ToBase64String(salt);
            D.PwHash = Convert.ToBase64String(Derive(pw, salt));
        }

        public void ClearPassword()
        {
            D.PwHash = null; D.PwSalt = null;
            D.RequirePasswordToPlay = false;
        }

        public bool Verify(string pw)
        {
            if (!HasPassword) return true;
            try
            {
                var h = Derive(pw, Convert.FromBase64String(D.PwSalt));
                return CryptographicOperations.FixedTimeEquals(h, Convert.FromBase64String(D.PwHash));
            }
            catch { return false; }
        }

        // ---------- play time ----------

        static string DayKey(DateTime d) => d.ToString("yyyy-MM-dd");

        /// <summary>Starts a fresh day when the date moves forward. A clock set backwards does not reset usage.</summary>
        public void Normalize()
        {
            string today = DayKey(DateTime.Now);
            if (D.UsageDay == null || string.CompareOrdinal(today, D.UsageDay) > 0)
            {
                D.UsageDay = today;
                D.UsedSeconds = 0;
                D.BonusMinutes = 0;
            }
        }

        public int AllowedMinutesToday
        {
            get
            {
                var dow = DateTime.Now.DayOfWeek;
                bool weekend = dow == DayOfWeek.Saturday || dow == DayOfWeek.Sunday;
                return (weekend ? D.WeekendMinutes : D.WeekdayMinutes) + D.BonusMinutes;
            }
        }

        public bool AnyRestriction => D.LimitEnabled || D.WindowEnabled;

        /// <summary>Seconds of play left right now; PositiveInfinity when nothing restricts play.</summary>
        public double RemainingSeconds()
        {
            Normalize();
            double rem = double.PositiveInfinity;
            if (D.LimitEnabled) rem = Math.Max(0, AllowedMinutesToday * 60.0 - D.UsedSeconds);
            if (D.WindowEnabled)
            {
                var now = DateTime.Now;
                double overrideLeft = (new DateTime(Math.Max(0, D.OverrideUntilTicks)) - now).TotalSeconds;
                double m = now.TimeOfDay.TotalMinutes;
                int s = D.WindowStartMin, e = D.WindowEndMin;
                double windowLeft;
                if (s == e) windowLeft = double.PositiveInfinity;
                else if (s < e) windowLeft = (m >= s && m < e) ? (e - m) * 60 : 0;
                else windowLeft = (m >= s) ? (e + 1440 - m) * 60 : (m < e ? (e - m) * 60 : 0);
                rem = Math.Min(rem, Math.Max(windowLeft, Math.Max(0, overrideLeft)));
            }
            return rem;
        }

        public bool CanPlayNow(out string reason)
        {
            reason = null;
            if (RemainingSeconds() > 0) return true;
            bool limitHit = D.LimitEnabled && AllowedMinutesToday * 60.0 - D.UsedSeconds <= 0;
            reason = limitHit
                ? "Today's play time is used up. See you tomorrow!"
                : $"Play is only allowed between {FormatMin(D.WindowStartMin)} and {FormatMin(D.WindowEndMin)}.";
            return false;
        }

        public static string FormatMin(int m) => DateTime.Today.AddMinutes(m).ToString("h:mm tt");

        public void AddUsage(double seconds)
        {
            Normalize();
            D.UsedSeconds += seconds;
        }

        public void GrantBonus(int minutes)
        {
            Normalize();
            D.BonusMinutes += minutes;
            var now = DateTime.Now;
            var baseT = new DateTime(Math.Max(now.Ticks, D.OverrideUntilTicks));
            D.OverrideUntilTicks = baseT.AddMinutes(minutes).Ticks;
        }
    }

    public static class AppPaths
    {
        public static string Root => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Blockhaven");
        public static string Worlds => Path.Combine(Root, "worlds");
    }

    public sealed class AppConfig
    {
        public string LastWorld { get; set; }
        public string Quality { get; set; } = "Auto";
        public bool Creative { get; set; }

        static string FilePath => Path.Combine(AppPaths.Root, "config.json");

        public static AppConfig Load()
        {
            try { if (File.Exists(FilePath)) return JsonSerializer.Deserialize<AppConfig>(File.ReadAllText(FilePath)) ?? new AppConfig(); }
            catch { }
            return new AppConfig();
        }

        public void Save()
        {
            try
            {
                Directory.CreateDirectory(AppPaths.Root);
                File.WriteAllText(FilePath, JsonSerializer.Serialize(this));
            }
            catch { }
        }
    }
}
