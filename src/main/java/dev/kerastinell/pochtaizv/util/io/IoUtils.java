package dev.kerastinell.pochtaizv.util.io;

import dev.kerastinell.pochtaizv.util.Logger;
import okhttp3.Response;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Утилиты для ввода-вывода
 */
public class IoUtils {
	/**
	 * Копирует данные из одного потока в другой.
	 * Если один из потоков не объявлен, ничего не произойдет.
	 * @param src Поток, из которого читаются данные
	 * @param dst Поток, в который записываются данные
	 * @throws IOException При ошибке ввода-вывода
	 */
	public static void transfer(InputStream src, OutputStream dst) throws IOException {
		if (src == null || dst == null) {
			Logger.error((src == null? "Входной" : "Выходной") + " поток не объявлен!");
			return;
		}

		int len;
		final byte[] buffer = new byte[1024];
		while ((len = src.read(buffer)) != -1)
			dst.write(buffer, 0, len);

		dst.flush();
	}

	/**
	 * Пытается закрыть выходной поток.
	 * @param stream Выходной поток для закрытия. Если stream не определён, ничего не произойдет.
	 */
	public static void closeOutputStream(OutputStream stream) {
		if (stream == null)
			return;

		try { // Запись оставшихся в буфере данных
			stream.flush();
		} catch (IOException exception) {
			Logger.error("Ошибка при записи оставшихся данных!", exception);
		}

		try { // Закрытие потока
			stream.close();
		} catch (IOException exception) {
			Logger.error("Ошибка при закрытии потока!", exception);
		}
	}

	/**
	 * Пытается закрыть тело ответа сервера.
	 * @param response Ответ сервера. Если response не определён, ничего не произойдёт.
	 */
	public static void closeResponse(Response response) {
		if (response != null)
			response.close();
	}

	/**
	 * Получает доступ к вводу пользователя.
	 * @return BufferedReader, из которого считывается ввод пользователя
	 */
	public static BufferedReader getUserInputReader() {
		final Console console = System.console();
		if (console == null) { // В случае, если программа запущена из среды разработки
			return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		} else return new BufferedReader(console.reader());
	}

	/**
	 * Получает доступ к внутреннему ресурсу.
	 * @param name Путь к внутреннему ресурсу или имя файла
	 * @return Path соответствующего ресурса
	 */
	public static Path getPath(String name) {
		return PathProvider.getInstance().get(name);
	}

	/**
	 * Открывает внутренний ресурс на чтение и возвращает поток.
	 * @see IoUtils#open(String)
	 * @param path Путь к ресурсу
	 * @return Поток для чтения данных
	 */
	public static InputStream open(Path path) throws IOException {
		InputStream stream = open(path.toString());
		if (stream == null) // случается при запуске в среде разработки
			stream = Files.newInputStream(path);
		return stream;
	}

	/**
	 * Открывает внутренний ресурс на чтение и возвращает поток.
	 * @param name Путь к ресурсу
	 * @return Поток для чтения данных
	 */
	public static InputStream open(String name) {
		if (name.startsWith("/")) name = name.substring(1);
		return ClassLoader.getSystemResourceAsStream(name);
	}

	/**
	 * Считывает строку в кодировке UTF-8 из потока.
	 * @param input Входной поток
	 * @return Считанная строка
	 * @throws IOException При ошибке чтения
	 */
	public static String readString(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		transfer(input, output);
		return output.toString(StandardCharsets.UTF_8.name());
	}
}
