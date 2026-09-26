package com.emmzy.controller;

import com.emmzy.model.Resume;
import com.emmzy.service.ResumeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Resume> uploadResume(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        Resume resume = resumeService.uploadResume(userId, file);

        return ResponseEntity.ok(resume);
    }

    @GetMapping
    public ResponseEntity<List<Resume>> getUserResumes(
            @RequestHeader("X-User-Id") Long userId
    ) {

        return ResponseEntity.ok(
                resumeService.getUserResumes(userId)
        );
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<Resume> getResume(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long userId
    ) throws Exception {

        return ResponseEntity.ok(
                resumeService.getResume(resumeId, userId)
        );
    }

    @GetMapping("/{resumeId}/download")
    public ResponseEntity<byte[]> downloadResume(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long userId
    ) throws Exception {

        Resume resume = resumeService.getResume(resumeId, userId);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(resume.getFileType())
        );

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(resume.getOriginalFileName())
                        .build()
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(resume.getFileData());
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long userId
    ) throws Exception {

        resumeService.deleteResume(resumeId, userId);

        return ResponseEntity.noContent().build();
    }
}