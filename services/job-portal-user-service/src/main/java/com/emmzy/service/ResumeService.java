package com.emmzy.service;

import com.emmzy.model.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    Resume uploadResume(Long userId, MultipartFile file) throws Exception;

    List<Resume> getUserResumes(Long userId);

    Resume getResume(Long resumeId, Long userId) throws Exception;

    byte[] downloadResume(Long resumeId, Long userId) throws Exception;

    void deleteResume(Long resumeId, Long userId) throws Exception;
}