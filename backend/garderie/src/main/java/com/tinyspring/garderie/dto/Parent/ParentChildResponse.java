package com.tinyspring.garderie.dto.Parent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentChildResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private Long classroomId;
    private String classroomName;
}
