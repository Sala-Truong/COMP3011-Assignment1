package com.sala.cassette.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Recording {

    private Long id;
    private String title;
    private String date;
    private String duration;
    private String icon;
    private List<String> tags;
    private String accent;
    private String transcript;
    private String audioUrl;

    // Used internally by the backend only
    @JsonIgnore
    private String audioFilename;

    public Recording() {
    }

    public Recording(
            Long id,
            String title,
            String date,
            String duration,
            String icon,
            List<String> tags,
            String accent,
            String transcript,
            String audioUrl,
            String audioFilename) {

        this.id = id;
        this.title = title;
        this.date = date;
        this.duration = duration;
        this.icon = icon;
        this.tags = tags;
        this.accent = accent;
        this.transcript = transcript;
        this.audioUrl = audioUrl;
        this.audioFilename = audioFilename;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getDuration() {
        return duration;
    }
    public void setDuration(String duration) {
        this.duration = duration;
    }
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
    public List<String> getTags() {
        return tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    public String getAccent() {
        return accent;
    }
    public void setAccent(String accent) {
        this.accent = accent;
    }
    public String getTranscript() {
        return transcript;
    }
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
    public String getAudioUrl() {
        return audioUrl;
    }
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
    public String getAudioFilename() {
        return audioFilename;
    }
    public void setAudioFilename(String audioFilename) {
        this.audioFilename = audioFilename;
    }
}