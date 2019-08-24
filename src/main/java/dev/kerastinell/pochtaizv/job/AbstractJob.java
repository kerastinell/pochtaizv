package dev.kerastinell.pochtaizv.job;

import dev.kerastinell.pochtaizv.util.Logger;
import dev.kerastinell.pochtaizv.util.TextUtils;
import dev.kerastinell.pochtaizv.util.io.IoUtils;
import dev.kerastinell.pochtaizv.values.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Абстрактная реализация задачи заполнения извещения.
 */
public abstract class AbstractJob implements Runnable {
	// Все ключи, которые встречаются в шаблоне
	protected static final String[] TEMPLATE_KEYS = {
			"Дата.Генерация",
			"Код отслеживания",
			"Получатель.Имя",
			"Получатель.Адрес.Указанный",
			"Вид и категория",
			"Разряд",
			"С уведомлением",
			"Срок хранения",
			"Откуда",
			"Масса",
			"Объявленная ценность",
			"Наложенный платеж",
			"Плата за возврат",
			"Плата за досыл",
			"Таможенная пошлина",
			"Получатель.Адрес.Выдача",
			"Вызов курьера",
			"Документ.Вид",
			"Документ.Серия",
			"Документ.Номер",
			"Документ.Выдан.День",
			"Документ.Выдан.Месяц",
			"Документ.Выдан.Год",
			"Документ.Выдан.Кем",
			"Получатель.Адрес.Регистрация",
			"Дата.Получение.День",
			"Дата.Получение.Месяц",
			"Дата.Получение.Год"
	};

	protected final String trackingCode;
	private HashMap<String, String> templateData;
	private InputStream barcodeImage;

	private final File outputFile;
	private ZipOutputStream output;

	protected AbstractJob(String trackingCode) {
		this.trackingCode = trackingCode;
		outputFile = new File(getFileName());
	}

	/**
	 * Возвращает имя выходного файла.
	 * @return Имя выходного файла.
	 */
	protected abstract String getFileName();

	/**
	 * Возвращает коллекцию значений для заполнения шаблона.
	 * @return Коллекция значений для заполнения шаблона.
	 */
	protected abstract HashMap<String, String> getTemplateData();

	/**
	 * Возвращает поток с изображением штрихкода кода отслеживания отправления.
	 * @return Поток с изображением штрихкода
	 */
	protected abstract InputStream getBarcodeImage();

	/**
	 * Запускает основную логику задачи. Получает данные от классов-наследников,
	 * открывает выходной файл для записи и начинает по-одному обрабатывать файлы шаблона.
	 */
	@Override
	public void run() {
		try {
			if (Constants.TEMPLATE_DIR_PATH == null) {
				Logger.error("Сохранение невозможно!", new NullPointerException("templateDirPath == null"));
				return;
			}

			// Подготовка данных
			templateData = getTemplateData();
			barcodeImage = getBarcodeImage();

			// Создает .odg файл и открывает поток для записи данных
			output = new ZipOutputStream(new FileOutputStream(outputFile));

			// Проходит по внутренней директории шаблона и направляет
			// каждый файл на обработку в processTemplateFile
			Files.walk(Constants.TEMPLATE_DIR_PATH)
					.filter(Files::isRegularFile)
					.forEachOrdered(this::processTemplateFile);
			Logger.track(trackingCode, "Сохранено в файл " + outputFile.getAbsolutePath());
		} catch (IOException exception) {
			Logger.error("Ошибка при записи файла!", exception);
		} finally {
			IoUtils.closeOutputStream(output);
		}
	}

	/**
	 * Обрабатывает файл шаблона извещения.
	 * @param path Путь к файлу
	 */
	private void processTemplateFile(Path path) {
		final String pathInZip = Constants.TEMPLATE_DIR_PATH
				.relativize(path).toString()
				// Когда программа запущена из среды разработки,
				// используется системный разделитель директорий,
				// из-за чего в Windows сгенерированные извещения не
				// открываются в LibreOffice без функции восстановления
				.replace("\\", "/");
		InputStream data;

		// Все файлы просто копируются в выходной файл за исключением двух:
		//    1. content.xml - сюда подставляются данные пользователя
		//    2. *.png - заменяется на сгенерированный штрихкод
		try {
			if (pathInZip.endsWith(Constants.TEMPLATE_XML_FILE)) {
				// Загрузить содержимое шаблона из файла в строку
				String template = IoUtils.readString(IoUtils.open(path));

				// Отправить в fillTemplate на заполнение
				template = fillTemplate(template);

				// Преобразовать в поток
				data = new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8));
			} else if (pathInZip.endsWith(Constants.TEMPLATE_BARCODE_FILE) && barcodeImage != null) {
				data = barcodeImage; // если наследующий класс вернул null вместо ожидаемого потока, то замена не произойдёт
			} else data = IoUtils.open(path);

			output.putNextEntry(new ZipEntry(pathInZip));
			IoUtils.transfer(data, output);
			output.closeEntry();
		} catch (IOException exception) {
			Logger.error("Ошибка при работе с выходным файлом!", exception);
		}
	}

	/**
	 * Заполняет шаблон методом замены строк в содержимом файла content.xml.
	 * @param source Строка-содержимое файла content.xml
	 * @return Содержимое заполненного извещения
	 */
	private String fillTemplate(String source) {
		Logger.track(trackingCode, "Заполнение шаблона");

		// Адрес выдачи заполняется иначе, потому что заполняемое поле
		// состоит из нескольких строк (для LibreOffice - абзацев)
		source = source.replace(
				TextUtils.paragraph("${Получатель.Адрес.Выдача}"),
				templateData.remove("Получатель.Адрес.Выдача")
		);

		// Замена всех оставшихся значений
		for (String key : templateData.keySet()) {
			String value = templateData.get(key);
			source = source.replace("${" + key + "}", value);
		}

		return source;
	}
}
