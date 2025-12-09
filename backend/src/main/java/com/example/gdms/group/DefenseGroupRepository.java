package com.example.gdms.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DefenseGroupRepository extends JpaRepository<DefenseGroup, Long>, JpaSpecificationExecutor<DefenseGroup> {
}

