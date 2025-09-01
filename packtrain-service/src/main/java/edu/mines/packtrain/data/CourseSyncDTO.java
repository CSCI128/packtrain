package edu.mines.packtrain.data;

import lombok.Value;

import java.util.UUID;

@Value
public class CourseSyncDTO {
    UUID courseId;
    String canvasId;
}
