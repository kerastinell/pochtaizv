package dev.kerastinell.pochtaizv.values;

import dev.kerastinell.pochtaizv.util.io.IoUtils;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Набор значений, которые не изменяются во время работы программы.
 */
public interface Constants {
	// Путь к директории с шаблоном извещения
	Path TEMPLATE_DIR_PATH = IoUtils.getPath("/template");
	// Имя файла, в который будут заменены данные извещения
	String TEMPLATE_XML_FILE = "content.xml";
	// Имя файла с штрихкодом отправления
	String TEMPLATE_BARCODE_FILE = "10000000000001F400000064723B2F633C7B66BB.png";

	DateTimeFormatter DATE_FORMAT_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
			.withZone(ZoneId.systemDefault()); // Для форматирования даты, введённой пользователем
	DateTimeFormatter DATE_FORMAT_STORAGE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
			.withZone(ZoneId.systemDefault()); // Для форматирования даты хранения отправления

	// Эти числа в элементе истории отслеживания отправления
	// означают статус "Ожидает вручения"
	int POST_ARRIVED_TYPE = 8;
	int POST_ARRIVED_ATTR = 2;

	// Все параметры, которые пользователь может задать/ввести самостоятельно
	String[][] USER_DEFINED_PARAMETERS = {
			// Формат:
			// [0] Параметр командной строки
			// [1] Ключ для initialFormData
			// [2] Строка для вывода в консоль перед запросом ввода данных у пользователя
			{"name", "Получатель.Имя", "Ф.И.О. получателя: "},
			{"address", "Получатель.Адрес.Указанный", "Адрес получателя: "},
			{"id-type", "Документ.Вид", "Вид документа, удостоверяющего личность: "},
			{"id-series", "Документ.Серия", "Серия документа: "},
			{"id-number", "Документ.Номер", "Номер документа: "},
			{"id-issue-day", "Документ.Выдан.День", "День выдачи документа: "},
			{"id-issue-month", "Документ.Выдан.Месяц", "Месяц выдачи документа (число): "},
			{"id-issue-year", "Документ.Выдан.Год", "Год выдачи документа: "},
			{"id-issued-by", "Документ.Выдан.Кем", "Кем выдан документ: "},
			{"registered-at", "Получатель.Адрес.Регистрация", "Адрес регистрации получателя: "}
	};
}
