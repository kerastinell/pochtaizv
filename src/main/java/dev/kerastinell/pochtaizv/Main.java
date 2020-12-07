package dev.kerastinell.pochtaizv;

import dev.kerastinell.pochtaizv.job.impl.*;
import dev.kerastinell.pochtaizv.util.Logger;
import dev.kerastinell.pochtaizv.values.GlobalOptions;

import java.util.concurrent.*;

public class Main {
	public static void main(String[] args) {
		new Main().run(args);
	}

	/**
	 * Запускает основную логику программы.
	 * @param args Параметры командной строки
	 */
	private void run(String[] args) {
		PochtaDataFetcher pochtaDataFetcher = new PochtaDataFetcher();

		// Загрузить данные для заполнения извещения из параметров командной строки
		new InitialFormDataJob(args, pochtaDataFetcher).run();

		// Запуск параллельных задач по генерации извещений
		final ExecutorService jobExecutor = Executors.newFixedThreadPool(3);
		if (GlobalOptions.EMPTY)
			jobExecutor.execute(new EmptyFormJob());
		else for (String trackingCode : GlobalOptions.TRACKING_CODES)
			jobExecutor.execute(new NormalFormJob(trackingCode, pochtaDataFetcher));

		// Ожидание завершения задач
		jobExecutor.shutdown();
		try {
			jobExecutor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException exception) {
			Logger.error("Ожидание завершения задач прервано!", exception);
		}
		pochtaDataFetcher.finish();
	}
}
