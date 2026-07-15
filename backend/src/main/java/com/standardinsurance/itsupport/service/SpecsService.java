package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.dto.SpecDtos.SpecContent;
import com.standardinsurance.itsupport.dto.SpecDtos.SpecSummary;
import com.standardinsurance.itsupport.exception.BadRequestException;
import com.standardinsurance.itsupport.exception.NotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Serves markdown spec files with a path-traversal guard. See docs/specs/07-specs-viewer.md. */
@Service
public class SpecsService {

    private static final Pattern SAFE_NAME = Pattern.compile("^[0-9A-Za-z._-]+\\.md$");

    private final Path specsDir;

    public SpecsService(@Value("${app.specs.dir}") String specsDir) {
        this.specsDir = Path.of(specsDir).toAbsolutePath().normalize();
    }

    public List<SpecSummary> list() {
        if (!Files.isDirectory(specsDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(specsDir)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(p -> new SpecSummary(p.getFileName().toString(), titleOf(p)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public SpecContent read(String name) {
        Path file = resolveSafely(name);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("Spec not found: " + name);
        }
        try {
            String markdown = Files.readString(file);
            return new SpecContent(name, titleOf(file), markdown);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Rejects bad names and any path that escapes the specs directory. */
    private Path resolveSafely(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw new BadRequestException("Invalid spec name: " + name);
        }
        Path resolved = specsDir.resolve(name).normalize();
        if (!resolved.startsWith(specsDir)) {
            throw new BadRequestException("Invalid spec path: " + name);
        }
        return resolved;
    }

    private String titleOf(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.filter(l -> l.startsWith("# "))
                    .findFirst()
                    .map(l -> l.substring(2).trim())
                    .orElse(file.getFileName().toString());
        } catch (IOException e) {
            return file.getFileName().toString();
        }
    }
}
