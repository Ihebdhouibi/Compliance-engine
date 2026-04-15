package com.devteam.aiauditserver.controllers.Auth;



import com.devteam.aiauditserver.Tools.exception.NotFoundException;
import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.Tools.util.TokenUtil;
import com.devteam.aiauditserver.enums.User.RoleEnum;
import com.devteam.aiauditserver.models.Auth.PasswordResetToken;
import com.devteam.aiauditserver.models.Auth.UserDevice;
import com.devteam.aiauditserver.models.User.CompanyInfo;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
import com.devteam.aiauditserver.requests.Auth.LogOutRequest;
import com.devteam.aiauditserver.requests.Auth.SignInRequest;
import com.devteam.aiauditserver.requests.Auth.signupRequest;
import com.devteam.aiauditserver.responses.Response.ApiResponse;
import com.devteam.aiauditserver.responses.Response.JwtResponse;
import com.devteam.aiauditserver.responses.Response.TokenResponse;
import com.devteam.aiauditserver.services.auth.PasswordResetTokenServices;
import com.devteam.aiauditserver.services.auth.RefreshTokenService;
import com.devteam.aiauditserver.services.auth.UserDeviceService;
import com.devteam.aiauditserver.services.auth.UserService;
import com.devteam.aiauditserver.services.project.MailsSenderService;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.io.IOException;
import java.util.NoSuchElementException;


@RestController
@CrossOrigin
@RequestMapping(value = "/api/v1/auth")
public class AuthController extends BaseController {
    private final Log logger = LogFactory.getLog(AuthController.class);

    @Autowired
    private UserDeviceService userDeviceService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordResetTokenServices resetTokenServices;

    @Autowired
    private UserDeviceService deviceService;
    @Autowired
    private MailsSenderService mailsSenderService;



    @PostMapping(value = {"/signin"})
    public ResponseEntity<JwtResponse> signin(@RequestBody(required = false) SignInRequest signInRequest) throws JSONException {
        if (!this.userService.existByEmail(signInRequest.getUsername()))
            return new ResponseEntity("Wrong userName ", HttpStatus.BAD_REQUEST);
        User usar = this.userService.findbyemail(signInRequest.getUsername());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(signInRequest.getPassword(), usar.getPassword()))
            return new ResponseEntity("Wrong Password ", HttpStatus.BAD_REQUEST);
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signInRequest.getUsername().toLowerCase(), signInRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = userService.loadUserByUsername(signInRequest.getUsername().toLowerCase());
        User user = userService.findByUserName(userDetails.getUsername());
        if (user == null)
            return new ResponseEntity("this user not found", HttpStatus.NOT_FOUND);
        TokenResponse token = tokenUtil.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername()).getToken();
        RoleEnum roles;
        try {

            roles = user.getRole();
            System.out.println("roles");
            System.out.println(roles);
        } catch (NoSuchElementException ex) {
            System.out.println("role33s");
            System.out.println(ex);
            throw new NotFoundException(String.format("user messing data"));
        }

        if (user.getDevice() != null) {

            UserDevice device = user.getDevice();
            device.setDeviceType(signInRequest.getDeviceType());
            device.setDeviceId(signInRequest.getDeviceId());
            device.setIp(signInRequest.getIp());
            device.setTokendevice(signInRequest.getTokendevice());
            device.setToken(signInRequest.getTokendevice());
            UserDevice result_device = this.deviceService.update(device);
            user.setDevice(result_device);

        } else {
            UserDevice device = new UserDevice(signInRequest.getDeviceType(), signInRequest.getDeviceId(), signInRequest.getIp(), token.getToken(), signInRequest.getTokendevice());
            UserDevice result_device = this.deviceService.save(device);
            user.setDevice(result_device);
        }
        user = this.userService.save(user);
        JwtResponse response = new JwtResponse(token.getToken(), refreshToken, roles, user.getDevice().getDeviceId(), user.getDevice().getDeviceType(), user.getDevice().getIp(), token.getExpirationdate());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PostMapping(value = {"/reset_password_first_step"})
    public ResponseEntity reset_password_first_step(@RequestParam("email") String email) throws IOException, MessagingException {
        if (!this.userService.existByEmail(email.toLowerCase()))
            return new ResponseEntity("user not found", HttpStatus.NOT_FOUND);
        User user = userService.findbyemail(email.toLowerCase());
        String token = UserService.generateRandomPassword(4);
        PasswordResetToken resetToken = this.resetTokenServices.findbyuser(user);
        if (resetToken != null) {
            this.resetTokenServices.remove_code(resetToken.getUser());
        }
        resetToken = this.resetTokenServices.createPasswordResetTokenForUser(user, token);




        return new ResponseEntity("code send it to email , the code wille be expired in : " + resetToken.getExpiryDate(), HttpStatus.OK);
    }


    @PostMapping(value = {"/validate_reset_code_second_step"})
    public ResponseEntity<String> validate_reset_code_second_step(@RequestParam("email") String email, @RequestParam("reset_code") String code) throws IOException {
        if (!this.userService.existByEmail(email.toLowerCase()))
            return new ResponseEntity("user not found", HttpStatus.NOT_FOUND);
        User user = userService.findbyemail(email.toLowerCase());
        PasswordResetToken resetToken = this.resetTokenServices.findbyuser(user);
        if (resetToken == null) {
            return new ResponseEntity("this code is not found", HttpStatus.NOT_FOUND);
        }
        String result = this.resetTokenServices.validatePasswordResetToken(user, code);
        if (result == null)
            return new ResponseEntity<>("code correct", HttpStatus.OK);
        if (result.equals("expired"))
            return new ResponseEntity("this code is expired", HttpStatus.NOT_ACCEPTABLE);
        if (result.equals("invalidToken"))
            return new ResponseEntity("this code is not valid", HttpStatus.NOT_ACCEPTABLE);
        return new ResponseEntity("this code is wrong", HttpStatus.NOT_ACCEPTABLE);
    }


    @PutMapping(value = {"/logout"})
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') or hasRole('SUPPLIER') or hasRole('SUB_SUPPLIER') or hasRole('SUB_ADMIN')")
    public ResponseEntity<ApiResponse> logoutUser(@Valid @RequestBody LogOutRequest logOutRequest) {
        User currentUser = userService.findByUserName(getCurrentUser().getUsername());
        if (!this.userDeviceService.existbytoken(logOutRequest.getToken()))
            return new ResponseEntity("user device not found", HttpStatus.BAD_REQUEST);

        if (this.userDeviceService.findbytoken(logOutRequest.getToken()) == null)
            return new ResponseEntity("user device not found", HttpStatus.BAD_REQUEST);
        UserDevice device = currentUser.getDevice();

        if (device != null) {
            device.setTokendevice(null);
            this.userDeviceService.update(device);
        } else {
            logger.error("Device is null for user: " + currentUser.getUsername());
        }

        return ResponseEntity.ok(new ApiResponse(true, "User has successfully logged out from the system!"));

    }

    @PostMapping(value = {"/change_password_final_step"})
    public ResponseEntity change_password_final_step(@RequestParam("email") String email, @RequestParam("reset_code") String code, @RequestParam("password") String newPassword) throws IOException {
        if (!this.userService.existByEmail(email.toLowerCase()))
            return new ResponseEntity("user not found", HttpStatus.NOT_FOUND);
        User user = userService.findbyemail(email.toLowerCase());
        PasswordResetToken resetToken = this.resetTokenServices.findbyuser(user);
        if (resetToken == null) {
            return new ResponseEntity("this code is not found", HttpStatus.NOT_FOUND);
        }
        String result = this.resetTokenServices.validatePasswordResetToken(user, code);
        if (result == null) {
            user.setPassword(newPassword);
            this.userService.updatePassword(user);
            this.resetTokenServices.remove_code(resetToken.getUser());
            return new ResponseEntity("password updated successfully !!", HttpStatus.OK);
        }

        if (result.equals("expired"))
            return new ResponseEntity("this code is expired", HttpStatus.NOT_ACCEPTABLE);
        if (result.equals("invalidToken"))
            return new ResponseEntity("this code is not valid", HttpStatus.NOT_ACCEPTABLE);
        return new ResponseEntity("this code is wrong", HttpStatus.NOT_ACCEPTABLE);
    }

    @PostMapping(value = {"/signup"})
    public ResponseEntity<User> signUpUser(@RequestBody signupRequest signupRequest) throws JSONException {
        if (signupRequest.getEmail().isEmpty() || signupRequest.getPhoneNumber().isEmpty() )
            return new ResponseEntity("email, phone number are required",   HttpStatus.NOT_ACCEPTABLE);

        if (this.userService.existByEmail(signupRequest.getEmail()))
            return new ResponseEntity("Email already Exist ", HttpStatus.BAD_REQUEST);

        if (this.userService.existByPhoneNumber(signupRequest.getPhoneNumber()))
            return new ResponseEntity("phone number already Exist ", HttpStatus.BAD_REQUEST);

        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setUsername(userService.GenerateUserName(signupRequest.getFirstName(), userService.countUser()));
        String generatedPassword = UserService.generateRandomPassword(8);
        user.setPassword(generatedPassword);
        user.setName(signupRequest.getFirstName() + " " + signupRequest.getLastName());
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        CompanyInfo companyInfo = new CompanyInfo();
        companyInfo.setCompanyAddress(signupRequest.getCompanyAddress());
        companyInfo.setCompanyName(signupRequest.getCompanyName());
        companyInfo.setCompanyActivity(signupRequest.getCompanyActivity());
        companyInfo.setUser(user);
        user.setCompanyInfo(companyInfo);
        user =  userService.saveUser(user);

        this.mailsSenderService.sendWelcomePassword("Get Started with Compliance Engine Platform: Your Login Credentials", signupRequest.getEmail(), user, generatedPassword );

        return new ResponseEntity<>(user, HttpStatus.OK);
    }



}
