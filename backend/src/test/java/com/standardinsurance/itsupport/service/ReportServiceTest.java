package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.entity.Ticket;
import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.BadRequestException;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.TicketRepository;
import com.standardinsurance.itsupport.service.ReportService.ReportFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCategoryRepository categoryRepository;
    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(ticketRepository, categoryRepository);
    }

    @Test
    void fromAfterToIsRejected() {
        assertThatThrownBy(() -> service.generate("2026-07-10", "2026-07-01", null, null, "csv"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void badDateIsRejected() {
        assertThatThrownBy(() -> service.generate("not-a-date", null, null, null, "csv"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownCategoryIsRejected() {
        when(categoryRepository.existsById("ZZ")).thenReturn(false);
        assertThatThrownBy(() -> service.generate(null, null, List.of("ZZ"), null, "csv"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> service.generate(null, null, null, List.of("Bogus"), "csv"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void csvContainsHeaderAndEscapesCommas() {
        Ticket t = ticket("Broken, printer");
        when(ticketRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(t));

        ReportFile file = service.generate(null, null, null, null, "csv");

        assertThat(file.filename()).endsWith(".csv");
        assertThat(file.contentType()).isEqualTo("text/csv");
        String csv = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Ticket ID,Title");
        assertThat(csv).contains("\"Broken, printer\"");
    }

    @Test
    void xlsxProducesNonEmptyWorkbook() {
        when(ticketRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ticket("Simple")));

        ReportFile file = service.generate(null, null, null, null, "xlsx");

        assertThat(file.filename()).endsWith(".xlsx");
        assertThat(file.content().length).isGreaterThan(0);
    }

    private Ticket ticket(String title) {
        TicketCategory sr = new TicketCategory("SR", "Service Request");
        User leiva = new User("1002", "x", "Leiva");
        return new Ticket(title, "desc", sr, leiva);
    }
}
