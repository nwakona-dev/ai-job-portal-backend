package com.emmzy.model;

import com.emmzy.domain.SocialPlatform;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLink {

    private SocialPlatform platform;
    private String url;
}
