package com.devteam.aiauditserver.controllers;

import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.requests.project.UpdateCoachProfile;
import com.devteam.aiauditserver.requests.project.UpdateUserProfile;
import com.devteam.aiauditserver.services.auth.UserDeviceService;
import com.devteam.aiauditserver.services.auth.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;

@RestController
@CrossOrigin
@RequestMapping(value = "/api/v1/users")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserDeviceService userDeviceService;


    @GetMapping(value = {"/current_user"})
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('USER') ")
    public ResponseEntity<User> current_user() throws IOException {
        String username = getCurrentUser().getUsername();
        try {
            User user = userService.findByUserName(username);
            return new ResponseEntity<>(user, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = {"/get_user/{userid}"})
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('JYM') ")
    public ResponseEntity<User> get_user_by_id(@PathVariable Long userid) throws IOException {
        if (!this.userService.existById(userid))
            return new ResponseEntity("user not exist", HttpStatus.NOT_FOUND);
        User result = this.userService.findById(userid);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PatchMapping(value = {"/update_user_image_profile/{userId}"})
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('GYM')")
    @ApiOperation(value = "update user details for user", notes = "Endpoint to update user profile")
    @ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Successfully updated"),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad request"),
            @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden"),
    })
    public ResponseEntity<User> update_my_profile(@PathVariable Long userId, @RequestParam MultipartFile image) {

        User user = this.userService.findById(userId);
        if(user == null){
            return new ResponseEntity("no user found with that id", HttpStatus.NOT_FOUND);
        }

        User updatedUser = userService.updateMyProfileImage(user, image);

        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping(value = {"/update_password"})
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('JYM') ")
    public ResponseEntity update_password(@NotNull @RequestParam("password") String password, @NotNull @RequestParam("oldpassword") String oldpassword) throws IOException {
        User user = this.userService.findByUserName(getCurrentUser().getUsername());
        if (user == null)
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(oldpassword, user.getPassword()))
            return new ResponseEntity("Wrong Password ", HttpStatus.BAD_REQUEST);
        user.setPassword(password);
        this.userService.updatePassword(user);
        return new ResponseEntity("password updated successfully", HttpStatus.OK);

    }


    @PutMapping(value = {"/logout"})
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('JYM') ")
    public ResponseEntity<?> logoutUser() {
        User currentUser = userService.findByUserName(getCurrentUser().getUsername());
        currentUser.setDevice(null);
        this.userService.save(currentUser);
        if (currentUser.getDevice() != null) {
            this.userDeviceService.Delete(currentUser.getDevice());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/auditor-profile/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAuditoProfile(@PathVariable Long userId,@RequestBody UpdateCoachProfile request) {
        if (this.userService.existByEmail(request.getEmail())) {
            return new ResponseEntity<>("email already exist", HttpStatus.NOT_ACCEPTABLE);
        }
        if(!userService.existById(userId)){
            return new ResponseEntity<>("User not found with id :" + userId, HttpStatus.NOT_FOUND);
        }
        User createdAuditor = this.userService.UpdateAuditorProfile(userId, request);
        return new ResponseEntity<>(createdAuditor, HttpStatus.OK);
    }

    @PatchMapping("/user-profile/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId,@RequestBody UpdateUserProfile request) {
        if (this.userService.existByEmail(request.getEmail())) {
            return new ResponseEntity<>("email already exist", HttpStatus.NOT_ACCEPTABLE);
        }
        if (this.userService.existByPhoneNumber(request.getPhoneNumber())) {
            return new ResponseEntity<>("phone number already exist", HttpStatus.NOT_ACCEPTABLE);
        }
        if(!userService.existById(userId)){
            return new ResponseEntity<>("User not found with id :" + userId, HttpStatus.NOT_FOUND);
        }
        User createdAuditor = this.userService.UpdateUserProfile(userId, request);
        return new ResponseEntity<>(createdAuditor, HttpStatus.OK);
    }

}
