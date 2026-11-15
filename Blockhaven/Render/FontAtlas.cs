using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Text;
using System.Runtime.InteropServices;

namespace Blockhaven
{
    /// <summary>Rasterises printable ASCII into one bitmap (16 columns x 6 rows) using GDI+.</summary>
    public static class FontAtlas
    {
        public const int Cols = 16, Rows = 6;

        public static byte[] Build(out int width, out int height, out int cellW, out int cellH)
        {
            using var font = new Font("Consolas", 30, FontStyle.Bold, GraphicsUnit.Pixel);
            using var probe = new Bitmap(8, 8, PixelFormat.Format32bppArgb);
            using (var g = Graphics.FromImage(probe))
            {
                var sz = g.MeasureString("M", font, 1000, StringFormat.GenericTypographic);
                cellW = (int)Math.Ceiling(sz.Width) + 2;
                cellH = (int)Math.Ceiling(font.GetHeight(g)) + 2;
            }
            width = cellW * Cols;
            height = cellH * Rows;
            using var bmp = new Bitmap(width, height, PixelFormat.Format32bppArgb);
            using (var g = Graphics.FromImage(bmp))
            {
                g.Clear(Color.Transparent);
                g.TextRenderingHint = TextRenderingHint.AntiAlias;
                for (int c = 32; c < 127; c++)
                {
                    int i = c - 32;
                    g.DrawString(((char)c).ToString(), font, Brushes.White, (i % Cols) * cellW + 1, (i / Cols) * cellH + 1, StringFormat.GenericTypographic);
                }
            }
            return ToBgra(bmp);
        }

        public static byte[] ToBgra(Bitmap bmp)
        {
            var data = bmp.LockBits(new Rectangle(0, 0, bmp.Width, bmp.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
            var bytes = new byte[bmp.Width * bmp.Height * 4];
            for (int y = 0; y < bmp.Height; y++)
                Marshal.Copy(data.Scan0 + y * data.Stride, bytes, y * bmp.Width * 4, bmp.Width * 4);
            bmp.UnlockBits(data);
            return bytes;
        }

        /// <summary>Saves bottom-up BGRA pixels (as returned by glReadPixels) to a PNG.</summary>
        public static void SavePng(string path, byte[] bgra, int w, int h)
        {
            using var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb);
            var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
            for (int y = 0; y < h; y++)
            {
                int src = (h - 1 - y) * w * 4;
                for (int x = 0; x < w; x++) bgra[src + x * 4 + 3] = 255;
                Marshal.Copy(bgra, src, data.Scan0 + y * data.Stride, w * 4);
            }
            bmp.UnlockBits(data);
            bmp.Save(path, ImageFormat.Png);
        }
    }
}
