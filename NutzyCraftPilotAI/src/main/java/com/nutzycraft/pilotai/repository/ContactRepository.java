package com.nutzycraft.pilotai.repository;

import com.nutzycraft.pilotai.entity.ContactSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<ContactSubmission, UUID> {
}
