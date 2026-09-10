package com.emmzy.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTagResponse {

    private Long id;
    private String name;
    private String slug;
}
