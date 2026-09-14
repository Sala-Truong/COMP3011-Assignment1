package com.sala.cassette.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sala.cassette.model.Recording;

public interface RecordingRepository
        extends JpaRepository<Recording, Long> {

    List<Recording> findAllByOrderByIdDesc();
}