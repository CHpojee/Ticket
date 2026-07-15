package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.standardinsurance.itsupport.dto.SpecDtos.SpecContent;
import com.standardinsurance.itsupport.exception.BadRequestException;
import com.standardinsurance.itsupport.exception.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpecsServiceTest {

    @TempDir
    Path dir;
    private SpecsService service;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(dir.resolve("01-login.md"), "# User Login\n\nBody");
        Files.writeString(dir.resolve("02-audit.md"), "# Audit Trail\n");
        service = new SpecsService(dir.toString());
    }

    @Test
    void listsMarkdownFilesWithTitles() {
        var specs = service.list();
        assertThat(specs).hasSize(2);
        assertThat(specs.get(0).name()).isEqualTo("01-login.md");
        assertThat(specs.get(0).title()).isEqualTo("User Login");
    }

    @Test
    void readsFileContent() {
        SpecContent content = service.read("01-login.md");
        assertThat(content.markdown()).contains("Body");
        assertThat(content.title()).isEqualTo("User Login");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> service.read("../secret.md"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.read("..%2fsecret.md"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownFileReturns404() {
        assertThatThrownBy(() -> service.read("99-missing.md"))
                .isInstanceOf(NotFoundException.class);
    }
}
