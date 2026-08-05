# pdf_scanner.py
import sys
import os
import argparse
from PIL import Image
import img2pdf
import tempfile
import subprocess
import shutil

def convert_to_pdfa(input_files, output_file, ocr=False, lang='rus+eng', compress=False, page_size='A4', dpi=300):
    """Конвертирует изображения в PDF/A."""
    # Проверка входных файлов
    images = []
    for f in input_files:
        if not os.path.exists(f):
            print(f"Файл не найден: {f}", file=sys.stderr)
            return False
        try:
            img = Image.open(f)
            images.append(img)
        except Exception as e:
            print(f"Ошибка открытия {f}: {e}", file=sys.stderr)
            return False

    # Создание временного PDF
    temp_pdf = tempfile.NamedTemporaryFile(suffix='.pdf', delete=False)
    temp_pdf.close()

    try:
        # Конвертация в PDF с помощью img2pdf
        with open(temp_pdf.name, 'wb') as f:
            # Получаем размер страницы в пунктах (1 pt = 1/72 inch)
            if page_size == 'A4':
                width, height = 595, 842  # pt
            elif page_size == 'Letter':
                width, height = 612, 792
            else:
                # Custom: ожидаем формат "595x842"
                try:
                    w, h = map(int, page_size.split('x'))
                    width, height = w, h
                except:
                    width, height = 595, 842

            # Конвертируем каждое изображение
            for img in images:
                # Изменяем размер под страницу
                if compress:
                    img.thumbnail((width, height), Image.Resampling.LANCZOS)
                # Сохраняем во временный файл
                temp_img = tempfile.NamedTemporaryFile(suffix='.jpg', delete=False)
                img.convert('RGB').save(temp_img.name, 'JPEG', quality=85 if compress else 95)
                temp_img.close()
                # Добавляем в PDF
                with open(temp_img.name, 'rb') as img_file:
                    pdf_bytes = img2pdf.convert(img_file.read(), layout_fun=img2pdf.get_layout_fun((width, height)))
                    f.write(pdf_bytes)
                os.unlink(temp_img.name)

        # Если нужен OCR, используем ocrmypdf
        if ocr:
            final_pdf = output_file if output_file else 'output.pdf'
            cmd = ['ocrmypdf', '--output-type', 'pdfa', '--language', lang, temp_pdf.name, final_pdf]
            if compress:
                cmd.insert(1, '--optimize')
                cmd.insert(2, '1')
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode != 0:
                print(f"Ошибка OCR: {result.stderr}", file=sys.stderr)
                return False
            print(f"PDF/A с OCR сохранён: {final_pdf}")
        else:
            # Просто копируем временный файл в выходной
            out_file = output_file if output_file else 'output.pdf'
            shutil.copy2(temp_pdf.name, out_file)
            print(f"PDF/A сохранён: {out_file}")

    except Exception as e:
        print(f"Ошибка: {e}", file=sys.stderr)
        return False
    finally:
        if os.path.exists(temp_pdf.name):
            os.unlink(temp_pdf.name)

    return True

def main():
    parser = argparse.ArgumentParser(description="Сканер документов в PDF/A")
    parser.add_argument('-i', '--input', nargs='+', required=True, help='Входные изображения')
    parser.add_argument('-o', '--output', default='output.pdf', help='Выходной PDF/A файл')
    parser.add_argument('--ocr', action='store_true', help='Включить распознавание текста')
    parser.add_argument('--lang', default='rus+eng', help='Язык для OCR')
    parser.add_argument('--compress', action='store_true', help='Сжатие изображений')
    parser.add_argument('--page-size', default='A4', help='Размер страницы: A4, Letter, или WxH')
    parser.add_argument('--dpi', type=int, default=300, help='Разрешение в DPI')
    parser.add_argument('-v', '--verbose', action='store_true', help='Подробный вывод')
    args = parser.parse_args()

    success = convert_to_pdfa(
        args.input, args.output, args.ocr, args.lang,
        args.compress, args.page_size, args.dpi
    )
    sys.exit(0 if success else 1)

if __name__ == '__main__':
    main()
