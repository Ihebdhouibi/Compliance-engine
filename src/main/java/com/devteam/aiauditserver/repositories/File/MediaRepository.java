package com.devteam.aiauditserver.repositories.File;


import com.devteam.aiauditserver.models.File.MediaModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaModel,Long> {
    boolean existsById(Long id);



}
