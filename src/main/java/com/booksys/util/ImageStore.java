package com.booksys.util;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ImageStore {
    private static final long MAX_PIXELS = 16_000_000;
    private ImageStore() {}

    public static Path directory() {
        String configured = System.getProperty("booksys.upload.dir",
                AppConfig.get("upload.directory", "BOOKSYS_UPLOAD_DIR", ""));
        return configured.isBlank()
                ? Paths.get(System.getProperty("catalina.base", System.getProperty("java.io.tmpdir")), "booksys-data", "uploads").toAbsolutePath().normalize()
                : Paths.get(configured).toAbsolutePath().normalize();
    }

    public static String upload(HttpServletRequest request, String field, long limit)
            throws IOException, javax.servlet.ServletException {
        String type = request.getContentType();
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) return null;
        Part part = request.getPart(field);
        if (part == null || part.getSize() == 0) return null;
        try (InputStream stream = part.getInputStream()) {
            return "/upload/" + store(directory(), stream, part.getSize(), limit);
        }
    }

    public static String store(Path directory, InputStream stream, long size, long limit) throws IOException {
        if (size <= 0 || size > limit) throw new IllegalArgumentException("图片大小超过限制");
        BufferedImage image;
        try (ImageInputStream input = ImageIO.createImageInputStream(stream)) {
            if (input == null) throw new IllegalArgumentException("无法读取图片");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("只允许上传 PNG、JPEG 或 GIF 图片");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Arrays.asList("png", "jpeg", "gif").contains(format))
                    throw new IllegalArgumentException("只允许上传 PNG、JPEG 或 GIF 图片");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || (long) width * height > MAX_PIXELS)
                    throw new IllegalArgumentException("图片尺寸过大");
                image = reader.read(0);
                if (image == null) throw new IllegalArgumentException("图片内容无效");
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException e) { throw new IllegalArgumentException("图片内容损坏", e); }
        int width = image.getWidth(), height = image.getHeight();
        if (Math.max(width, height) > 1024) {
            double scale = 1024.0 / Math.max(width, height);
            BufferedImage resized = new BufferedImage(Math.max(1, (int) (width * scale)),
                    Math.max(1, (int) (height * scale)), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = resized.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(image, 0, 0, resized.getWidth(), resized.getHeight(), null);
            } finally { graphics.dispose(); }
            image = resized;
        }
        Files.createDirectories(directory);
        String name = UUID.randomUUID().toString().replace("-", "") + ".png";
        Path path = directory.resolve(name);
        try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder unavailable");
        } catch (IOException e) { Files.deleteIfExists(path); throw e; }
        return name;
    }

    public static void discard(String stored) throws IOException {
        if (stored != null && stored.matches("/upload/[0-9a-f]{32}\\.png"))
            Files.deleteIfExists(directory().resolve(stored.substring("/upload/".length())));
    }
}
