📄 Document Scanner – Конвертация изображений в PDF/A
Мощный консольный инструмент для сканирования документов и сохранения в архивируемый PDF/A-формат.
Поддерживает 7 языков программирования – выберите свой!

✨ Возможности
📸 Конвертация изображений – из JPG, PNG, BMP, TIFF в PDF/A-1, PDF/A-2, PDF/A-3.

🔍 Распознавание текста (OCR) – извлечение текста из отсканированных документов (опционально).

📄 Объединение нескольких файлов – создание одного PDF/A из множества изображений.

📏 Настройка размера страницы – A4, Letter, Custom.

🎨 Коррекция изображений – автоматическая обрезка, поворот, улучшение контраста.

🧹 Сжатие – уменьшение размера файла без потери качества.

🔒 Стандарт PDF/A – обеспечивает долгосрочное хранение документов.

🖥️ Кроссплатформенность – работает в Linux, macOS и Windows.

📦 Поддерживаемые языки
Язык	Файл	Основные библиотеки
Python	pdf_scanner.py	Pillow, img2pdf, ocrmypdf
Go	pdf_scanner.go	gofpdf, github.com/disintegration/imaging
Rust	pdf_scanner.rs	image, printpdf, pdf
JavaScript	pdf_scanner.js	pdf-lib, sharp, tesseract.js
C#	pdf_scanner.cs	iTextSharp.LGPLv2.Core, SixLabors.ImageSharp
Java	PdfScanner.java	Apache PDFBox, OpenCV
C++	pdf_scanner.cpp	PoDoFo, OpenCV
🚀 Быстрый старт
1. Склонируйте репозиторий
bash
git clone https://github.com/yourname/pdf-scanner.git
cd pdf-scanner
2. Установите зависимости и запустите
Python

bash
pip install Pillow img2pdf ocrmypdf
python pdf_scanner.py --input image1.jpg image2.png --output document.pdf --ocr --lang rus
Go

bash
go mod init pdf_scanner
go get github.com/jung-kurt/gofpdf/v2
go get github.com/disintegration/imaging
go run pdf_scanner.go -input image1.jpg,image2.png -output document.pdf
Rust (добавьте зависимости в Cargo.toml)

bash
cargo new pdf_scanner --bin
# добавьте image, printpdf, pdf
cargo run -- --input image1.jpg image2.png --output document.pdf
JavaScript (Node.js)

bash
npm install pdf-lib sharp
node pdf_scanner.js --input image1.jpg image2.png --output document.pdf
C#

bash
dotnet new console -n pdf_scanner
dotnet add package iTextSharp.LGPLv2.Core
dotnet add package SixLabors.ImageSharp
dotnet run -- --input image1.jpg image2.png --output document.pdf
Java (сборка с Maven/Gradle)

bash
javac -cp .:pdfbox-2.0.27.jar PdfScanner.java
java -cp .:pdfbox-2.0.27.jar PdfScanner --input image1.jpg image2.png --output document.pdf
C++ (установите PoDoFo и OpenCV)

bash
g++ -std=c++17 pdf_scanner.cpp -lpodofo -lopencv_core -lopencv_imgproc -lopencv_imgcodecs -o pdf_scanner
./pdf_scanner --input image1.jpg image2.png --output document.pdf
📋 Пример использования
bash
python pdf_scanner.py --input scan1.jpg scan2.png --output contract.pdf --ocr --lang rus --compress
Вывод:

text
Обработка изображений...
  scan1.jpg → страница 1
  scan2.png → страница 2
Распознавание текста...
Сохранение PDF/A-3...
Файл сохранён: contract.pdf (1.2 MB)
⚙️ Опции командной строки
Параметр	Описание
-i, --input	Список изображений для обработки
-o, --output	Выходной PDF/A файл
--ocr	Включить распознавание текста (OCR)
--lang	Язык для OCR (по умолчанию rus+eng)
--compress	Сжатие изображений
--page-size	Размер страницы: A4, Letter, custom
--dpi	Разрешение в DPI (по умолчанию 300)
-v, --verbose	Подробный вывод
-h, --help	Справка
📄 Лицензия
MIT – свободно используйте, модифицируйте и распространяйте.

🤝 Вклад
Приветствуются pull request'ы! Если хотите добавить новый язык или улучшить существующий – создавайте issue.

🧠 Авторы
Проект создан в образовательных целях для демонстрации обработки изображений и создания PDF на разных языках.

