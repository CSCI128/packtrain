package edu.mines.gradingadmin.data.gradescope;

import lombok.Data;

@Data
public class LoginDTO {
    private String email;
    private String password;
    private String authenticityToken;
    private final int rememberMe = 0;
    private final String commit = "Log In";
}
