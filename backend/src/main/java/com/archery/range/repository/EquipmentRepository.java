package com.archery.range.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archery.range.domain.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByOrderByEquipCodeAsc();

    List<Equipment> findByTypeOrderByEquipCodeAsc(String type);

    List<Equipment> findByStatusOrderByEquipCodeAsc(String status);

    boolean existsByEquipCode(String equipCode);
}
