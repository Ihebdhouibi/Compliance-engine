package com.devteam.aiauditserver.repositories.Auth;

import com.devteam.aiauditserver.models.Auth.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByToken(String token);



    Boolean existsByToken(String token);


    @Query("SELECT u.ip, COUNT(u) FROM UserDevice u WHERE u.ip IS NOT NULL GROUP BY u.ip")
    List<Object[]> countUsersByIp();

}
