package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.dto.UserDtos.CreateUserRequest;
import com.standardinsurance.itsupport.dto.UserDtos.UpdateUserRequest;
import com.standardinsurance.itsupport.dto.UserDtos.UserDetail;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.ConflictException;
import com.standardinsurance.itsupport.exception.NotFoundException;
import com.standardinsurance.itsupport.repository.TicketRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.repository.UserRestrictionRepository;
import com.standardinsurance.itsupport.security.Roles;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin user maintenance. See docs/specs/02-user-maintenance.md. */
@Service
public class UserService {

    private static final String ACTION_CREATED = "USER_CREATED";
    private static final String ACTION_UPDATED = "USER_UPDATED";
    private static final String ACTION_DELETED = "USER_DELETED";

    private final UserRepository userRepository;
    private final UserRestrictionRepository restrictionRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       UserRestrictionRepository restrictionRepository,
                       TicketRepository ticketRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.restrictionRepository = restrictionRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserDetail> list() {
        return userRepository.findAll().stream().map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public UserDetail get(String userId) {
        return toDetail(require(userId));
    }

    @Transactional
    public UserDetail create(String actorId, CreateUserRequest req) {
        if (userRepository.existsByUserId(req.userId())) {
            throw new ConflictException("User " + req.userId() + " already exists");
        }
        User user = new User(req.userId(), passwordEncoder.encode(req.password()), req.name(),
                req.approver() ? User.APPROVER_FLAG : null, blankToNull(req.emailAddress()));
        userRepository.save(user);
        auditService.record(actorId, null, ACTION_CREATED, "userId", null, req.userId());
        return toDetail(user);
    }

    @Transactional
    public UserDetail update(String actorId, String userId, UpdateUserRequest req) {
        User user = require(userId);
        if (req.name() != null && !req.name().isBlank() && !req.name().equals(user.getName())) {
            String old = user.getName();
            user.setName(req.name());
            auditService.record(actorId, null, ACTION_UPDATED, "name", old, req.name());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
            auditService.record(actorId, null, ACTION_UPDATED, "password", "***", "***");
        }
        if (req.approver() != null) {
            String old = user.getApprover();
            String next = req.approver() ? User.APPROVER_FLAG : null;
            if (!java.util.Objects.equals(old, next)) {
                user.setApprover(next);
                auditService.record(actorId, null, ACTION_UPDATED, "approver", old, next);
            }
        }
        if (req.emailAddress() != null) {
            String next = blankToNull(req.emailAddress());
            if (!java.util.Objects.equals(user.getEmailAddress(), next)) {
                String old = user.getEmailAddress();
                user.setEmailAddress(next);
                auditService.record(actorId, null, ACTION_UPDATED, "emailAddress", old, next);
            }
        }
        userRepository.save(user);
        return toDetail(user);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @Transactional
    public void delete(String actorId, String userId) {
        User user = require(userId);
        if (ticketRepository.existsByRequestor_UserId(userId)) {
            throw new ConflictException(
                    "Cannot delete user " + userId + ": they own one or more tickets");
        }
        userRepository.delete(user);
        auditService.record(actorId, null, ACTION_DELETED, "userId", userId, null);
    }

    private User require(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Unknown user " + userId));
    }

    private UserDetail toDetail(User user) {
        List<String> restrictions = restrictionRepository.findByUser_UserId(user.getUserId())
                .stream().map(r -> r.getCategory().getCode()).toList();
        return new UserDetail(user.getUserId(), user.getName(),
                Roles.forUserId(user.getUserId()), user.isApprover(),
                user.getEmailAddress(), restrictions);
    }
}
