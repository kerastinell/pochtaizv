package dev.kerastinell.pochtaizv.job.impl;

import dev.kerastinell.pochtaizv.PochtaDataFetcher;
import dev.kerastinell.pochtaizv.util.*;
import dev.kerastinell.pochtaizv.util.io.IoUtils;
import dev.kerastinell.pochtaizv.values.Constants;
import dev.kerastinell.pochtaizv.values.GlobalOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static dev.kerastinell.pochtaizv.util.TextUtils.is;
import static dev.kerastinell.pochtaizv.values.GlobalOptions.TRACKING_CODES;

/**
 * Класс, в задачи которого входит обработка параметров,
 * указанных пользователем при запуске программы.
 */
public class InitialFormDataJob implements Runnable {
	private final String[] args;
	private final PochtaDataFetcher pochtaDataFetcher;

	private final BufferedReader userInput;
	private final HashMap<String, String> formData;

	public InitialFormDataJob(String[] args, PochtaDataFetcher pochtaDataFetcher) {
		this.args = args;
		this.pochtaDataFetcher = pochtaDataFetcher;

		userInput = IoUtils.getUserInputReader(); // Для получения пользовательского ввода
		formData = TextUtils.initHashMap(null,
				"Получатель.Имя", "Получатель.Адрес.Указанный", "Документ.Вид",
				"Документ.Серия", "Документ.Номер", "Документ.Выдан.Кем",
				"Получатель.Адрес.Регистрация");
	}

	@Override
	public void run() {
		for (String arg : args) {
			// Парсинг параметров без значений
			switch (arg) {
				// Команды помощи
				case "-h":
				case "-help":
				case "--help":
					printHelpAndExit();
					continue;

				// Параметры, которые влияют на переменные в GlobalOptions
				case "-q":
				case "--quiet":
					GlobalOptions.QUIET = true;
					continue;
				case "--empty":
					GlobalOptions.EMPTY = true;
					return; // При использовании параметра --empty все остальные параметры игнорируются
				case "--offline":
				case "--no-api":
					GlobalOptions.OFFLINE = true;
					continue;
				case "--no-input":
					GlobalOptions.NO_INPUT = true;
					continue;
			}

			// Все остальные параметры должны передавать значение:
			// --параметр=значение
			if (!(arg.startsWith("--") && arg.contains("="))) {
				Logger.verbose("Пропускаю неизвестный параметр: " + arg);
				continue;
			}

			// Разделение строки вида --параметр=значение на две
			final String value = arg.substring(arg.indexOf("=") + 1);
			arg = arg.substring(2, arg.indexOf("="));

			// Парсинг User agent
			if (is(arg, "user-agent")) {
				GlobalOptions.API_USER_AGENT = value;
			}

			// Парсинг кодов отслеживания
			if (is(arg, "track", "tracks")) {
				TRACKING_CODES.addAll(Arrays.asList(value.split(";")));
				continue;
			}

			// Парсинг календарных дат
			if (arg.endsWith("-date")) {
				formData.putAll(TextUtils.processDate(FormDate.fromCli(arg), value));
				continue;
			}

			for (String[] udp : Constants.USER_DEFINED_PARAMETERS)
				if (is(arg, udp[0]))
					formData.put(udp[1], value);
		}

		// Пользователь не указал коды отслеживания отправлений
		if (TRACKING_CODES.size() == 0) {
			if (GlobalOptions.NO_INPUT) {
				Logger.verbose("Не указаны коды отслеживания! Будет сгенирировано одно извещение без штрихкода");
				TRACKING_CODES.add("");
				GlobalOptions.OFFLINE = true;
			} else {
				final String enteredTrackingCode = askForInput("Коды отслеживания (разделить точкой с запятой): ");
				TRACKING_CODES.addAll(Arrays.asList(enteredTrackingCode.split(";")));
				// Если пользователь просто нажал Enter, то в список добавился пустой код отслеживания
				if (!TRACKING_CODES.isEmpty() && TRACKING_CODES.get(0).isEmpty()) {
					Logger.verbose("Обнаружен пустой код отслеживания!");
					GlobalOptions.OFFLINE = true;
				}
			}
		}

		// Начать инициализацию pochtaDataFetcher
		pochtaDataFetcher.initialize();

		// Чтение параметров завершено, проверить и попросить ввести пользователя недостающие данные
		for (String key : formData.keySet()) {
			String value = formData.get(key);
			if (value != null) continue;

			formData.put(key, askForInput(key));
		}

		// Пользователь не указал дату выдачи документа в параметрах командной строки, попросить ввести
		if (formData.get("Документ.Выдан.День") == null ||
				formData.get("Документ.Выдан.Месяц") == null ||
				formData.get("Документ.Выдан.Год") == null) {
			formData.putAll(TextUtils.processDate(FormDate.ID_ISSUE, askForInput("Дата выдачи документа в формате ISO 8601: ")));
		}

		// Пользователь не указал дату генерации извещения, использовать текущую
		if (formData.get("Дата.Генерация") == null)
			formData.putAll(TextUtils.processDate(FormDate.GENERATION, null));

		// Пользователь не указал дату получения отправления, использовать текущую
		if (formData.get("Дата.Получение.День") == null ||
				formData.get("Дата.Получение.Месяц") == null ||
				formData.get("Дата.Получение.Год") == null) {
			formData.putAll(TextUtils.processDate(FormDate.PICKUP, null));
		}

		// Загрузить значения в коллекцию
		GlobalOptions.INITIAL_FORM_DATA.putAll(formData);

		// Ожидать завершения инициализации pochtaDataFetcher
		pochtaDataFetcher.waitForInitialization();
	}

	/**
	 * Выводит на экран сообщение с помощью и завершает программу
	 */
	private void printHelpAndExit() {
		try {
			System.out.printf("pochtaizv v%s - Генератор извещений (форма 22) Почты России%n",
					getClass().getPackage().getImplementationVersion());

			System.out.println(IoUtils.readString(IoUtils.open("help")));
		} catch (IOException exception) {
			Logger.error("Ошибка при получении доступа к внутренним ресурсам!", exception);
		}

		System.exit(0);
	}

	/**
	 * Выводит сообщение и просит пользователя ввести данные с клавиатуры
	 * @param key Ключ шаблона извещения для замены. Если такого ключа не
	 *            существует, на экран выведется непосредственно key.
	 * @return Ввод пользователя или пустая строка, если установлен флаг {@link dev.kerastinell.pochtaizv.values.GlobalOptions#NO_INPUT}
	 */
	private String askForInput(String key) {
		if (GlobalOptions.NO_INPUT)
			return "";

		String message = key;
		switch (key) {
			case "Получатель.Имя":
				message = "Ф.И.О. получателя: ";
				break;
			case "Получатель.Адрес.Указанный":
				message = "Адрес получателя: ";
				break;
			case "Документ.Вид":
				message = "Вид документа, удостоверяющего личность: ";
				break;
			case "Документ.Серия":
				message = "Серия документа: ";
				break;
			case "Документ.Номер":
				message = "Номер документа: ";
				break;
			case "Документ.Выдан.Кем":
				message = "Кем выдан документ: ";
				break;
			case "Получатель.Адрес.Регистрация":
				message = "Адрес регистрации получателя: ";
				break;
		}

		Logger.ask(message);
		try {
			return userInput.readLine();
		} catch (IOException exception) {
			Logger.error("Невозможно считать строку!", exception);
			Logger.verbose("Будет возвращена пустая строка");
			return "";
		}
	}
}
