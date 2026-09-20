// ============================================================
// EXCEPTION HANDLING
// ============================================================

// ---------- ResourceNotFoundException.java ----------
package com.example.authservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
 