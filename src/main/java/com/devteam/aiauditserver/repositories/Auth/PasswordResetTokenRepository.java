package com.devteam.aiauditserver.repositories.Auth;


import com.devteam.aiauditserver.models.Auth.PasswordResetToken;
import com.devteam.aiauditserver.models.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    PasswordResetToken findByUser(User user);
    List<PasswordResetToken> findAllByUser(User user);
    PasswordResetToken findByToken(String token);
}
