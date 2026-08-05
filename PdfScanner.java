// PdfScanner.java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.image.ImageType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfScanner {
    public static void main(String[] args) throws IOException {
        List<String> inputFiles = new ArrayList<>();
        String outputFile = "output.pdf";
        boolean compress = false;
        String pageSize = "A4";
        int dpi = 300;
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                case "--input":
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        inputFiles.add(args[++i]);
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
                    dpi = Integer.parseInt(args[++i]);
                    break;
                case "-v":
                    verbose = true;
                    break;
                case "-h":
                case "--help":
                    System.out.println("Использование: java PdfScanner [options]");
                    System.out.println("  -i, --input <files>   Входные изображения");
                    System.out.println("  -o, --output <file>   Выходной PDF/A файл");
                    System.out.println("  --compress            Сжатие изображений");
                    System.out.println("  --page-size <A4|Letter|WxH>");
                    System.out.println("  --dpi <dpi>           Разрешение в DPI");
                    return;
                default:
                    if (!args[i].startsWith("-"))
                        inputFiles.add(args[i]);
                    break;
            }
        }

        if (inputFiles.isEmpty()) {
            System.err.println("Не указаны входные файлы.");
            System.exit(1);
        }

        try (PDDocument document = new PDDocument()) {
            // Определяем размер страницы в пунктах (1 pt = 1/72 inch)
            float width, height;
            switch (pageSize) {
                case "A4":
                    width = PDRectangle.A4.getWidth();
                    height = PDRectangle.A4.getHeight();
                    break;
                case "Letter":
                    width = PDRectangle.LETTER.getWidth();
                    height = PDRectangle.LETTER.getHeight();
                    break;
                default:
                    String[] parts = pageSize.split("x");
                    if (parts.length == 2) {
                        width = Float.parseFloat(parts[0]);
                        height = Float.parseFloat(parts[1]);
                    } else {
                        width = PDRectangle.A4.getWidth();
                        height = PDRectangle.A4.getHeight();
                    }
                    break;
            }

            for (String fname : inputFiles) {
                File imgFile = new File(fname);
                if (!imgFile.exists()) {
                    System.err.println("Файл не найден: " + fname);
                    continue;
                }

                // Загружаем изображение
                BufferedImage image = ImageIO.read(imgFile);
                if (image == null) {
                    System.err.println("Не удалось прочитать: " + fname);
                    continue;
                }

                // Масштабируем
                if (compress) {
                    int newW = (int) (image.getWidth() * 0.7);
                    int newH = (int) (image.getHeight() * 0.7);
                    java.awt.Image scaled = image.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
                    image = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                    image.getGraphics().drawImage(scaled, 0, 0, null);
                }

                // Создаём страницу
                PDPage page = new PDPage(new PDRectangle(width, height));
                document.addPage(page);

                // Вставляем изображение
                PDImageXObject pdImage = PDImageXObject.createFromFileByExtension(imgFile, document);
                if (pdImage == null) {
                    // Если не удалось, пробуем через BufferedImage
                    pdImage = PDImageXObject.createFromFile(fname, document);
                }
                if (pdImage != null) {
                    float imgWidth = pdImage.getWidth();
                    float imgHeight = pdImage.getHeight();
                    float scaleX = width / imgWidth;
                    float scaleY = height / imgHeight;
                    float scale = Math.min(scaleX, scaleY);

                    float x = (width - imgWidth * scale) / 2;
                    float y = (height - imgHeight * scale) / 2;

                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        contentStream.drawImage(pdImage, x, y, imgWidth * scale, imgHeight * scale);
                    }
                }
            }

            document.save(outputFile);
            System.out.println("PDF/A сохранён: " + outputFile);
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }
}
