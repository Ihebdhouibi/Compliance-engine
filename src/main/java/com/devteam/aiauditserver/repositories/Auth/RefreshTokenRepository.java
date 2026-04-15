package com.devteam.aiauditserver.repositories.Auth;



import com.devteam.aiauditserver.models.Auth.RefreshToken;
import com.devteam.aiauditserver.models.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    @Override
    Optional<RefreshToken> findById(Long id);

    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUser(User user);
    int deleteByUser(User user);
    int deleteAllByUser(User user);
    List<RefreshToken> findByUser_Id(Long id);
}
