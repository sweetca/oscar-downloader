package com.oscar.downloader.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class StorageUtils {

    private static final Pattern PATTERN = Pattern.compile("(?<count>\\d+)\\s*(?<unit>\\w+)");
    private static final Map<String, Long> UNITS_MAP = Collections.unmodifiableMap(new HashMap<String, Long>() {
        {
            put("kb", 1024L);
            put("mb", 1048576L);
            put("gb", 1073741824L);
            put("tb", 1099511627776L);
        }
    });

    private StorageUtils() {
    }

    public static long parseStorageLimit(String storageLimit) {
        Matcher matcher = PATTERN.matcher(storageLimit);
        if (matcher.find()) {
            long count = Long.parseLong(matcher.group("count"));
            String unit = matcher.group("unit").toLowerCase();
            if (!UNITS_MAP.containsKey(unit)) {
                throw new IllegalArgumentException("Unsupported storage unit: " + unit);
            }
            return count * UNITS_MAP.get(unit);
        } else {
            throw new IllegalArgumentException("Invalid format of storageLimit properties");
        }
    }

    @SneakyThrows
    public static long getDirectorySize(Path path) {
        final AtomicLong size = new AtomicLong(0);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

        });
        return size.get();
    }

    @SneakyThrows
    public static void deleteDirectory(Path path) {
        FileSystemUtils.deleteRecursively(path);
    }

    @SneakyThrows
    public static void createDirectory(Path path) {
        Files.createDirectories(path);
    }

    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

}
