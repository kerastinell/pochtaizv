package dev.kerastinell.pochtaizv;

import dev.kerastinell.pochtaizv.util.Logger;
import dev.kerastinell.pochtaizv.util.io.IoUtils;
import uk.org.okapibarcode.backend.Code128;
import uk.org.okapibarcode.backend.HumanReadableLocation;
import uk.org.okapibarcode.output.Java2DRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Обертка над Code128 для создания штрихкодов.
 * @see uk.org.okapibarcode.backend.Code128
 */
public class PochtaBarcode extends Code128 {
	public PochtaBarcode() {
		super();

		setModuleWidth(2); // Ширина полосы
		setBarHeight(64); // Высота полосы
		setHumanReadableLocation(HumanReadableLocation.NONE); // Отключение отрисовки текста на изображении
	}

	/**
	 * Генерирует штрихкод.
	 * @param input Входные данные
	 * @return Поток с изображением штрихкода
	 */
	public InputStream generate(String input) {
		ByteArrayOutputStream pngOutputStream = null;
		InputStream barcodeImageStream;

		try {
			setContent(input);

			BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			Java2DRenderer renderer = new Java2DRenderer(image.createGraphics(), 1, Color.WHITE, Color.BLACK);
			renderer.render(this);

			pngOutputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", pngOutputStream);
			barcodeImageStream = new ByteArrayInputStream(pngOutputStream.toByteArray());
		} catch (IOException exception) {
			Logger.error("Ошибка при генерации штрихкода!", exception);
			barcodeImageStream = new ByteArrayInputStream(new byte[0]);
		} finally {
			IoUtils.closeOutputStream(pngOutputStream);
		}

		return barcodeImageStream;
	}
}
