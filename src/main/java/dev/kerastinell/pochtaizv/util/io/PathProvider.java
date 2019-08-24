package dev.kerastinell.pochtaizv.util.io;

import dev.kerastinell.pochtaizv.util.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;

/**
 * Синглтон для предоставления доступа к внутренним ресурсам.
 * Доступ к классу есть только у {@link dev.kerastinell.pochtaizv.util.io.IoUtils}.
 */
class PathProvider {
	private boolean isJar; // Флаг, указывающий на нахождение ресурсов внутри .jar
	private FileSystem jarFS; // Реализация файловой системы внутри .jar

	private static final PathProvider instance = new PathProvider();
	static PathProvider getInstance() {
		return instance;
	}

	/**
	 * При вызове конструктора проверяется, находятся ли ресурсы в .jar (т.е. программа запущена сама по себе)
	 * или в иной директории в распакованном виде (т.е. программа запущена в среде разработки). Если ресурсы
	 * находятся в .jar, то устанавливается соответствующий флаг и единоразово инициализируется jarFS для
	 * предоставления дальнейшего доступа.
	 */
	private PathProvider() {
		try {
			URI rootUri = getClass().getResource("").toURI();
			if ((isJar = rootUri.getScheme().equals("jar")))
				jarFS = FileSystems.newFileSystem(rootUri, Collections.emptyMap());
		} catch (IOException | URISyntaxException exception) {
			Logger.error("Невозможно получить доступ к внутренним ресурсам!", exception);
		}
	}

	/**
	 * Получает доступ к внутреннему ресурсу.
	 * @param name Путь к внутреннему ресурсу или имя файла
	 * @return Path соответствующего ресурса
	 */
	Path get(String name) {
		try {
			return isJar? jarFS.getPath(name) : Paths.get(getClass().getResource(name).toURI());
		} catch (URISyntaxException exception) {
			Logger.error("Невозможно получить доступ к внутренним ресурсам!", exception);
			return null;
		}
	}
}
