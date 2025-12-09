package com.example.gdms.org;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrgRepository extends JpaRepository<Org, Long>, JpaSpecificationExecutor<Org> {
    Optional<Org> findByName(String name);
    Page<Org> findAll(Pageable pageable);
    List<Org> findByParentId(Long parentId);
    List<Org> findByParentIdIsNull();
    boolean existsByParentId(Long parentId);
}

