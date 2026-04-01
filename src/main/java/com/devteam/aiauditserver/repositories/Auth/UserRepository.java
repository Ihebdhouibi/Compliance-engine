package com.devteam.aiauditserver.repositories.Auth;

import com.devteam.aiauditserver.models.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByPhoneNumber(String phonenumber);
    User findByPhoneNumberContains(String phonenumber);
    boolean existsById(Long id);
    User findTopByOrderByIdDesc();
    User findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    List<User> findAllByPhoneNumberLikeIgnoreCase(String phonenumber);
    List<User> findAllByPhoneNumber(String phonenumber);
    boolean existsByPhoneNumber(String phonenumber);


}

