package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.dto.SpecDtos.SpecContent;
import com.standardinsurance.itsupport.dto.SpecDtos.SpecSummary;
import com.standardinsurance.itsupport.service.SpecsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Specs Viewer backend. See docs/specs/07-specs-viewer.md. */
@RestController
@RequestMapping("/api/specs")
public class SpecsController {

    private final SpecsService specsService;

    public SpecsController(SpecsService specsService) {
        this.specsService = specsService;
    }

    @GetMapping
    public List<SpecSummary> list() {
        return specsService.list();
    }

    @GetMapping("/{name}")
    public SpecContent read(@PathVariable String name) {
        return specsService.read(name);
    }
}
