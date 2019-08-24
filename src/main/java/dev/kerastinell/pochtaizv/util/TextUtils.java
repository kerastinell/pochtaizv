package dev.kerastinell.pochtaizv.util;

import dev.kerastinell.pochtaizv.values.Constants;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Утилиты для работы с текстом.
 */
public class TextUtils {
	/**
	 * Проверяет строку на соответствие одной из указанных в массиве.
	 * @param source Строка для проверки
	 * @param check Строки, содержание которых сравнивается с source
	 * @return true, если содержимое source соответствует одной из строк. false в любом другом случае.
	 */
	public static boolean is(String source, String... check) {
		for (String test : check)
			if (source.equals(test))
				return true;
		return false;
	}

	/**
	 * Генерирует HashMap с указанными ключами с одним и тем же значением.
	 * @param value Значение для всех ключей
	 * @param keys Список ключей
	 * @return Коллекция ключей
	 */
	public static HashMap<String, String> initHashMap(String value, String... keys) {
		HashMap<String, String> map = new LinkedHashMap<>(keys.length);
		for (String key : keys) map.put(key, value);
		return map;
	}

	/**
	 * Преобразует строковую дату в коллекцию HashMap.
	 * @param type Тип даты
	 * @param value Строка с датой. Если null, то преобразуется текущая дата.
	 * @return HashMap с преобразованными значениями даты
	 */
	public static HashMap<String, String> processDate(FormDate type, String value) {
		if (type == null) return new HashMap<>();

		LocalDate date = LocalDate.now();
		try {
			if (value != null && !value.isEmpty()) // вызов функции при value == null означает использование текущей даты
				date = LocalDate.parse(value, Constants.DATE_FORMAT_INPUT);
		} catch (DateTimeParseException exception) {
			Logger.error("Ошибка при распознавании введенной даты!", exception);
		}

		return FormDate.toHashMap(type, date);
	}

	/**
	 * Форматирует срок хранения отправления.
	 * @param timestamp Крайний срок хранения отправления в формате Unix epoch
	 * @return Строка с датой в формате дд.мм.гггг
	 */
	public static String formatDate(long timestamp) {
		return timestamp == 0 ? "" : Constants.DATE_FORMAT_STORAGE.format(Instant.ofEpochMilli(timestamp));
	}

	/**
	 * Оборавичает строку в теги абзаца.
	 * @param string Строка для обработки
	 * @return Строка, форматированная как абзац LibreOffice
	 */
	public static String paragraph(String string) {
		return "<text:p>" + string + "</text:p>";
	}

	/**
	 * Форматирует массу отправления.
	 * @param weight Масса отправления в граммах
	 * @return Строковое представление массы с указанием килограммов и/или граммов
	 */
	public static String formatWeight(int weight) {
		if (weight == 0)
			return "";

		else if (weight > 0 && weight % 1000 == 0)
			return String.format("%d кг", weight / 1000);

		else return String.format("%d кг %d г", weight / 1000, weight % 1000);
	}

	/**
	 * Форматирует стоимость.
	 * @param amount Денежная сумма в формате числа с плавающей точкой
	 * @return Строковое представление стоимости с указанием рублей и/или копеек
	 */
	public static String formatCurrency(double amount) {
		if (amount == 0)
			return "";

		// Разделить строку на составляющие с рублями и копейками
		String[] split = String.format(Locale.ENGLISH, "%.2f", amount).split("\\.");

		String result = split[0] + " руб.";

		// Если в сумме есть копейки, то приписать их к окончательной строке
		if (Integer.parseInt(split[1]) > 0)
			result += " " + split[1] + " коп.";

		return result;
	}

	/**
	 * Возвращает название месяца на русском языке в родительном падеже.
	 * @param month месяц в виде {@link java.time.Month}
	 * @return Название месяца в виде строки
	 */
	static String getLocalizedMonth(Month month) {
		switch (month) {
			case JANUARY: return "января";
			case FEBRUARY: return "февраля";
			case MARCH: return "марта";
			case APRIL: return "апреля";
			case MAY: return "мая";
			case JUNE: return "июня";
			case JULY: return "июля";
			case AUGUST: return "августа";
			case SEPTEMBER: return "сентября";
			case OCTOBER: return "октября";
			case NOVEMBER: return "ноября";
			case DECEMBER: return "декабря";
			default: return "";
		}
	}
}
