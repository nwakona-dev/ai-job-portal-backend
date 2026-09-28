package com.emmzy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMeetingLinkRequest {

    @NotBlank(message = "Meeting link is required")
    private String meetingLink;
}