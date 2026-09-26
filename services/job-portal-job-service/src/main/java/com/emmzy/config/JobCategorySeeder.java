package com.emmzy.config;

import com.emmzy.model.JobCategory;
import com.emmzy.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobCategorySeeder implements CommandLineRunner {

    private final JobCategoryRepository jobCategoryRepository;

    @Override
    public void run(String... args) {

        seedCategory("Technology", "technology",
                "Software development, engineering, IT and technology roles.");

        seedCategory("Design", "design",
                "UI/UX, graphic design, product design and creative roles.");

        seedCategory("Marketing", "marketing",
                "Digital marketing, advertising, branding and communications roles.");

        seedCategory("Data Science", "data-science",
                "Data analysis, data science, machine learning and AI roles.");
    }

    private void seedCategory(
            String name,
            String slug,
            String description
    ) {

        if (jobCategoryRepository.existsByName(name)) {
            return;
        }

        JobCategory category = JobCategory.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .active(true)
                .build();

        jobCategoryRepository.save(category);

        System.out.println(
                "[Job Category Seeder] Created category: " + name
        );
    }
}