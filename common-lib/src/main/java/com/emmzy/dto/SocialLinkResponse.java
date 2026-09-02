package com.emmzy.dto;

import com.emmzy.domain.SocialPlatform;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLinkResponse {

    private SocialPlatform platform;
    private String url;
}
