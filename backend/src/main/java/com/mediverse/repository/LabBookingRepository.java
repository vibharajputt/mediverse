package com.mediverse.repository;

import com.mediverse.model.LabBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabBookingRepository extends JpaRepository<LabBooking, Long> {
    List<LabBooking> findByPatientId(Long patientId);
    List<LabBooking> findByLabName(String labName);
    List<LabBooking> findByPrescriptionId(Long prescriptionId);
}
