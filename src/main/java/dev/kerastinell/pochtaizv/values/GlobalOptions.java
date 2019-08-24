package dev.kerastinell.pochtaizv.values;

import java.util.ArrayList;
import java.util.HashMap;

public class GlobalOptions {
	public static boolean QUIET = false; // Отвечает за отсутствие сообщений в консоли
	public static boolean EMPTY = false; // Отвечает за генерацию пустого извещения
	public static boolean OFFLINE = false; // Отвечает за работоспособность PochtaDataFetcher
	public static boolean NO_INPUT = false; // Отвечает за отсутствие запросов ввода данных

	// Эти данные не изменяются в зависимости от кода отслеживания отправлений.
	// Ф.И.О. получателя, его адрес, данные документа, удостоверяющего личность
	public static final HashMap<String, String> INITIAL_FORM_DATA = new HashMap<>();

	// Список кодов отслеживания отправлений, для которых требуется сгенерировать извещения
	public static final ArrayList<String> TRACKING_CODES = new ArrayList<>();

	// User agent, используемый при запросах к серверу Почты России
	public static String API_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36";
}
