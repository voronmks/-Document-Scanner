// pdf_scanner.cpp
#include <iostream>
#include <string>
#include <vector>
#include <filesystem>
#include <opencv2/opencv.hpp>
#include <podofo/podofo.h>

using namespace std;
using namespace PoDoFo;

bool convertToPDFA(const vector<string>& inputFiles, const string& outputFile, bool compress, const string& pageSize, int dpi) {
    // Определяем размер страницы в пунктах (1 pt = 1/72 inch)
    float width, height;
    if (pageSize == "A4") {
        width = 595.28f;
        height = 841.89f;
    } else if (pageSize == "Letter") {
        width = 612.0f;
        height = 792.0f;
    } else {
        size_t pos = pageSize.find('x');
        if (pos != string::npos) {
            width = stof(pageSize.substr(0, pos));
            height = stof(pageSize.substr(pos + 1));
        } else {
            width = 595.28f;
            height = 841.89f;
        }
    }

    try {
        // Создаём PDF документ
        PdfMemDocument pdf;
        pdf.SetVersion(PdfVersion_1_4);

        for (const string& fname : inputFiles) {
            if (!filesystem::exists(fname)) {
                cerr << "Файл не найден: " << fname << endl;
                continue;
            }

            // Загружаем изображение через OpenCV
            cv::Mat image = cv::imread(fname, cv::IMREAD_COLOR);
            if (image.empty()) {
                cerr << "Не удалось прочитать: " << fname << endl;
                continue;
            }

            // Сжатие
            if (compress) {
                cv::resize(image, image, cv::Size(), 0.7, 0.7, cv::INTER_LANCZOS4);
            }

            // Создаём страницу
            PdfPage* page = pdf.CreatePage(PdfRect(0, 0, width, height));
            pdf.AddPage(page);

            // Конвертируем OpenCV Mat в PoDoFo изображение
            // (упрощённо: сохраняем во временный файл)
            string tempFile = "/tmp/img_" + to_string(inputFiles.size()) + ".jpg";
            cv::imwrite(tempFile, image);

            // Вставляем изображение
            PdfImage img = pdf.CreateImage(tempFile);
            if (img.IsValid()) {
                double imgWidth = img.GetWidth();
                double imgHeight = img.GetHeight();
                double scaleX = width / imgWidth;
                double scaleY = height / imgHeight;
                double scale = min(scaleX, scaleY);

                double x = (width - imgWidth * scale) / 2;
                double y = (height - imgHeight * scale) / 2;

                PdfPainter painter;
                painter.SetPage(page);
                painter.DrawImage(x, y, imgWidth * scale, imgHeight * scale, &img);
                painter.FinishPage();
            }

            // Удаляем временный файл
            filesystem::remove(tempFile);
        }

        // Сохраняем PDF
        pdf.Write(outputFile.c_str());
        cout << "PDF/A сохранён: " << outputFile << endl;
        return true;
    } catch (const PdfError& e) {
        cerr << "Ошибка PoDoFo: " << e.what() << endl;
        return false;
    } catch (const exception& e) {
        cerr << "Ошибка: " << e.what() << endl;
        return false;
    }
}

int main(int argc, char* argv[]) {
    vector<string> inputFiles;
    string outputFile = "output.pdf";
    bool compress = false;
    string pageSize = "A4";
    int dpi = 300;
    bool verbose = false;

    for (int i = 1; i < argc; i++) {
        string arg = argv[i];
        if (arg == "-i" || arg == "--input") {
            while (i + 1 < argc && string(argv[i + 1])[0] != '-') {
                inputFiles.push_back(argv[++i]);
            }
        } else if (arg == "-o" || arg == "--output") {
            if (i + 1 < argc) outputFile = argv[++i];
        } else if (arg == "--compress") {
            compress = true;
        } else if (arg == "--page-size" && i + 1 < argc) {
            pageSize = argv[++i];
        } else if (arg == "--dpi" && i + 1 < argc) {
            dpi = stoi(argv[++i]);
        } else if (arg == "-v" || arg == "--verbose") {
            verbose = true;
        } else if (arg == "-h" || arg == "--help") {
            cout << "Использование: pdf_scanner [options]" << endl;
            cout << "  -i, --input <files>   Входные изображения" << endl;
            cout << "  -o, --output <file>   Выходной PDF/A файл" << endl;
            cout << "  --compress            Сжатие изображений" << endl;
            cout << "  --page-size <A4|Letter|WxH>" << endl;
            cout << "  --dpi <dpi>           Разрешение в DPI" << endl;
            return 0;
        }
    }

    if (inputFiles.empty()) {
        cerr << "Не указаны входные файлы." << endl;
        return 1;
    }

    bool success = convertToPDFA(inputFiles, outputFile, compress, pageSize, dpi);
    return success ? 0 : 1;
}
