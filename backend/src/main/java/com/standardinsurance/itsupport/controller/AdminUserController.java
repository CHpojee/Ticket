package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.dto.UserDtos.CreateUserRequest;
import com.standardinsurance.itsupport.dto.UserDtos.RestrictionRequest;
import com.standardinsurance.itsupport.dto.UserDtos.UpdateUserRequest;
import com.standardinsurance.itsupport.dto.UserDtos.UserDetail;
import com.standardinsurance.itsupport.security.CurrentUser;
import com.standardinsurance.itsupport.service.RestrictionService;
import com.standardinsurance.itsupport.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only user + restriction maintenance. See docs/specs/02-user-maintenance.md. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final RestrictionService restrictionService;
    private final CurrentUser currentUser;

    public AdminUserController(UserService userService, RestrictionService restrictionService,
                               CurrentUser currentUser) {
        this.userService = userService;
        this.restrictionService = restrictionService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<UserDetail> list() {
        return userService.list();
    }

    @GetMapping("/{userId}")
    public UserDetail get(@PathVariable String userId) {
        return userService.get(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDetail create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(currentUser.userId(), request);
    }

    @PutMapping("/{userId}")
    public UserDetail update(@PathVariable String userId,
                             @RequestBody UpdateUserRequest request) {
        return userService.update(currentUser.userId(), userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String userId) {
        userService.delete(currentUser.userId(), userId);
    }

    @GetMapping("/{userId}/restrictions")
    public List<String> restrictions(@PathVariable String userId) {
        return restrictionService.listCategoryCodes(userId);
    }

    @PostMapping("/{userId}/restrictions")
    @ResponseStatus(HttpStatus.CREATED)
    public List<String> addRestriction(@PathVariable String userId,
                                       @Valid @RequestBody RestrictionRequest request) {
        restrictionService.add(userId, request.ticketCategoryCode());
        return restrictionService.listCategoryCodes(userId);
    }

    @DeleteMapping("/{userId}/restrictions/{categoryCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRestriction(@PathVariable String userId,
                                  @PathVariable String categoryCode) {
        restrictionService.remove(userId, categoryCode);
    }
}
