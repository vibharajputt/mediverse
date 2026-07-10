package com.mediverse.repository;

import com.mediverse.model.PharmacyMedicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PharmacyMedicineRepository extends JpaRepository<PharmacyMedicine, Long> {
    List<PharmacyMedicine> findByPharmacyId(Long pharmacyId);
    List<PharmacyMedicine> findByPrescriptionId(Long prescriptionId);
    List<PharmacyMedicine> findByPharmacyIdAndPrescriptionId(Long pharmacyId, Long prescriptionId);
}
