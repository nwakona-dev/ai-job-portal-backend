package com.emmzy.config;

import com.emmzy.model.JobTag;
import com.emmzy.repository.JobTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobTagDataInitializer implements CommandLineRunner {

    private final JobTagRepository jobTagRepository;

    @Override
    public void run(String... args) {

        List<String> tags = List.of(
                // Programming Languages
                "Java",
                "JavaScript",
                "TypeScript",
                "Python",
                "C-Sharp",
                "C-Plus-Plus",
                "PHP",

                // Backend
                "Spring Boot",
                "Spring Security",
                "Hibernate",
                "JPA",
                "Node.js",
                "Express.js",
                "Django",
                "FastAPI",
                ".NET",
                "Laravel",
                "REST API",
                "GraphQL",
                "Microservices",

                // Frontend
                "React",
                "React Native",
                "Angular",
                "Vue.js",
                "Next.js",
                "HTML",
                "CSS",
                "Tailwind CSS",

                // Databases
                "MySQL",
                "PostgreSQL",
                "MongoDB",
                "Redis",
                "SQL",
                "Database Design",

                // Cloud & DevOps
                "AWS",
                "Microsoft Azure",
                "Google Cloud",
                "Docker",
                "Kubernetes",
                "CI/CD",
                "Linux",

                // Version Control
                "Git",
                "GitHub",
                "GitLab",

                // AI & Data
                "Artificial Intelligence",
                "Machine Learning",
                "Deep Learning",
                "TensorFlow",
                "PyTorch",
                "Data Analysis",
                "Data Visualization",

                // Cybersecurity
                "Cybersecurity",
                "Network Security",
                "Penetration Testing",

                // Design
                "UI Design",
                "UX Design",
                "Figma",
                "Wireframing",

                // Mobile
                "Mobile Development",
                "Android Development",
                "iOS Development",

                // Software Engineering
                "System Design",
                "Software Architecture",
                "Agile",
                "Scrum",
                "Problem Solving"
        );

        for (String tagName : tags) {

            if (!jobTagRepository.existsByName(tagName)) {

                JobTag tag = JobTag.builder()
                        .name(tagName)
                        .slug(generateSlug(tagName))
                        .build();

                jobTagRepository.save(tag);

                System.out.println(
                        "Created job tag: " + tagName
                );
            }
        }

        System.out.println("Job tag initialization completed.");
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
    }
}