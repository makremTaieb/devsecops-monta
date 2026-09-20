// ============================================================
// LAYER 1 — ENTITIES
// ============================================================

// ---------- Role.java ----------
package com.example.authservice.entities;

// FIX: Devops → DEVOPS, Auditeur → AUDITOR (screaming case convention)
// role.name() in JWT was producing "Devops" instead of "DEVOPS"
public enum Role {
    ADMIN,
    DEV,
    DEVOPS,
    AUDITOR
}
 