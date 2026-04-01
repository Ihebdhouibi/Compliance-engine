package com.devteam.aiauditserver.services.auth;



import com.devteam.aiauditserver.Tools.exception.NotFoundException;
import com.devteam.aiauditserver.Tools.exception.TokenRefreshException;
import com.devteam.aiauditserver.models.Auth.RefreshToken;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.repositories.Auth.RefreshTokenRepository;
import com.devteam.aiauditserver.repositories.Auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${auth.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    public RefreshToken updateToken(RefreshToken re) {
        try {
            return refreshTokenRepository.save(re);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException("not found data");
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    public List<RefreshToken> findbyuser(User user) {
        try {
            List<RefreshToken> result = this.refreshTokenRepository.findAllByUser(user);
            return result;
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format(ex.getMessage()));
        }

    }

    public RefreshToken createRefreshToken(String usename) {
        RefreshToken refreshToken = new RefreshToken();
        User user = this.userRepository.findByUsername(usename);
        if(user==null)
            user = this.userRepository.findByEmail(usename);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs*7));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    public List<RefreshToken> findByUserId(Long userid){
        return this.refreshTokenRepository.findByUser_Id(userid);
    }


    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }
 @Transactional
    public void deleteById(Long id) {
         this.refreshTokenRepository.deleteById(id);
    }
 @Transactional
    public int deleteAllByUserId(User user) {
        return refreshTokenRepository.deleteAllByUser(user);
    }
}
