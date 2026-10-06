package com.emmzy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationData {

    private Long id;

    private Long jobId;

    private Long candidateId;

    private String status;
}

