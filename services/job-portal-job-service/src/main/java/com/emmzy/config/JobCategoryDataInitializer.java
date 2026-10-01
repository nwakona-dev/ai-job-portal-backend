package com.emmzy.config;

import com.emmzy.model.JobCategory;
import com.emmzy.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobCategoryDataInitializer implements CommandLineRunner {

    private final JobCategoryRepository jobCategoryRepository;

    @Override
    public void run(String... args) {

        List<String> categories = List.of(
                "Software Engineering",
                "DevOps Engineering",
                "Data Science",
                "Data Engineering",
                "Artificial Intelligence & Machine Learning",
                "Cybersecurity",
                "Cloud Computing",
                "IT & Networking",
                "UI/UX Design",
                "Product Management",
                "Project Management",
                "Business Analysis",
                "Marketing",
                "Sales",
                "Finance & Accounting",
                "Human Resources",
                "Customer Service",
                "Operations",
                "Legal",
                "Healthcare & Medical",
                "Education & Training",
                "Architecture & Engineering",
                "Construction",
                "Manufacturing",
                "Logistics & Supply Chain",
                "Media & Communications",
                "Hospitality & Tourism",
                "Retail",
                "Administrative & Office",
                "Research"
        );

        for (String categoryName : categories) {

            if (!jobCategoryRepository.existsByName(categoryName)) {

                JobCategory category = JobCategory.builder()
                        .name(categoryName)
                        .slug(generateSlug(categoryName))
                        .description("Jobs related to " + categoryName + ".")
                        .active(true)
                        .build();

                jobCategoryRepository.save(category);

                System.out.println(
                        "Created job category: " + categoryName
                );
            }
        }

        System.out.println("Job category initialization completed.");
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
    }
}