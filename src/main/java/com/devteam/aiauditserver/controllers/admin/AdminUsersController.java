package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.enums.User.Gender;
import com.devteam.aiauditserver.enums.User.RoleEnum;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.requests.project.AddUserRequest;
import com.devteam.aiauditserver.requests.project.UpdateCoachProfile;
import com.devteam.aiauditserver.responses.Response.DynamicResponse;
import com.devteam.aiauditserver.services.auth.UserService;
import com.devteam.aiauditserver.services.project.MailsSenderService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@CrossOrigin
@RequestMapping(value = "/api/v1/admin-users")
public class AdminUsersController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private MailsSenderService mailsSenderService;


    @PostMapping("/auditor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addAuditor(@RequestBody AddUserRequest request) {
        if (request.getEmail() == null) {
            return new ResponseEntity<>("email is required ", HttpStatus.NOT_ACCEPTABLE);
        }
        if (this.userService.existByEmail(request.getEmail())) {
            return new ResponseEntity<>("email already exist", HttpStatus.NOT_ACCEPTABLE);
        }
        User createdAuditor = this.userService.addNewAuditor(request);
        return new ResponseEntity<>(createdAuditor, HttpStatus.OK);
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

    @GetMapping(value = {"/password_remainder"})
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('JYM') ")
    public ResponseEntity<?> password_remainder(@RequestParam String email) throws IOException {
        String username = getCurrentUser().getUsername();
        try {
            User user = userService.findbyemail(email);
            if (user == null) {
                return new ResponseEntity<>("User not find with that email", HttpStatus.NOT_FOUND);
            }
            String generatedPassword = UserService.generateRandomPassword(8);
            this.mailsSenderService.resendPassword("New password generated: Ai Audit System Platform", user.getEmail(), user, generatedPassword);
            return new ResponseEntity<>(user, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PatchMapping(value = {"/activated/{id}"})
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('JYM') ")
    @ApiOperation(value = "update user activation to the opposit", notes = "Endpoint to update user's activate attribute")
    @ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Successfully add"),
            @io.swagger.annotations.ApiResponse(code = 400, message = "Bad request, check the id supplier "),
            @io.swagger.annotations.ApiResponse(code = 403, message = "Forbidden, you are not an admin"),
    })
    public ResponseEntity<User> updateUserActivation(@PathVariable Long id) {
        User user = this.userService.updateActivatedUser(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/all_users_pg")
    @PreAuthorize("hasRole('FEDERATION') or hasRole('COACH') or hasRole('JYM') ")
    public ResponseEntity<DynamicResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) RoleEnum role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) Long id
    ) {
        Page<User> pageResult = this.userService.findUsersByCriteria(
                page,
                size,
                id,
                search,
                gender,
                role,
                active
        );
        DynamicResponse result = new DynamicResponse(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
