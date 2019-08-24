package dev.kerastinell.pochtaizv.util;

import java.time.LocalDate;
import java.util.HashMap;

/**
 * Тип даты, используемой в извещении.
 */
public enum FormDate {
	// Дата генерации извещения
	GENERATION,
	// Датаа выдачи документа, удостоверяющего личность
	ID_ISSUE,
	// Дата вручения отправления
	PICKUP;

	/**
	 * Преобразует тип даты из параметра командной строки.
	 * @param arg Параметр командной строки
	 * @return Тип даты. При нераспознанном параметре - null
	 */
	public static FormDate fromCli(String arg) {
		switch (arg) {
			case "gen-date":
				return GENERATION;
			case "id-issue-date":
				return ID_ISSUE;
			case "pickup-date":
				return PICKUP;
			default:
				Logger.verbose("Пропускаю неизвестный параметр: " + arg);
				return null;
		}
	}

	/**
	 * Преобразует вид даты в коллекцию данных для заполнения извещения.
	 * @param type Тип даты
	 * @param date Дата, числа из которой будут использоваться при создании коллекции
	 * @return Коллекция данных для заполнения шаблона
	 */
	public static HashMap<String, String> toHashMap(FormDate type, LocalDate date) {
		final HashMap<String, String> map = new HashMap<>();
		switch (type) {
			case GENERATION:
				map.put("Дата.Генерация", String.format("%d %s %d",
						date.getDayOfMonth(),
						TextUtils.getLocalizedMonth(date.getMonth()),
						date.getYear())
				);
				break;
			case ID_ISSUE:
				map.put("Документ.Выдан.День", String.valueOf(date.getDayOfMonth()));
				map.put("Документ.Выдан.Месяц", TextUtils.getLocalizedMonth(date.getMonth()));
				map.put("Документ.Выдан.Год", String.valueOf(date.getYear()));
				break;
			case PICKUP:
				map.put("Дата.Получение.День", String.valueOf(date.getDayOfMonth()));
				map.put("Дата.Получение.Месяц", TextUtils.getLocalizedMonth(date.getMonth()));
				map.put("Дата.Получение.Год", String.valueOf(date.getYear() - 2000)); // последние 2 цифры
				break;
		}

		return map;
	}
}
