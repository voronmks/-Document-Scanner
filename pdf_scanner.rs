// pdf_scanner.rs
use std::env;
use std::fs::File;
use std::io::BufWriter;
use std::path::Path;
use image::io::Reader as ImageReader;
use printpdf::*;
use printpdf::types::PdfLayerReference;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<String> = env::args().collect();
    let mut input_files = Vec::new();
    let mut output_file = "output.pdf".to_string();
    let mut compress = false;
    let mut page_size = "A4".to_string();
    let mut dpi = 300;

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "-i" | "--input" => {
                while i + 1 < args.len() && !args[i + 1].starts_with("-") {
                    input_files.push(args[i + 1].clone());
                    i += 1;
                }
            }
            "-o" | "--output" => {
                output_file = args[i + 1].clone();
                i += 2;
            }
            "--compress" => {
                compress = true;
                i += 1;
            }
            "--page-size" => {
                page_size = args[i + 1].clone();
                i += 2;
            }
            "--dpi" => {
                dpi = args[i + 1].parse()?;
                i += 2;
            }
            "-h" | "--help" => {
                println!("Использование: pdf_scanner [options]");
                println!("  -i, --input <files>   Входные изображения");
                println!("  -o, --output <file>   Выходной PDF/A файл");
                println!("  --compress            Сжатие изображений");
                println!("  --page-size <A4|Letter|WxH>");
                println!("  --dpi <dpi>           Разрешение в DPI");
                return Ok(());
            }
            _ => {
                i += 1;
            }
        }
    }

    if input_files.is_empty() {
        eprintln!("Не указаны входные файлы.");
        return Ok(());
    }

    // Создаём PDF документ
    let (doc, page1, layer1) = PdfDocument::new("PDF/A Document", Mm(210.0), Mm(297.0), "Layer 1");
    let mut current_page = page1;
    let mut current_layer = layer1;

    // Определяем размер страницы в мм
    let (page_width, page_height) = match page_size.as_str() {
        "A4" => (210.0, 297.0),
        "Letter" => (215.9, 279.4),
        _ => {
            // Парсим "WxH"
            let parts: Vec<&str> = page_size.split('x').collect();
            if parts.len() == 2 {
                (parts[0].parse().unwrap_or(210.0), parts[1].parse().unwrap_or(297.0))
            } else {
                (210.0, 297.0)
            }
        }
    };

    for (idx, fname) in input_files.iter().enumerate() {
        // Загружаем изображение
        let img = ImageReader::open(fname)?.decode()?;
        let (img_width, img_height) = (img.width(), img.height());

        // Масштабируем под страницу
        let scale_x = (page_width * 72.0 / 25.4) / img_width as f64;
        let scale_y = (page_height * 72.0 / 25.4) / img_height as f64;
        let scale = scale_x.min(scale_y);

        // Если не первая страница, добавляем новую
        if idx > 0 {
            let (doc_ref, page, layer) = doc.add_page(Mm(page_width), Mm(page_height), "Page");
            current_page = page;
            current_layer = layer;
        }

        // Вставляем изображение
        let x = (page_width * 72.0 / 25.4 - img_width as f64 * scale) / 2.0;
        let y = (page_height * 72.0 / 25.4 - img_height as f64 * scale) / 2.0;

        // Сохраняем изображение в PNG (временный файл)
        let temp_path = format!("/tmp/img_{}.png", idx);
        img.save(&temp_path)?;

        // Добавляем изображение на страницу
        let image = Image::from_file(&temp_path)?;
        current_layer.add_image(image, Mm(x * 25.4 / 72.0), Mm(y * 25.4 / 72.0), Some(Mm(img_width as f64 * scale * 25.4 / 72.0)), Some(Mm(img_height as f64 * scale * 25.4 / 72.0)));

        // Удаляем временный файл
        std::fs::remove_file(temp_path)?;
    }

    // Сохраняем PDF
    doc.save(&mut BufWriter::new(File::create(&output_file)?))?;
    println!("PDF/A сохранён: {}", output_file);
    Ok(())
}
