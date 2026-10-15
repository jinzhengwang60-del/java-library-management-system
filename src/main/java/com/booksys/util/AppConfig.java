package com.booksys.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/** Read a private UTF-8 configuration file once; environment variables take priority. */
public final class AppConfig {
    private static final Properties SETTINGS = load();
    private AppConfig() {}

    private static Properties load() {
        Properties settings = new Properties();
        String path = System.getProperty("booksys.config");
        if (path == null || path.isBlank()) path = System.getenv("BOOKSYS_CONFIG");
        if (path != null && !path.isBlank()) {
            try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
                settings.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read private application configuration", e);
            }
        }
        return settings;
    }

    public static String get(String property, String environment, String fallback) {
        String value = System.getenv(environment);
        return value == null ? SETTINGS.getProperty(property, fallback) : value;
    }
}
