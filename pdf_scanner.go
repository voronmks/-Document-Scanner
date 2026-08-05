// pdf_scanner.go
package main

import (
	"flag"
	"fmt"
	"image"
	"image/jpeg"
	"os"
	"path/filepath"
	"strings"

	"github.com/disintegration/imaging"
	"github.com/jung-kurt/gofpdf/v2"
)

func convertToPDFA(inputFiles []string, outputFile string, compress bool, pageSize string, dpi int) error {
	// Создаём PDF
	pdf := gofpdf.New("P", "mm", "A4", "")
	pdf.SetCompression(compress)
	pdf.SetFont("Arial", "", 12)

	// Определяем размер страницы в мм
	var width, height float64
	switch pageSize {
	case "A4":
		width, height = 210, 297
	case "Letter":
		width, height = 215.9, 279.4
	default:
		// Парсим "WxH" в мм
		parts := strings.Split(pageSize, "x")
		if len(parts) == 2 {
			fmt.Sscanf(parts[0], "%f", &width)
			fmt.Sscanf(parts[1], "%f", &height)
		} else {
			width, height = 210, 297
		}
	}

	for _, fname := range inputFiles {
		if _, err := os.Stat(fname); os.IsNotExist(err) {
			return fmt.Errorf("файл не найден: %s", fname)
		}
		// Открываем изображение
		file, err := os.Open(fname)
		if err != nil {
			return err
		}
		img, err := jpeg.Decode(file)
		if err != nil {
			// пробуем декодировать как PNG
			file.Seek(0, 0)
			img, err = imaging.Decode(file)
			if err != nil {
				file.Close()
				return fmt.Errorf("не удалось декодировать %s: %w", fname, err)
			}
		}
		file.Close()

		// Добавляем страницу
		pdf.AddPage()

		// Масштабируем изображение под страницу
		bounds := img.Bounds()
		imgW := float64(bounds.Dx()) / 25.4 * 72 // переводим пиксели в пункты (при 72 DPI)
		imgH := float64(bounds.Dy()) / 25.4 * 72
		// Масштабируем с сохранением пропорций
		scaleW := width / imgW
		scaleH := height / imgH
		scale := scaleW
		if scaleH < scaleW {
			scale = scaleH
		}
		// Если сжатие, уменьшаем разрешение
		if compress {
			// Уменьшаем изображение до целевого размера
			newW := int(float64(bounds.Dx()) * 0.7)
			newH := int(float64(bounds.Dy()) * 0.7)
			img = imaging.Resize(img, newW, newH, imaging.Lanczos)
		}
		// Вставляем изображение в PDF (с центрированием)
		x := (width - float64(bounds.Dx())/25.4*72*scale) / 2
		y := (height - float64(bounds.Dy())/25.4*72*scale) / 2
		// Сохраняем изображение во временный файл
		tmpFile, err := os.CreateTemp("", "img_*.jpg")
		if err != nil {
			return err
		}
		defer os.Remove(tmpFile.Name())
		err = jpeg.Encode(tmpFile, img, &jpeg.Options{Quality: 85})
		if err != nil {
			return err
		}
		tmpFile.Close()

		pdf.Image(tmpFile.Name(), x, y, float64(bounds.Dx())/25.4*72*scale, float64(bounds.Dy())/25.4*72*scale, false, "", 0, "")
	}

	err := pdf.OutputFileAndClose(outputFile)
	if err != nil {
		return fmt.Errorf("ошибка сохранения PDF: %w", err)
	}
	fmt.Printf("PDF/A сохранён: %s\n", outputFile)
	return nil
}

func main() {
	var inputList string
	var outputFile string
	var compress bool
	var pageSize string
	var dpi int
	var verbose bool

	flag.StringVar(&inputList, "input", "", "Список изображений через запятую")
	flag.StringVar(&inputList, "i", "", "Список изображений через запятую")
	flag.StringVar(&outputFile, "output", "output.pdf", "Выходной PDF/A файл")
	flag.StringVar(&outputFile, "o", "output.pdf", "Выходной PDF/A файл")
	flag.BoolVar(&compress, "compress", false, "Сжатие изображений")
	flag.StringVar(&pageSize, "page-size", "A4", "Размер страницы: A4, Letter, или WxH")
	flag.IntVar(&dpi, "dpi", 300, "Разрешение в DPI")
	flag.BoolVar(&verbose, "v", false, "Подробный вывод")
	flag.Parse()

	if inputList == "" {
		fmt.Fprintln(os.Stderr, "Не указаны входные файлы. Используйте -input file1.jpg,file2.png")
		os.Exit(1)
	}
	inputFiles := strings.Split(inputList, ",")
	for i := range inputFiles {
		inputFiles[i] = strings.TrimSpace(inputFiles[i])
	}

	err := convertToPDFA(inputFiles, outputFile, compress, pageSize, dpi)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Ошибка: %v\n", err)
		os.Exit(1)
	}
}
