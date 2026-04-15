package com.devteam.aiauditserver.services.auth;



import com.devteam.aiauditserver.Tools.exception.NotFoundException;
import com.devteam.aiauditserver.Tools.util.TokenUtil;
import com.devteam.aiauditserver.models.Auth.UserDevice;
import com.devteam.aiauditserver.repositories.Auth.UserDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.NoSuchElementException;

@Service
public class UserDeviceService {

    @Autowired
    private UserDeviceRepository repository;
    @Autowired
    private TokenUtil tokenUtil;

    public UserDevice findbytoken(String token) {
        try {
            return this.repository.findByToken(token).get();
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }


    public Boolean existbytoken(String token) {
        try {
            return this.repository.existsByToken(token);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }

    public void Delete(UserDevice id) {
        try {
            this.repository.delete(id);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }




    public UserDevice save(UserDevice model) {
        try {
            return this.repository.save(model);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }

    @Transactional
    public void deletebyid(Long id) {
        try {
            this.repository.deleteById(id);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }


    public UserDevice update(UserDevice model) {
        try {
            return this.repository.save(model);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }



}
