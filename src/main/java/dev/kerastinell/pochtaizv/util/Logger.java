package dev.kerastinell.pochtaizv.util;

import dev.kerastinell.pochtaizv.values.GlobalOptions;

/**
 * Простейшая реализация логгера, использующая System.out для вывода текста.
 */
public class Logger {
	/**
	 * Выводит отладочное сообщение с кодом отслеживания отправления.
	 * Учитывает {@link dev.kerastinell.pochtaizv.values.GlobalOptions#QUIET}.
	 * @param trackingCode Код отслеживания отправления
	 * @param message Текст сообщения
	 */
	public static synchronized void track(String trackingCode, String message) {
		if (!GlobalOptions.QUIET)
			System.out.printf("[%s] %s%n", trackingCode, message);
	}

	/**
	 * Выводит сообщение.
	 * @param message Текст сообщения
	 */
	public static void ask(String message) {
		System.out.print(message);
	}

	/**
	 * Выводит сообщение, учитывая {@link dev.kerastinell.pochtaizv.values.GlobalOptions#QUIET}.
	 * @param message Текст сообщения
	 */
	public static void verbose(String message) {
		if (!GlobalOptions.QUIET)
			System.out.println(message);
	}

	/**
	 * Выводит ошибку в {@link System#err}, учитывая {@link dev.kerastinell.pochtaizv.values.GlobalOptions#QUIET}.
	 * @param message Текст ошибки
	 */
	public static void error(String message) {
		if (!GlobalOptions.QUIET)
			System.err.println(message);
	}

	/**
	 * Выводит ошибку в {@link System#err}, учитывая {@link dev.kerastinell.pochtaizv.values.GlobalOptions#QUIET}.
	 * @param message Текст ошибки
	 * @param exception Ошибка
	 */
	public static void error(String message, Exception exception) {
		if (!GlobalOptions.QUIET) {
			System.err.println(message);
			exception.printStackTrace();
		}
	}
}
