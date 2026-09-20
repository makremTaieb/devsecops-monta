package com.example.pipelineservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Centralized authorization logic for role-based access control.
 *
 * ┌──────────┬──────────────────────────────────────────────────────────────┐
 * │ Role     │ What they can access                                         │
 * ├──────────┼──────────────────────────────────────────────────────────────┤
 * │ ADMIN    │ Full access: all projects, all pipelines, all executions     │
 * │ DEVOPS   │ Full access by role: all projects/pipelines, can deploy      │
 * │ DEV      │ Own projects only (createdBy == username)                    │
 * │ AUDITOR  │ Read-only on own projects (enforced at controller level)     │
 * └──────────┴──────────────────────────────────────────────────────────────┘
 */
@Component
public class AuthorizationHelper {

    /** Current authenticated username. */
    public String currentUsername() {
        return getAuth().getName();
    }

    /** Current Authentication object. */
    public Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * ADMIN → bypasses all ownership checks.
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * DEVOPS → full access by role, no ownership check needed.
     */
    public boolean isDevOps() {
        return hasRole("ROLE_DEVOPS");
    }

    /**
     * DEV → access only to projects they created.
     */
    public boolean isDev() {
        return hasRole("ROLE_DEV");
    }

    /**
     * AUDITOR → read-only; treated like DEV for ownership but
     * blocked at controller level for write operations.
     */
    public boolean isAuditor() {
        return hasRole("ROLE_AUDITOR");
    }

    /**
     * Returns true if the current user can access ANY project
     * without an ownership check.
     *
     * ADMIN and DEVOPS both qualify.
     */
    public boolean hasGlobalAccess() {
        return isAdmin() || isDevOps();
    }

    /**
     * Core ownership gate.
     *
     * Logic:
     *   - ADMIN   → always allowed
     *   - DEVOPS  → always allowed (access by role, not by ownership)
     *   - DEV     → allowed only if they own the project (createdBy == username)
     *   - AUDITOR → allowed only if they own the project (read-only enforced at controller)
     *
     * @param projectCreatedBy  the `createdBy` field of the Project entity
     * @return true if the current user is authorized to interact with this project
     */
    public boolean canAccessProject(String projectCreatedBy) {
        if (hasGlobalAccess()) {
            return true;
        }
        // DEV and AUDITOR: must own the project
        return currentUsername().equals(projectCreatedBy);
    }

    // ── private helpers ───────────────────────────────────────

    private boolean hasRole(String role) {
        return getAuth().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}