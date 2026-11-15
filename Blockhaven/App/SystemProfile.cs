using System;
using System.IO;
using System.Runtime.InteropServices;

namespace Blockhaven
{
    public enum Quality { Low = 0, Medium = 1, High = 2, Ultra = 3 }

    public sealed class GraphicsSettings
    {
        public Quality Quality;
        public int RenderDistance;     // in chunks
        public bool AO;                // smooth corner shading
        public int Msaa;               // anti-aliasing samples
        public bool Clouds;
        public int UploadsPerFrame;
        public int Workers;

        public static GraphicsSettings For(Quality q, int cores)
        {
            int workers = Math.Clamp(cores - 2, 1, 6);
            switch (q)
            {
                case Quality.Low:
                    return new GraphicsSettings { Quality = q, RenderDistance = 5, AO = false, Msaa = 0, Clouds = false, UploadsPerFrame = 2, Workers = Math.Min(workers, 2) };
                case Quality.Medium:
                    return new GraphicsSettings { Quality = q, RenderDistance = 8, AO = true, Msaa = 0, Clouds = true, UploadsPerFrame = 3, Workers = Math.Min(workers, 3) };
                case Quality.High:
                    return new GraphicsSettings { Quality = q, RenderDistance = 12, AO = true, Msaa = 2, Clouds = true, UploadsPerFrame = 4, Workers = workers };
                default:
                    return new GraphicsSettings { Quality = q, RenderDistance = 16, AO = true, Msaa = 4, Clouds = true, UploadsPerFrame = 6, Workers = workers };
            }
        }
    }

    /// <summary>Reads RAM, CPU and free disk space and picks a graphics preset that fits the machine.</summary>
    public sealed class SystemProfile
    {
        public long TotalRamMB, FreeRamMB, FreeDiskMB;
        public int Cores;
        public Quality Recommended;
        public string Reason;
        public bool DiskTooLowToSave;

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
        struct MEMORYSTATUSEX
        {
            public uint dwLength, dwMemoryLoad;
            public ulong ullTotalPhys, ullAvailPhys, ullTotalPageFile, ullAvailPageFile, ullTotalVirtual, ullAvailVirtual, ullAvailExtendedVirtual;
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        static extern bool GlobalMemoryStatusEx(ref MEMORYSTATUSEX lpBuffer);

        public static SystemProfile Detect(string dataDir)
        {
            var s = new SystemProfile { Cores = Environment.ProcessorCount };
            try
            {
                var m = new MEMORYSTATUSEX { dwLength = (uint)Marshal.SizeOf<MEMORYSTATUSEX>() };
                if (GlobalMemoryStatusEx(ref m))
                {
                    s.TotalRamMB = (long)(m.ullTotalPhys >> 20);
                    s.FreeRamMB = (long)(m.ullAvailPhys >> 20);
                }
            }
            catch { }
            if (s.TotalRamMB <= 0)
            {
                s.TotalRamMB = GC.GetGCMemoryInfo().TotalAvailableMemoryBytes >> 20;
                s.FreeRamMB = s.TotalRamMB / 2;
            }
            try
            {
                var drive = new DriveInfo(Path.GetPathRoot(Path.GetFullPath(dataDir)));
                s.FreeDiskMB = drive.AvailableFreeSpace >> 20;
            }
            catch { s.FreeDiskMB = 100_000; }

            // RAM decides the starting tier.
            Quality q = s.TotalRamMB < 6_000 ? Quality.Low
                      : s.TotalRamMB < 12_000 ? Quality.Medium
                      : s.TotalRamMB < 24_000 ? Quality.High
                      : Quality.Ultra;
            string why = $"{s.TotalRamMB / 1024.0:0.#} GB RAM";

            if (s.FreeRamMB < 1_500 && q > Quality.Low) { q--; why += ", little free memory right now"; }
            if (s.Cores <= 2 && q > Quality.Low) { q = Quality.Low; why += ", 2 or fewer CPU cores"; }
            else if (s.Cores <= 4 && q > Quality.Medium) { q = Quality.Medium; why += ", 4 or fewer CPU cores"; }

            // Free disk space caps the tier: bigger view distances explore and save far more world data.
            if (s.FreeDiskMB < 1_000) { if (q > Quality.Low) q = Quality.Low; why += ", under 1 GB free disk"; }
            else if (s.FreeDiskMB < 5_000) { if (q > Quality.Medium) q = Quality.Medium; why += ", under 5 GB free disk"; }
            s.DiskTooLowToSave = s.FreeDiskMB < 200;

            s.Recommended = q;
            s.Reason = why;
            return s;
        }

        public string Summary =>
            $"RAM {TotalRamMB / 1024.0:0.#} GB ({FreeRamMB / 1024.0:0.#} GB free)  |  {Cores} CPU threads  |  Disk free {FreeDiskMB / 1024.0:0.#} GB";
    }
}
