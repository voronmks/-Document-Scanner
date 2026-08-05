// pdf_scanner.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using iTextSharp.LGPLv2.Core;
using iTextSharp.text;
using iTextSharp.text.pdf;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.Processing;
using SixLabors.ImageSharp.Formats.Jpeg;

class PdfScanner
{
    static void Main(string[] args)
    {
        var inputFiles = new List<string>();
        string outputFile = "output.pdf";
        bool compress = false;
        string pageSize = "A4";
        int dpi = 300;
        bool verbose = false;

        for (int i = 0; i < args.Length; i++)
        {
            switch (args[i])
            {
                case "-i":
                case "--input":
                    while (i + 1 < args.Length && !args[i + 1].StartsWith("-"))
                    {
                        inputFiles.Add(args[++i]);
                    }
                    break;
                case "-o":
                case "--output":
                    outputFile = args[++i];
                    break;
                case "--compress":
                    compress = true;
                    break;
                case "--page-size":
                    pageSize = args[++i];
                    break;
                case "--dpi":
                    dpi = int.Parse(args[++i]);
                    break;
                case "-v":
                    verbose = true;
                    break;
                case "-h":
                case "--help":
                    Console.WriteLine("Использование: dotnet run -- [options]");
                    Console.WriteLine("  -i, --input <files>   Входные изображения");
                    Console.WriteLine("  -o, --output <file>   Выходной PDF/A файл");
                    Console.WriteLine("  --compress            Сжатие изображений");
                    Console.WriteLine("  --page-size <A4|Letter|WxH>");
                    Console.WriteLine("  --dpi <dpi>           Разрешение в DPI");
                    return;
                default:
                    if (!args[i].StartsWith("-"))
                        inputFiles.Add(args[i]);
                    break;
            }
        }

        if (inputFiles.Count == 0)
        {
            Console.Error.WriteLine("Не указаны входные файлы.");
            Environment.Exit(1);
        }

        try
        {
            ConvertToPDFA(inputFiles, outputFile, compress, pageSize, dpi);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Ошибка: {ex.Message}");
            Environment.Exit(1);
        }
    }

    static void ConvertToPDFA(List<string> inputFiles, string outputFile, bool compress, string pageSize, int dpi)
    {
        // Определяем размер страницы в пунктах (1 pt = 1/72 inch)
        float width, height;
        switch (pageSize)
        {
            case "A4": width = 595.28f; height = 841.89f; break;
            case "Letter": width = 612f; height = 792f; break;
            default:
                var parts = pageSize.Split('x');
                if (parts.Length == 2)
                {
                    width = float.Parse(parts[0]);
                    height = float.Parse(parts[1]);
                }
                else
                {
                    width = 595.28f; height = 841.89f;
                }
                break;
        }

        using (var doc = new Document(new Rectangle(width, height)))
        {
            var writer = PdfWriter.GetInstance(doc, new FileStream(outputFile, FileMode.Create));
            writer.PdfVersion = PdfWriter.VERSION_1_4;
            doc.Open();

            foreach (var fname in inputFiles)
            {
                if (!File.Exists(fname))
                {
                    Console.Error.WriteLine($"Файл не найден: {fname}");
                    continue;
                }

                // Загружаем изображение через ImageSharp
                using (var image = Image.Load(fname))
                {
                    // Масштабируем
                    if (compress)
                    {
                        image.Mutate(x => x.Resize((int)(image.Width * 0.7), (int)(image.Height * 0.7)));
                    }

                    // Сохраняем во временный JPG
                    string tempFile = Path.GetTempFileName() + ".jpg";
                    image.Save(tempFile, new JpegEncoder { Quality = compress ? 70 : 95 });

                    // Добавляем изображение в PDF
                    var pdfImage = iTextSharp.text.Image.GetInstance(tempFile);
                    float imgWidth = pdfImage.Width;
                    float imgHeight = pdfImage.Height;

                    float scaleX = width / imgWidth;
                    float scaleY = height / imgHeight;
                    float scale = Math.Min(scaleX, scaleY);

                    pdfImage.ScaleToFit(imgWidth * scale, imgHeight * scale);
                    float x = (width - pdfImage.ScaledWidth) / 2;
                    float y = (height - pdfImage.ScaledHeight) / 2;
                    pdfImage.SetAbsolutePosition(x, y);

                    doc.NewPage();
                    doc.Add(pdfImage);

                    File.Delete(tempFile);
                }
            }
            doc.Close();
        }

        Console.WriteLine($"PDF/A сохранён: {outputFile}");
    }
}
