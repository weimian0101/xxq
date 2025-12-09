package com.example.gdms.config;

import com.example.gdms.user.UserRepository;
import com.example.gdms.user.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import com.example.gdms.menu.Menu;
import com.example.gdms.menu.MenuRepository;

@Configuration
public class DataInitializer {
    private final UserRepository userRepository;
    private final UserService userService;
    private final MenuRepository menuRepository;

    public DataInitializer(UserRepository userRepository, UserService userService, MenuRepository menuRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.menuRepository = menuRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (userRepository.count() == 0) {
            userService.createUser("admin", "admin123", "ADMIN", "系统管理员");
            userService.createUser("teacher", "teacher123", "TEACHER", "示例教师");
            userService.createUser("student", "student123", "STUDENT", "示例学生");
        }
        if (menuRepository.count() == 0) {
            menuRepository.save(menu("仪表盘", "/dashboard", "ADMIN", 1));
            menuRepository.save(menu("课题管理", "/topics", "ADMIN", 2));
            menuRepository.save(menu("阶段任务", "/stage", "ADMIN", 3));
            menuRepository.save(menu("公告", "/ann", "ADMIN", 4));
            menuRepository.save(menu("申请审批", "/applications", "ADMIN", 5));
            menuRepository.save(menu("答辩分组", "/defense", "ADMIN", 6));
            menuRepository.save(menu("组织管理", "/orgs", "ADMIN", 7));
            menuRepository.save(menu("菜单管理", "/menus", "ADMIN", 8));
            menuRepository.save(menu("阶段配置", "/stage-admin", "ADMIN", 9));

            menuRepository.save(menu("课题管理", "/topics", "TEACHER", 1));
            menuRepository.save(menu("阶段任务", "/stage", "TEACHER", 2));
            menuRepository.save(menu("公告", "/ann", "TEACHER", 3));
            menuRepository.save(menu("申请审批", "/applications", "TEACHER", 4));
            menuRepository.save(menu("答辩任务", "/defense", "TEACHER", 5));
            menuRepository.save(menu("我的学生", "/my-students", "TEACHER", 6));

            menuRepository.save(menu("课题选择", "/topics", "STUDENT", 1));
            menuRepository.save(menu("阶段任务", "/stage", "STUDENT", 2));
            menuRepository.save(menu("公告", "/ann", "STUDENT", 3));
            menuRepository.save(menu("申请", "/applications", "STUDENT", 4));
        }
        ensureMenu("ADMIN", "申请审批", "/applications", 5);
        ensureMenu("ADMIN", "答辩分组", "/defense", 6);
        ensureMenu("ADMIN", "组织管理", "/orgs", 7);
        ensureMenu("ADMIN", "菜单管理", "/menus", 8);
        ensureMenu("ADMIN", "阶段配置", "/stage-admin", 9);
        ensureMenu("TEACHER", "申请审批", "/applications", 4);
        ensureMenu("TEACHER", "答辩任务", "/defense", 5);
        ensureMenu("STUDENT", "申请", "/applications", 4);
    }

    private Menu menu(String name, String path, String role, int order) {
        Menu m = new Menu();
        m.setName(name);
        m.setPath(path);
        m.setRole(role);
        m.setOrderIndex(order);
        return m;
    }

    private void ensureMenu(String role, String name, String path, int order) {
        if (menuRepository.existsByRoleAndPath(role, path)) return;
        menuRepository.save(menu(name, path, role, order));
    }
}

