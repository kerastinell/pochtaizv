package dev.kerastinell.pochtaizv.job.impl;

import dev.kerastinell.pochtaizv.PochtaBarcode;
import dev.kerastinell.pochtaizv.PochtaDataFetcher;
import dev.kerastinell.pochtaizv.job.AbstractJob;
import dev.kerastinell.pochtaizv.values.GlobalOptions;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Реализация задачи заполнению извещения, опирающаяся на ввод пользователя
 * и данные об отправлении, полученные от серверов Почты России.
 */
public class NormalFormJob extends AbstractJob {
	private final PochtaDataFetcher pochtaDataFetcher;

	private final PochtaBarcode pochtaBarcode;
	private final HashMap<String, String> formData;

	public NormalFormJob(String trackingCode, PochtaDataFetcher pochtaDataFetcher) {
		super(trackingCode);
		this.pochtaDataFetcher = pochtaDataFetcher;

		pochtaBarcode = new PochtaBarcode();
		formData = new HashMap<>();
		formData.putAll(GlobalOptions.INITIAL_FORM_DATA);
	}

	@Override
	protected String getFileName() {
		String postfix = "(без кода отслеживания @" + System.currentTimeMillis() + ")";
		if (!trackingCode.isEmpty())
			postfix = trackingCode;

		return String.format("Извещение %s.odg", postfix);
	}

	@Override
	protected HashMap<String, String> getTemplateData() {
		formData.put("Код отслеживания", trackingCode);
		formData.putAll(pochtaDataFetcher.getApiData(trackingCode));
		return formData;
	}

	@Override
	protected InputStream getBarcodeImage() {
		return trackingCode.isEmpty() ? null : pochtaBarcode.generate(trackingCode);
	}
}
