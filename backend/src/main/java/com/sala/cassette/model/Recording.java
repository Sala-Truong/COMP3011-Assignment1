package com.sala.cassette.model;

public class Recording {
    private Long id;
    private String name;
    private String transcript;

    public Recording(Long id, String name, String transcript) {
        this.id = id;
        this.name = name;
        this.transcript = transcript;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getTranscript() {
        return transcript;
    }
}