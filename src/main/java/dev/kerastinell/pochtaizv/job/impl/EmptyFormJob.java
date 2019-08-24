package dev.kerastinell.pochtaizv.job.impl;

import dev.kerastinell.pochtaizv.job.AbstractJob;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Реализация генератора полностью пустого извещения.
 * @see AbstractJob
 */
public class EmptyFormJob extends AbstractJob {
	private final HashMap<String, String> formData;

	public EmptyFormJob() {
		super("");
		formData = new HashMap<>();

		for (String key : TEMPLATE_KEYS)
			formData.put(key, "");
	}

	@Override
	protected String getFileName() {
		return "Извещение.odg";
	}

	@Override
	protected HashMap<String, String> getTemplateData() {
		return formData;
	}

	@Override
	protected InputStream getBarcodeImage() {
		return null;
	}
}
