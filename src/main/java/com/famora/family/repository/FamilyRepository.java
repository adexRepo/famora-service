package com.famora.family.repository;

import com.famora.family.entity.Family;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

}
