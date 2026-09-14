package com.sala.cassette.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

@Entity
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String date;
    private String duration;
    private String icon;
    private String accent;

    @Column(length = 10000)
    private String transcript;

    private String audioUrl;

    @JsonIgnore
    private String audioFilename;

    @ElementCollection
    @CollectionTable(
        name = "recording_tags",
        joinColumns = @JoinColumn(name = "recording_id")
    )
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();


    // Required by JPA
    public Recording() {
    }


    // Used when creating a new recording
    // ID is NOT included because H2 generates it
    public Recording(
            String title,
            String date,
            String duration,
            String icon,
            List<String> tags,
            String accent,
            String transcript,
            String audioUrl,
            String audioFilename) {

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