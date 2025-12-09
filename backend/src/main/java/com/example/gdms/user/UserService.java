package com.example.gdms.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String username, String rawPassword, String role, String fullName) {
        return createUser(username, rawPassword, role, fullName, null, null, null, true);
    }

    public User createUser(String username, String rawPassword, String role, String fullName, String phone, String signatureUrl, Long orgId, Boolean enabled) {
        User u = new User();
        u.setUsername(username);
        // 直接存储明文密码
        u.setPassword(rawPassword);
        u.setRole(role);
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setSignatureUrl(signatureUrl);
        u.setOrgId(orgId);
        u.setEnabled(enabled == null ? true : enabled);
        return userRepository.save(u);
    }

    public User updateUser(Long id, UserController.UserUpdateRequest req) {
        User u = userRepository.findById(id).orElseThrow();
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.signatureUrl() != null) u.setSignatureUrl(req.signatureUrl());
        if (req.role() != null) u.setRole(req.role());
        if (req.orgId() != null) u.setOrgId(req.orgId());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        return userRepository.save(u);
    }

    public User setEnabled(Long id, boolean enabled) {
        User u = userRepository.findById(id).orElseThrow();
        u.setEnabled(enabled);
        return userRepository.save(u);
    }

    public User resetPassword(Long id, String rawPassword) {
        User u = userRepository.findById(id).orElseThrow();
        // 直接存储明文密码
        u.setPassword(rawPassword);
        return userRepository.save(u);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User updateProfile(String username, UserController.UserUpdateRequest req) {
        User u = getByUsername(username);
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.signatureUrl() != null) u.setSignatureUrl(req.signatureUrl());
        return userRepository.save(u);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled() != null && user.getEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    @Transactional
    public void batchUpdateRole(List<Long> userIds, String role) {
        List<User> users = userRepository.findAllById(userIds);
        users.forEach(u -> u.setRole(role));
        userRepository.saveAll(users);
    }

    public Page<User> findUsers(Pageable pageable, String keyword, String role, Long orgId, Boolean enabled) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                Predicate usernamePred = cb.like(root.get("username"), pattern);
                Predicate fullNamePred = cb.like(root.get("fullName"), pattern);
                Predicate phonePred = cb.like(root.get("phone"), pattern);
                predicates.add(cb.or(usernamePred, fullNamePred, phonePred));
            }
            
            if (role != null && !role.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            
            if (orgId != null) {
                predicates.add(cb.equal(root.get("orgId"), orgId));
            }
            
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return userRepository.findAll(spec, pageable);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void batchDeleteUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("用户ID列表不能为空");
        }
        userRepository.deleteAllById(userIds);
    }
}

