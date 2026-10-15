package com.booksys;

import com.booksys.util.ImageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageStoreTest {
    @TempDir Path directory;
    @Test void realImageGetsANewPngNameAndCanBeDecoded() throws Exception {
        byte[] bytes = image(16, 12);
        String first = ImageStore.store(directory, new ByteArrayInputStream(bytes), bytes.length, 4096);
        String second = ImageStore.store(directory, new ByteArrayInputStream(bytes), bytes.length, 4096);
        assertTrue(first.matches("[0-9a-f]{32}\\.png"));
        assertNotEquals(first, second);
        BufferedImage decoded = ImageIO.read(directory.resolve(first).toFile());
        assertEquals(16, decoded.getWidth());
        assertEquals(12, decoded.getHeight());
    }
    @Test void largeImageIsResizedToBoundDisplayAndStorageCosts() throws Exception {
        byte[] bytes = image(2048, 4);
        String name = ImageStore.store(directory, new ByteArrayInputStream(bytes), bytes.length, 1_000_000);
        BufferedImage decoded = ImageIO.read(directory.resolve(name).toFile());
        assertEquals(1024, decoded.getWidth());
        assertEquals(2, decoded.getHeight());
    }
    @Test void ScriptDisguisedAsImageAndOversizedBytesLeaveNoFile() throws Exception {
        byte[] script = "<% out.print(\"executable\"); %>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () ->
                ImageStore.store(directory, new ByteArrayInputStream(script), script.length, 4096));
        byte[] png = image(1, 1);
        assertThrows(IllegalArgumentException.class, () ->
                ImageStore.store(directory, new ByteArrayInputStream(png), png.length, 1));
        try (java.util.stream.Stream<Path> files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
    @Test void ExcessiveDimensionsAreRejectedBeforeDecodingPixels() throws Exception {
        byte[] png = image(8193, 1);
        assertThrows(IllegalArgumentException.class, () ->
                ImageStore.store(directory, new ByteArrayInputStream(png), png.length, 1_000_000));
    }
    static byte[] image(int width, int height) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
