// pdf_scanner.js
#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const { PDFDocument, rgb } = require('pdf-lib');
const sharp = require('sharp');

async function convertToPDFA(inputFiles, outputFile, options = {}) {
    const { compress = false, pageSize = 'A4', dpi = 300 } = options;
    const pdfDoc = await PDFDocument.create();
    pdfDoc.setTitle('Scanned Document');

    // Определяем размер страницы в пунктах (1 pt = 1/72 inch)
    let width, height;
    switch (pageSize) {
        case 'A4': width = 595.28; height = 841.89; break;
        case 'Letter': width = 612; height = 792; break;
        default:
            const parts = pageSize.split('x');
            if (parts.length === 2) {
                width = parseFloat(parts[0]);
                height = parseFloat(parts[1]);
            } else {
                width = 595.28; height = 841.89;
            }
    }

    for (const fname of inputFiles) {
        if (!fs.existsSync(fname)) {
            console.error(`Файл не найден: ${fname}`);
            continue;
        }

        // Загружаем изображение
        let imageBuffer = fs.readFileSync(fname);
        let image;
        try {
            image = await pdfDoc.embedPng(imageBuffer);
        } catch (e) {
            // пробуем как JPG
            try {
                image = await pdfDoc.embedJpg(imageBuffer);
            } catch (e2) {
                // конвертируем через sharp
                const jpegBuffer = await sharp(imageBuffer).jpeg({ quality: 85 }).toBuffer();
                image = await pdfDoc.embedJpg(jpegBuffer);
            }
        }

        // Добавляем страницу
        const page = pdfDoc.addPage([width, height]);

        // Масштабируем изображение
        const imgWidth = image.width;
        const imgHeight = image.height;
        const scaleX = width / imgWidth;
        const scaleY = height / imgHeight;
        const scale = Math.min(scaleX, scaleY);

        const x = (width - imgWidth * scale) / 2;
        const y = (height - imgHeight * scale) / 2;

        page.drawImage(image, {
            x: x,
            y: y,
            width: imgWidth * scale,
            height: imgHeight * scale,
        });
    }

    const pdfBytes = await pdfDoc.save();
    fs.writeFileSync(outputFile, pdfBytes);
    console.log(`PDF/A сохранён: ${outputFile}`);
}

async function main() {
    const args = process.argv.slice(2);
    let inputFiles = [];
    let outputFile = 'output.pdf';
    let compress = false;
    let pageSize = 'A4';
    let dpi = 300;
    let verbose = false;

    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '-i':
            case '--input':
                while (i + 1 < args.length && !args[i + 1].startsWith('-')) {
                    inputFiles.push(args[++i]);
                }
                break;
            case '-o':
            case '--output':
                outputFile = args[++i];
                break;
            case '--compress':
                compress = true;
                break;
            case '--page-size':
                pageSize = args[++i];
                break;
            case '--dpi':
                dpi = parseInt(args[++i]);
                break;
            case '-v':
                verbose = true;
                break;
            case '-h':
            case '--help':
                console.log('Использование: node pdf_scanner.js [options]');
                console.log('  -i, --input <files>   Входные изображения');
                console.log('  -o, --output <file>   Выходной PDF/A файл');
                console.log('  --compress            Сжатие изображений');
                console.log('  --page-size <A4|Letter|WxH>');
                console.log('  --dpi <dpi>           Разрешение в DPI');
                return;
            default:
                if (!args[i].startsWith('-')) {
                    inputFiles.push(args[i]);
                }
        }
    }

    if (inputFiles.length === 0) {
        console.error('Не указаны входные файлы.');
        process.exit(1);
    }

    await convertToPDFA(inputFiles, outputFile, { compress, pageSize, dpi });
}

main().catch(console.error);
