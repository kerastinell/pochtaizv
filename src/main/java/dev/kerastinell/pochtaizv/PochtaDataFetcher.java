package dev.kerastinell.pochtaizv;

import dev.kerastinell.pochtaizv.util.Logger;
import dev.kerastinell.pochtaizv.util.TextUtils;
import dev.kerastinell.pochtaizv.util.io.IoUtils;
import dev.kerastinell.pochtaizv.values.Constants;
import dev.kerastinell.pochtaizv.values.GlobalOptions;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * Класс для взаимодействия с сервером Почты России и
 * загрузки оттуда данных об отправлениях.
 */
public class PochtaDataFetcher {
	private static final String URL_INIT = "https://www.pochta.ru/tracking";
	private static String URL_TRACKING; // Задаётся единожды при инициализации

	// Клиент для запросов к серверу. Сохраняет cookies.
	private final OkHttpClient httpClient;

	private final Thread initializationThread;

	public PochtaDataFetcher() {
		httpClient = new OkHttpClient();
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
	 * Завершает работу.
	 */
	public void finish() {
		httpClient.connectionPool().evictAll();
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
		Response initResponse = null;

		try {
			Logger.verbose("[PochtaDataFetcher] Инициализация");

			final Request initRequest = new Request.Builder()
					.url(URL_INIT)
					.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
					.addHeader("accept-language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
					.addHeader("sec-fetch-dest", "document")
					.addHeader("sec-fetch-mode", "navigate")
					.addHeader("sec-fetch-site", "none")
					.addHeader("sec-fetch-user", "?1")
					.addHeader("upgrade-insecure-requests", "1")
					.addHeader("user-agent", GlobalOptions.API_USER_AGENT)
					.build();

			initResponse = httpClient.newCall(initRequest).execute();
			if (!initResponse.isSuccessful())
				throw new RuntimeException("!initResponse.isSuccessful()");

			Logger.verbose("[PochtaDataFetcher] Извлечение ссылки для отслеживания отправлений");
			final String html = initResponse.body().string();
			final String urlKey = "getTrackingsByBarcodesUrl:\"";
			int idxBegin = html.indexOf(urlKey) + urlKey.length();
			int idxEnd = html.indexOf("\"", idxBegin);
			URL_TRACKING = html.substring(idxBegin, idxEnd);

			Logger.verbose("[PochtaDataFetcher] Инициализация завершена!");
		} catch (IOException exception) {
			Logger.error("Ошибка при инициализации! Будет включен оффлайн-режим", exception);

			GlobalOptions.OFFLINE = true;
		} finally {
			IoUtils.closeResponse(initResponse);
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

		Response trackingResponse = null;
		String json = "";

		// Строка trackingCode может быть пустой, если пользователь включил тихий режим и не указал коды отслеживания
		if (trackingCode.isEmpty() || GlobalOptions.OFFLINE)
			return apiData;

		try {
			Logger.track(trackingCode, "Запрашиваю данные об отправлении");

			final RequestBody trackingPostData = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("barcodes", trackingCode)
					.build();

			final Request trackingRequest = new Request.Builder()
					.url(URL_TRACKING)
					.addHeader("accept", "*/*")
					.addHeader("accept-language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
					.addHeader("sec-fetch-dest", "document")
					.addHeader("sec-fetch-mode", "navigate")
					.addHeader("sec-fetch-site", "none")
					.addHeader("sec-fetch-user", "?1")
					.addHeader("upgrade-insecure-requests", "1")
					.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36")
					.post(trackingPostData)
					.build();

			trackingResponse = httpClient.newCall(trackingRequest).execute();
			if (!trackingResponse.isSuccessful())
				throw new RuntimeException("!trackingResponse.isSuccessful()");

			json = trackingResponse.body().string();
			final JSONObject jTracking = new JSONObject(json)
					.optJSONArray("response")
					.optJSONObject(0);
			final JSONArray jTrackingHistory = jTracking.optJSONObject("trackingItem")
					.optJSONArray("trackingHistoryItemList");
			final JSONObject jForm22 = jTracking.optJSONObject("formF22Params");
			final JSONObject jPostOffice = jTracking.optJSONObject("officeSummary");

			// Определить статус отправления по истории отслеживания
			if (jTrackingHistory.isEmpty()) {
				Logger.track(trackingCode, "Сервер не вернул историю отслеживания!");
				return apiData;
			} else if (!isReadyForPickup(jTrackingHistory)) {
				Logger.track(trackingCode, "Отправление еще не прибыло в место вручения!");
				return apiData;
			}

			// В некоторых случаях сервер Почты России не включает в JSON объект formF22Params
			if (jForm22 == null) {
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

			// Для некоторых отправлений отсутствует информация о почтовом отделении
			if (jPostOffice == null) {
				Logger.track(trackingCode, "Сервер не вернул данные о почтовом отделении!");
				return apiData;
			}

			final JSONArray jPostOfficeSchedule = jPostOffice.optJSONArray("workingSchedule");
			final JSONArray jPostOfficePhones = jPostOffice.optJSONArray("phones");

			apiData.put("Получатель.Адрес.Выдача", formatPostOfficeAddress(
					jPostOffice.optString("addressSource"),
					jPostOfficeSchedule,
					jPostOfficePhones));
			apiData.put("Вызов курьера", jPostOfficePhones.optString(0));
		} catch (Exception exception) {
			Logger.error("Ошибка при обработке данных!", exception);
			Logger.error(json);
		} finally {
			IoUtils.closeResponse(trackingResponse);
		}

		return apiData;
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
