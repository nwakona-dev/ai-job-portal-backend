package com.emmzy.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTagRequest {

    @NotBlank(message = "Tag nane is required")
    private String name;

}
