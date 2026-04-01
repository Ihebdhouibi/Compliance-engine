package com.devteam.aiauditserver.services.auth;


import com.devteam.aiauditserver.Tools.exception.NotFoundException;
import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.Tools.util.UserPrincipal;
import com.devteam.aiauditserver.enums.User.Gender;
import com.devteam.aiauditserver.enums.User.RoleEnum;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
import com.devteam.aiauditserver.repositories.Auth.UserRepository;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.devteam.aiauditserver.requests.project.AddUserRequest;
import com.devteam.aiauditserver.requests.project.UpdateCoachProfile;
import com.devteam.aiauditserver.requests.project.UpdateUserProfile;
import com.devteam.aiauditserver.services.project.MailsSenderService;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.*;

@Service
public class UserService extends BaseController implements UserDetailsService {
    private final Log logger = LogFactory.getLog(UserService.class);
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private FilesStorageService filesStorageService;
    @Autowired
    private MailsSenderService mailsSenderService;

    public User save(User user) {
        try {
            return userRepository.save(user);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException("not found data");
        }
    }

    public User saveAdmin(User User) {
        try {
            User.setPassword(passwordEncoder.encode(User.getPassword()));
            User.setActive(true);
            User.setRole(RoleEnum.ROLE_ADMIN);
            return userRepository.save(User);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException("not found data");
        }
    }

    public User saveAuditor(User User) {
        try {
            User.setPassword(passwordEncoder.encode(User.getPassword()));
            User.setActive(true);
            User.setRole(RoleEnum.ROLE_AUDITOR);
            return userRepository.save(User);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException("not found data");
        }
    }

    public User saveUser(User User) {
        try {
            User.setPassword(passwordEncoder.encode(User.getPassword()));
            User.setActive(true);
            User.setRole(RoleEnum.ROLE_USER);
            return userRepository.save(User);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException("not found data");
        }
    }

    public void updatePassword(User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            this.userRepository.save(user);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException("not found data");
        }
    }

    public boolean existByEmail(String email) {
        try {
            return userRepository.existsByEmail(email);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }

    public boolean existByPhoneNumber(String phonenumber) {
        try {
            return userRepository.existsByPhoneNumber(phonenumber);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }

    public boolean existById(Long id) {
        try {
            return userRepository.existsById(id);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }

    public User findById(Long userid) {
        try {
            return userRepository.findById(userid).get();

        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }
    }



    public static String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            char randomChar = random.nextBoolean()
                    ? (char) (random.nextInt(26) + 'a')
                    : (char) (random.nextInt(10) + '0');
            sb.append(randomChar);
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Entering loadUserByUsername with identifier: " + username);

        // Attempt to find user using email
        logger.debug("Searching for user by username: " + username);
        User user = userRepository.findByUsername(username);
        // If still not found, attempt using phone number
        if (user == null) {
            logger.debug("No user found by email. Searching for user by phone number: " + username);
            user = userRepository.findByPhoneNumber(username);
        }
        // If not found by email, attempt using username
        if (user == null) {
            logger.debug("No user found by username. Searching for user by username: " + username);
            user = userRepository.findByEmail(username);
        }
        // Optionally create a new user if not found and identifier looks like an email
        if (user == null && username != null && username.contains("@")) {
            logger.info("User not found for identifier '" + username + "'. Creating a new user based on email format.");
            user = new User();
            user.setUsername(username);
            user.setActive(true);
            user.setRole(RoleEnum.ROLE_USER);
            user = userRepository.save(user);
            logger.info("New user created successfully for identifier: " + username);
        }

        if (user == null) {
            logger.error("User not found with identifier: " + username);
            throw new UsernameNotFoundException("User not found with identifier: " + username);
        }

        logger.debug("User found: " + user);
        UserDetails userDetails = UserPrincipal.create(user);
        logger.debug("UserDetails created: " + userDetails);
        return userDetails;
    }

    public String GenerateUserName(String name, Long iduser) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        String username = trimAllWhitespace(name).toUpperCase(Locale.ROOT) + iduser + sb;
        return username;

    }


    public static String trimAllWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }

        int len = str.length();
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }

    public User findByUserName(String username) {
        try {
            User user = this.userRepository.findByUsername(username);
            if (user == null)
                user = this.userRepository.findByEmail(username);
            return user;
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No user found with username [%s] in our data base", username));
        }

    }

    public User updateActivatedUser(Long userid) {
        User user = findById(userid);
        user.setActive(!user.getActive());
        if(user.getActive()){
            this.mailsSenderService.sendActivationEmail("Access Restored: Compliance Engine Platform", user.getEmail(), user );
        }else{
            this.mailsSenderService.sendDeactivationEmail("Notice of Account Deactivation - Compliance Engine Platform", user.getEmail(), user );
        }
        return this.userRepository.save(user);

    }

    public User findbyemail(String email) {
        try {
            return userRepository.findByEmail(email);
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }

    }

    public long countUser() {
        try {
            return userRepository.count();

        } catch (NoSuchElementException ex) {
            throw new NotFoundException(String.format("No data found"));
        }
    }

    public Page<User> findUsersByCriteria(int page, int size, Long id,
                                          String search, Gender gender,
                                          RoleEnum role, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());

        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> user = cq.from(User.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(user.get("deleted"), false));

        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = "%" + search.toLowerCase() + "%";
            Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(user.get("email")), lowerSearch),
                    cb.like(cb.lower(user.get("phoneNumber")), lowerSearch),
                    cb.like(cb.lower(user.get("name")), lowerSearch)
            );
            predicates.add(searchPredicate);
        }

        if (id != null) {
            predicates.add(cb.equal(user.get("id"), id));
        }

        if (gender != null) {
            predicates.add(cb.equal(user.get("gender"), gender));
        }
        if (role != null) {
            predicates.add(cb.equal(user.get("role"), role));
        }
        if (active != null) {
            predicates.add(cb.equal(user.get("active"), active));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(user.get("timestamp"))); // Maintain consistent order

        TypedQuery<User> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageRequest.getOffset());
        query.setMaxResults(pageRequest.getPageSize());

        List<User> resultList = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countRoot = countQuery.from(User.class);

        countQuery.select(cb.count(countRoot)).where(predicates.toArray(new Predicate[0]));
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageRequest, totalElements);
    }



    @Transactional
    public User updateMyProfileImage(User user, MultipartFile profileImage) {
        if(profileImage != null){
            user.setProfileimage(filesStorageService.save_file(profileImage, "users/profileImages"));
        }
        return userRepository.save(user);
    }

    public User addNewAuditor(AddUserRequest req) {
        String generatedPassword = UserService.generateRandomPassword(8);
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setUsername(this.GenerateUserName(req.getFirstName(), this.countUser()));
        user.setPassword(generatedPassword);
        user.setName(req.getFirstName() + " " + req.getLastName());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setGender(req.getGender());
        user =  this.saveAuditor(user);
        this.mailsSenderService.sendAuditorWelcomeEmail("Onboarding: Auditor Account Activation: Compliance Engine Platform", req.getEmail(), user, generatedPassword );
        return user;
    }

    public User UpdateAuditorProfile(Long userId , UpdateCoachProfile req) {

        User user = this.findById(userId);
        if(req.getEmail() != null)
            user.setEmail(req.getEmail());
        if(req.getPhoneNumber() != null)
            user.setPhoneNumber(req.getPhoneNumber());
        if(req.getFirstName() != null || req.getLastName() != null)
            user.setName(req.getFirstName() + " " + req.getLastName());
        if(req.getFirstName() != null)
            user.setFirstName(req.getFirstName());
        if(req.getLastName() != null)
            user.setLastName(req.getLastName());
        if(req.getGender() != null)
            user.setGender(req.getGender());
        user =  this.save(user);
        return user;
    }

    public User UpdateUserProfile(Long userId , UpdateUserProfile req) {

        User user = this.findById(userId);
        if(req.getEmail() != null)
            user.setEmail(req.getEmail());
        if(req.getPhoneNumber() != null)
            user.setPhoneNumber(req.getPhoneNumber());
        if(req.getFirstName() != null || req.getLastName() != null)
            user.setName(req.getFirstName() + " " + req.getLastName());
        if(req.getFirstName() != null)
            user.setFirstName(req.getFirstName());
        if(req.getLastName() != null)
            user.setLastName(req.getLastName());
       if(req.getCompanyActivity() != null)
           user.getCompanyInfo().setCompanyActivity(req.getCompanyActivity());
        if(req.getCompanyAddress() != null)
            user.getCompanyInfo().setCompanyAddress(req.getCompanyAddress());
        if(req.getCompanyName() != null)
            user.getCompanyInfo().setCompanyName(req.getCompanyName());
        user =  this.save(user);
        return user;
    }
}

