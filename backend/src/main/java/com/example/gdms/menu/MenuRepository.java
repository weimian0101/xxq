package com.example.gdms.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByRoleOrderByOrderIndex(String role);
    boolean existsByRoleAndPath(String role, String path);
}

