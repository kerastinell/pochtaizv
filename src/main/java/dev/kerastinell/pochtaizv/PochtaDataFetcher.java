package dev.kerastinell.pochtaizv;

import dev.kerastinell.pochtaizv.util.Logger;
import dev.kerastinell.pochtaizv.util.TextUtils;
import dev.kerastinell.pochtaizv.util.io.IoUtils;
import dev.kerastinell.pochtaizv.values.Constants;
import dev.kerastinell.pochtaizv.values.GlobalOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

/**
 * Класс для взаимодействия с сервером Почты России и
 * загрузки оттуда данных об отправлениях.
 */
public class PochtaDataFetcher {
	private static final String URL_COOKIES = "https://www.pochta.ru/tracking";
	private static String URL_TRACKING; // Задаётся единожды при инициализации

	private final HashMap<String, String> headers;

	private final  Thread initializationThread;

	public PochtaDataFetcher() {
		// Изначальный набор заголовков для запроса cookies. После получения
		// cookies добавятся некоторые другие заголовки, чтобы работало отслеживание
		// почтовых отправлений
		headers = new HashMap<>();
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		headers.put("Accept-Encoding", "gzip, deflate, br");
		headers.put("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3");
		headers.put("User-Agent", GlobalOptions.API_USER_AGENT);

		initializationThread = new Thread(this::getCookies, "PochtaDataFetcher Initialization Thread");
	}

	/**
	 * Запускает поток инициализации.
	 */
	public void initialize() {
		if (!GlobalOptions.OFFLINE)
			initializationThread.start();
	}

	/**
	 * Первично обращается к серверу Почты России, сохраняет cookies
	 * и извлекает API endpoint для отслеживания отправлений.
	 *
	 * Функция выполняется в отдельном потоке.
	 *
	 * В случае ошибки устанавливается флаг {@link GlobalOptions#OFFLINE}
	 */
	private void getCookies() {
		try {
			Logger.verbose("[PochtaDataFetcher] Запрос cookies");
			final StringJoiner cookies = new StringJoiner("; ");
			final HttpsURLConnection conn = connect(URL_COOKIES);

			// Извлечь cookies из заголовка ответа сервера
			conn.getHeaderFields().get("Set-Cookie").forEach(cookies::add);

			// Помимо загруженных cookies надо дополнительно добавить:
			cookies.add("HeaderBusinessTooltip=showed");

			// Добавить cookies и некоторые другие параметры к заголовкам последующих запросов
			headers.put("Cookie", cookies.toString());
			headers.put("TE", "Trailers");
			headers.put("Referer", "https://www.pochta.ru/tracking");
			headers.put("X-Requested-With", "XMLHttpRequest");

			Logger.verbose("[PochtaDataFetcher] Извлечение ссылки для отслеживания отправлений");
			final String html = readResponse(conn);
			int beginIndex = html.indexOf("getUrl:\"") + "getUrl:\"".length();
			int endIndex = html.indexOf("\"", beginIndex);
			URL_TRACKING = html.substring(beginIndex, endIndex);

			Logger.verbose("[PochtaDataFetcher] Инициализация завершена!");
		} catch (IOException exception) {
			Logger.error("Ошибка при инициализации! Будет включен оффлайн-режим", exception);

			GlobalOptions.OFFLINE = true;
		}
	}

	/**
	 * Ожидает завершения потока инициализации.
	 * В случае ошибки устанавливается флаг {@link GlobalOptions#OFFLINE}
	 */
	public void waitForInitialization() {
		if (!GlobalOptions.OFFLINE) try {
			initializationThread.join();
		} catch (InterruptedException exception) {
			Logger.error("Ожидание инициализации прервано! Будет включен оффлайн-режим", exception);

			GlobalOptions.OFFLINE = true;
		}
	}

	/**
	 * Загружает информацию об отправлении с сервера Почты России.
	 * @param trackingCode Код отслеживания отправления
	 * @return Коллекция с данными для замены в шаблоне извещения
	 */
	public HashMap<String, String> getApiData(String trackingCode) {
		// Инициализация HashMap значениями по-умолчанию
		final HashMap<String, String> apiData = TextUtils.initHashMap("",
				"Откуда", "Вид и категория", "С уведомлением", "Срок хранения", "Разряд",
				"Масса", "Объявленная ценность", "Наложенный платеж", "Плата за возврат",
				"Плата за досыл", "Таможенная пошлина", "Получатель.Адрес.Выдача", "Вызов курьера");

		String json = "";

		// Строка trackingCode может быть пустой, если пользователь включил тихий режим и не указал коды отслеживания
		if (trackingCode.isEmpty() || GlobalOptions.OFFLINE)
			return apiData;

		try {
			Logger.track(trackingCode, "Запрашиваю данные об отправлении");

			@SuppressWarnings("StringBufferReplaceableByString")
			final String url = new StringBuilder(URL_TRACKING)
					.append("&barcodeList=").append(trackingCode)
					.append("&_=").append(System.currentTimeMillis()).toString();
			HttpsURLConnection conn = connect(url);

			json = readResponse(conn);
			final JSONObject jTracking = new JSONObject(json)
					.optJSONArray("list")
					.optJSONObject(0);
			final JSONArray jTrackingHistory = jTracking.optJSONObject("trackingItem")
					.optJSONArray("trackingHistoryItemList");
			final JSONObject jForm22 = jTracking.optJSONObject("formF22Params");

			// Определить статус отправления по истории отслеживания
			if (jTrackingHistory.isEmpty()) {
				Logger.track(trackingCode, "Сервер не вернул историю отслеживания!");
				return apiData;
			} else if (!isReadyForPickup(jTrackingHistory)) {
				Logger.track(trackingCode, "Отправление еще не прибыло в место вручения!");
				return apiData;
			}

			// В некоторых случаях сервер Почты России не включает в JSON объект formF22Params
			if (jTracking.isNull("formF22Params")) {
				Logger.track(trackingCode, "Сервер не вернул данные об отправлении!");
				return apiData;
			}

			apiData.put("Откуда", jForm22.optString("senderAddress"));
			apiData.put("Вид и категория", String.join(", ",
					jForm22.optString("MailTypeText"),
					jForm22.optString("MailCtgText")));
			apiData.put("С уведомлением", jForm22.optString("postmarkText")); // Код отслеживания: 41654063044609
			apiData.put("Срок хранения", TextUtils.formatDate(jForm22.optLong("endStorageDate", 0)));
			apiData.put("Разряд", jForm22.optString("MailRankText"));
			apiData.put("Масса", TextUtils.formatWeight(jForm22.optInt("WeightGr")));
			apiData.put("Объявленная ценность", TextUtils.formatCurrency(jForm22.optDouble("SummInsured", 0)));
			apiData.put("Наложенный платеж", TextUtils.formatCurrency(jForm22.optDouble("SummCashOnDelivery", 0)));
			apiData.put("Плата за возврат", TextUtils.formatCurrency(jForm22.optDouble("ReturningRate", 0))); // TODO а точно ли это ReturningRate?
			apiData.put("Плата за досыл", ""); // TODO Найти образцы с платой за досыл
			apiData.put("Таможенная пошлина", TextUtils.formatCurrency(jForm22.optDouble("CustomDuty", 0)));

			final JSONObject jPostOffice = jTracking.optJSONObject("officeSummary");
			final JSONArray jPostOfficeSchedule = jPostOffice.optJSONArray("workingSchedule");
			final JSONArray jPostOfficePhones = jPostOffice.optJSONArray("phones");

			apiData.put("Получатель.Адрес.Выдача", formatPostOfficeAddress(
					jPostOffice.optString("addressSource"),
					jPostOfficeSchedule,
					jPostOfficePhones));
			apiData.put("Вызов курьера", jPostOfficePhones.optString(0));

		} catch (Exception exception) {
			Logger.error("Ошибка при взаимодействии с сервером Почты России!", exception);
			Logger.error(json);
		}

		return apiData;
	}

	/**
	 * Устанавливает связь с удалённым сервером.
	 * @param url Адрес ресурса
	 * @return Подключение к серверу
	 * @throws IOException При ошибке подключения
	 */
	private HttpsURLConnection connect(String url) throws IOException {
		final URL endpoint = new URL(url);
		HttpsURLConnection connection = (HttpsURLConnection) endpoint.openConnection();
		headers.forEach(connection::addRequestProperty);

		return connection;
	}

	/**
	 * Считывает ответ сервера в строку.
	 * @param connection Подключение к серверу
	 * @return Ответ сервера
	 * @throws IOException При ошибке подключения
	 */
	private String readResponse(HttpsURLConnection connection) throws IOException {
		try (
				GZIPInputStream input = new GZIPInputStream(connection.getInputStream());
				ByteArrayOutputStream output = new ByteArrayOutputStream()
		) {
			IoUtils.transfer(input, output);
			return output.toString(StandardCharsets.UTF_8.name());
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Проверяет готовность отправления к вручению.
	 * @param history Массив с элементами истории отслеживания отправления
	 * @return флаг готовности отправления к вручению
	 */
	private boolean isReadyForPickup(JSONArray history) {
		for (int i = 0; i < history.length(); i++) {
			JSONObject jEntry = history.getJSONObject(i);
			if (jEntry.optInt("operationType") == Constants.POST_ARRIVED_TYPE &&
					jEntry.optInt("operationAttr") == Constants.POST_ARRIVED_ATTR)
				return true;
		}

		return false;
	}

	/**
	 * Форматирует адрес отделения Почты России,
	 * в котором получателя ожидает отправление.
	 * @param addressSource Первая строка
	 * @param arrays Массивы строк
	 * @return Отформатированная строка для замены в шаблоне извещения
	 */
	private String formatPostOfficeAddress(String addressSource, JSONArray... arrays) {
		final StringBuilder postOfficeAddress = new StringBuilder();
		postOfficeAddress.append(TextUtils.paragraph(addressSource));

		for (JSONArray array : arrays)
			IntStream.range(0, array.length())
					.mapToObj(array::optString)
					.map(TextUtils::paragraph)
					.forEachOrdered(postOfficeAddress::append);

		return postOfficeAddress.toString();
	}
}
