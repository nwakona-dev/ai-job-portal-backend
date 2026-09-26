package com.emmzy.service.impl;

import com.emmzy.model.Resume;
import com.emmzy.repository.ResumeRepository;
import com.emmzy.service.ResumeService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;

    @Override
    public Resume uploadResume(Long userId, MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Resume file name is missing");
        }

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("Unable to determine file type");
        }

        boolean validType =
                contentType.equalsIgnoreCase("application/pdf")
                        || contentType.equalsIgnoreCase(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
                        || contentType.equalsIgnoreCase("application/msword");

        if (!validType) {
            throw new IllegalArgumentException(
                    "Only PDF, DOC, and DOCX files are allowed"
            );
        }

        // 10 MB maximum
        long maxFileSize = 10 * 1024 * 1024;

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "Resume file must not exceed 10 MB"
            );
        }

        Resume resume = Resume.builder()
                .userId(userId)
                .originalFileName(fileName)
                .fileType(contentType)
                .fileSize(file.getSize())
                .fileData(file.getBytes())
                .build();

        return resumeRepository.save(resume);
    }

    @Override
    public List<Resume> getUserResumes(Long userId) {
        return resumeRepository.findByUserId(userId);
    }

    @Override
    public Resume getResume(Long resumeId, Long userId) throws Exception {

        return resumeRepository
                .findByIdAndUserId(resumeId, userId)
                .orElseThrow(() ->
                        new Exception("Resume not found")
                );
    }

    @Override
    public byte[] downloadResume(Long resumeId, Long userId) throws Exception {

        Resume resume = getResume(resumeId, userId);

        return resume.getFileData();
    }

    @Override
    public void deleteResume(Long resumeId, Long userId) throws Exception {

        Resume resume = getResume(resumeId, userId);

        resumeRepository.delete(resume);
    }
}