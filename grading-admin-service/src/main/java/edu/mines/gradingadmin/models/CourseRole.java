package edu.mines.gradingadmin.models;

import lombok.Getter;

import java.util.stream.Stream;

public enum CourseRole {
    STUDENT("student"),
    TA("ta"),
    INSTRUCTOR("instructor"),
    OWNER("owner");

    @Getter
    private final String role;

    CourseRole(String role){
        this.role = role;
    }

    public static CourseRole fromString(String type){
        return Stream.of(CourseRole.values()).filter(r -> r.role.equals(type)).findFirst().orElseThrow(RuntimeException::new);
    }
}
