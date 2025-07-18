
package com.swen755.fault_recovery_redundancy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {
    @NotBlank
    public String username;

    @NotBlank(message = "Password is required")
    public String password;

    @NotBlank(message = "Conirm Password is required")
    public String confirmPassword;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    public String email;
}
