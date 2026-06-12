package com.devteam.aiauditserver.services.Oathloginservice;



import com.devteam.aiauditserver.Tools.util.UserPrincipal;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.repositories.Auth.UserRepository;
import com.devteam.aiauditserver.responses.Response.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        try {
            User user = userRepository.findByEmail(email);
            if(user == null) {
                user = userRepository.findByUsername(email);
            }

            return UserPrincipal.create(user);
        }catch (NoSuchElementException ex){
            throw  new ResourceNotFoundException("User"+"email"+email);
        }

    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User"+"id: "+ id)
        );

        return UserPrincipal.create(user);
    }
}
