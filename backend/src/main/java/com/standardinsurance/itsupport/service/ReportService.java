package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.entity.Ticket;
import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.exception.BadRequestException;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.TicketRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ticket export to CSV/XLSX with date/category/status filters. See docs/specs/06-generate-report.md. */
@Service
public class ReportService {

    private static final String[] HEADERS = {
            "Ticket ID", "Title", "Category Code", "Category Description", "Status",
            "Requestor ID", "Requestor Name", "Approver Name", "Created At", "Updated At"};

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;

    public ReportService(TicketRepository ticketRepository,
                         TicketCategoryRepository categoryRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
    }

    public record ReportFile(String filename, String contentType, byte[] content) {
    }

    @Transactional(readOnly = true)
    public ReportFile generate(String from, String to, List<String> categories,
                               List<String> statuses, String format) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        List<String> categoryCodes = validateCategories(categories);
        List<TicketStatus> statusEnums = validateStatuses(statuses);

        Specification<Ticket> spec = Specification
                .where(TicketSpecifications.createdFrom(
                        fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .and(TicketSpecifications.createdTo(
                        toDate == null ? null
                                : toDate.plusDays(1).atStartOfDay().minusNanos(1)
                                .toInstant(ZoneOffset.UTC)))
                .and(TicketSpecifications.categoryIn(categoryCodes))
                .and(TicketSpecifications.statusIn(statusEnums));
        List<Ticket> tickets = ticketRepository.findAll(spec, Sort.by("id").ascending());

        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd")
                .format(LocalDate.now(ZoneOffset.UTC));
        if (xlsx) {
            return new ReportFile("tickets_" + ts + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    toXlsx(tickets));
        }
        return new ReportFile("tickets_" + ts + ".csv", "text/csv", toCsv(tickets));
    }

    private byte[] toCsv(List<Ticket> tickets) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\r\n");
        for (Ticket t : tickets) {
            List<String> cells = row(t);
            sb.append(cells.stream().map(this::csvEscape)
                    .collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toXlsx(List<Ticket> tickets) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Tickets");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int r = 1;
            for (Ticket t : tickets) {
                Row row = sheet.createRow(r++);
                List<String> cells = row(t);
                for (int c = 0; c < cells.size(); c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(cells.get(c) == null ? "" : cells.get(c));
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> row(Ticket t) {
        List<String> cells = new ArrayList<>();
        cells.add(String.valueOf(t.getId()));
        cells.add(t.getTitle());
        cells.add(t.getCategory().getCode());
        cells.add(t.getCategory().getDescription());
        cells.add(t.getStatus().getLabel());
        cells.add(t.getRequestor().getUserId());
        cells.add(t.getRequestor().getName());
        cells.add(t.getApprover() == null ? "" : t.getApprover().getName());
        cells.add(String.valueOf(t.getCreatedAt()));
        cells.add(String.valueOf(t.getUpdatedAt()));
        return cells;
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("'" + field + "' is not a valid ISO date (yyyy-MM-dd)");
        }
    }

    private List<String> validateCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        for (String code : categories) {
            if (!categoryRepository.existsById(code)) {
                throw new BadRequestException("Unknown category code: " + code);
            }
        }
        return categories;
    }

    private List<TicketStatus> validateStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        List<TicketStatus> result = new ArrayList<>();
        for (String s : statuses) {
            TicketStatus status = TicketStatus.fromLabelOrName(s);
            if (status == null) {
                throw new BadRequestException("Unknown status: " + s);
            }
            result.add(status);
        }
        return result;
    }
}
