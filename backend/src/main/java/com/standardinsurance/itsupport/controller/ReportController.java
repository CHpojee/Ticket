package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.service.ReportService;
import com.standardinsurance.itsupport.service.ReportService.ReportFile;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ticket report export. See docs/specs/06-generate-report.md. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/tickets")
    public ResponseEntity<byte[]> tickets(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false, defaultValue = "csv") String format) {
        ReportFile file = reportService.generate(from, to, category, status, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
