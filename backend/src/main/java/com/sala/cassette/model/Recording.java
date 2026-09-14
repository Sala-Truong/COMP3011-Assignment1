package com.sala.cassette.model;

public class Recording {

    private Long id;
    private String name;
    private String transcript;

    // Empty constructor for Spring
    public Recording() {}
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
    public void setId(Long id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
}