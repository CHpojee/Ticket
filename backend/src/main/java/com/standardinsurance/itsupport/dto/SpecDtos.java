package com.standardinsurance.itsupport.dto;

/** Specs Viewer payloads. See docs/specs/07-specs-viewer.md. */
public final class SpecDtos {

    private SpecDtos() {
    }

    public record SpecSummary(String name, String title) {
    }

    public record SpecContent(String name, String title, String markdown) {
    }
}
