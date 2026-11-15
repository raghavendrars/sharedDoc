using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;

namespace InterviewDesk
{
    public sealed class QuestionSet
    {
        public List<Question> Questions = new List<Question>();
        public bool FromInternet;
        public string Level = "";
    }

    /// <summary>
    /// Downloads multiple-choice questions from the Open Trivia Database (opentdb.com) and picks a set whose
    /// difficulty matches the candidate's years of experience. Downloads are kept in questions.csv, which is
    /// also the fallback when the internet is down.
    /// </summary>
    public static class QuestionService
    {
        public static readonly (string name, int id)[] Topics =
        {
            ("Computers", 18), ("Mathematics", 19), ("Science and Nature", 17), ("Gadgets", 30), ("General Knowledge", 9),
        };

        static readonly HttpClient Http = CreateClient();

        static HttpClient CreateClient()
        {
            var c = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };
            c.DefaultRequestHeaders.UserAgent.ParseAdd("InterviewDesk/1.0");
            return c;
        }

        /// <summary>Experience band and its easy/medium/hard mix in percent.</summary>
        public static (string name, int easy, int medium, int hard) LevelFor(int years)
        {
            if (years <= 1) return ("Fresher (0-1 yrs)", 80, 20, 0);
            if (years <= 4) return ("Junior (2-4 yrs)", 40, 50, 10);
            if (years <= 8) return ("Mid-level (5-8 yrs)", 10, 50, 40);
            return ("Senior (9+ yrs)", 0, 30, 70);
        }

        static async Task<List<Question>> Download(string topic)
        {
            int id = Topics.FirstOrDefault(t => t.name == topic).id;
            if (id == 0) id = 18;
            string json = await Http.GetStringAsync($"https://opentdb.com/api.php?amount=50&category={id}&type=multiple").ConfigureAwait(false);
            var list = new List<Question>();
            using var doc = JsonDocument.Parse(json);
            if (!doc.RootElement.TryGetProperty("results", out var results)) return list;
            foreach (var r in results.EnumerateArray())
            {
                var q = new Question
                {
                    Topic = topic,
                    Difficulty = (r.GetProperty("difficulty").GetString() ?? "").ToLowerInvariant(),
                    Text = WebUtility.HtmlDecode(r.GetProperty("question").GetString() ?? "").Trim(),
                    Correct = WebUtility.HtmlDecode(r.GetProperty("correct_answer").GetString() ?? "").Trim(),
                    Wrong = r.GetProperty("incorrect_answers").EnumerateArray().Select(a => WebUtility.HtmlDecode(a.GetString() ?? "").Trim()).Where(a => a.Length > 0).ToArray(),
                    Source = "opentdb.com",
                };
                if (q.Valid) list.Add(q);
            }
            return list;
        }

        static void Shuffle<T>(List<T> list)
        {
            for (int i = list.Count - 1; i > 0; i--)
            {
                int j = Random.Shared.Next(i + 1);
                (list[i], list[j]) = (list[j], list[i]);
            }
        }

        public static async Task<QuestionSet> GetAsync(Store store, Settings settings, int years)
        {
            var set = new QuestionSet();
            List<Question> pool;
            try
            {
                var fresh = await Download(settings.Topic).ConfigureAwait(false);
                set.FromInternet = fresh.Count > 0;
                pool = await Task.Run(() => store.MergeQuestions(fresh)).ConfigureAwait(false);
            }
            catch
            {
                pool = await Task.Run(() => store.LoadQuestions()).ConfigureAwait(false);
            }
            pool = pool.Where(q => string.Equals(q.Topic, settings.Topic, StringComparison.OrdinalIgnoreCase)).ToList();
            Shuffle(pool);

            var (name, easy, medium, hard) = LevelFor(years);
            set.Level = name;
            int n = settings.QuestionCount;
            int nHard = (int)Math.Round(n * hard / 100.0), nMed = (int)Math.Round(n * medium / 100.0);
            if (nHard + nMed > n) nMed = n - nHard;
            int nEasy = n - nHard - nMed;

            var picked = new List<Question>();
            void Take(string difficulty, int count) => picked.AddRange(pool.Where(q => q.Difficulty == difficulty).Take(count));
            Take("easy", nEasy); Take("medium", nMed); Take("hard", nHard);
            // If one difficulty ran short, top up from whatever is left.
            if (picked.Count < n) picked.AddRange(pool.Except(picked).Take(n - picked.Count));

            string[] order = { "easy", "medium", "hard" };
            set.Questions = picked.OrderBy(q => Array.IndexOf(order, q.Difficulty)).ToList();
            return set;
        }
    }
}
