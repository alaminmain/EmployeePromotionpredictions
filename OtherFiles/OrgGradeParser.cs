using System;
using System.Globalization;
using System.IO;
using System.Linq;
using CsvHelper;
using CsvHelper.Configuration;

namespace OrgGradeParser
{
    public class OrgGrade
    {
        public int SL_No { get; set; }
        public int GradeNo { get; set; }
        public int DesgNo { get; set; }
        public string DesignationName { get; set; }
        public string DesgType { get; set; }
        public int? TotalPost { get; set; }
        public int? YearsNeeded { get; set; }
        public string PromotionFromPostSL { get; set; }
        public string PreviousPostPercentise { get; set; }
    }

    public class OrgGradeMap : ClassMap<OrgGrade>
    {
        public OrgGradeMap()
        {
            Map(m => m.SL_No).Name("SL_No");
            Map(m => m.GradeNo).Name("grade_no");
            Map(m => m.DesgNo).Name("desg_no");
            Map(m => m.DesignationName).Name("desg_nm");
            Map(m => m.DesgType).Name("desg_type");
            Map(m => m.TotalPost).Name("TotalPost");
            Map(m => m.YearsNeeded).Name("YearNeedtobepromoted");
            Map(m => m.PromotionFromPostSL).Name("PromotionFromPostSL");
            Map(m => m.PreviousPostPercentise).Name("PreviousPostPercentise");
        }
    }

    class Program
    {
        static void Main(string[] args)
        {
            var csvPath = @"E:\Project\TestProjects\EMPPromotion\OrgGrade.csv";
            var outputPath = @"E:\Project\TestProjects\EMPPromotion\OrgGrade_Report.txt";

            using (var reader = new StreamReader(csvPath))
            using (var csv = new CsvReader(reader, CultureInfo.InvariantCulture))
            using (var writer = new StreamWriter(outputPath))
            {
                csv.Context.RegisterClassMap<OrgGradeMap>();
                var records = csv.GetRecords<OrgGrade>().OrderBy(o => o.GradeNo).ThenBy(o => o.SL_No).ToList();

                writer.WriteLine("=".PadRight(120, '='));
                writer.WriteLine("ORGANIZATIONAL GRADE STRUCTURE - PROMOTION RULES");
                writer.WriteLine("=".PadRight(120, '='));
                writer.WriteLine($"\nTotal Posts Defined: {records.Count}\n");

                writer.WriteLine($"{"SL",-4} {"Grade",-6} {"Desg",-5} {"Designation",-35} {"Type",-10} {"Posts",-6} {"Years",-6} {"Promoted From (SL)",-20}");
                writer.WriteLine("-".PadRight(120, '-'));

                foreach (var post in records)
                {
                    writer.WriteLine($"{post.SL_No,-4} {post.GradeNo,-6} {post.DesgNo,-5} {Truncate(post.DesignationName, 34),-35} " +
                                   $"{Truncate(post.DesgType, 9),-10} {post.TotalPost?.ToString() ?? "N/A",-6} " +
                                   $"{post.YearsNeeded?.ToString() ?? "N/A",-6} {Truncate(post.PromotionFromPostSL, 19),-20}");
                }

                writer.WriteLine("\n" + "=".PadRight(120, '='));
                writer.WriteLine("\nGRADE HIERARCHY (Top to Bottom):");
                writer.WriteLine("=".PadRight(120, '='));

                var gradeGroups = records.GroupBy(r => r.GradeNo).OrderBy(g => g.Key);
                foreach (var gradeGroup in gradeGroups)
                {
                    writer.WriteLine($"\n--- GRADE {gradeGroup.Key} ---");
                    foreach (var post in gradeGroup)
                    {
                        writer.WriteLine($"  [{post.SL_No}] {post.DesignationName} (Posts: {post.TotalPost}, Years: {post.YearsNeeded}, From: {post.PromotionFromPostSL ?? "N/A"})");
                    }
                }
            }

            Console.WriteLine($"Report generated: {outputPath}");
            Console.WriteLine("\nPress any key to view the report...");
            Console.ReadKey();

            // Display the report
            Console.Clear();
            Console.WriteLine(File.ReadAllText(outputPath));
        }

        static string Truncate(string value, int maxLength)
        {
            if (string.IsNullOrEmpty(value)) return "";
            return value.Length <= maxLength ? value : value.Substring(0, maxLength - 2) + "..";
        }
    }
}
