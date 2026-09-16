package com.sala.cassette.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sala.cassette.model.Recording;

public interface RecordingRepository extends JpaRepository<Recording, Long> {

    // This method returns the newest entries first so the UI can display the most recent recordings at the top.
    List<Recording> findAllByOrderByIdDesc();
}