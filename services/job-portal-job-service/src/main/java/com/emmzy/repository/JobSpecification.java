package com.emmzy.repository;

import com.emmzy.domain.JobStatus;
import com.emmzy.model.Job;
import com.emmzy.payload.JobSearchRequest;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    private JobSpecification() {
    }

    public static Specification<Job> build(
            JobSearchRequest req
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            /*
             * PUBLIC JOB-SEEKER QUERY
             *
             * The /api/jobs endpoint should only expose jobs that
             * employers have made available to job seekers.
             *
             * Do NOT rely on JobSearchRequest.status here.
             * Spring creates JobSearchRequest through @ModelAttribute,
             * and a default value inside that DTO could accidentally
             * filter out OPEN jobs.
             */
            predicates.add(
                    cb.isTrue(
                            root.get("active")
                    )
            );

            predicates.add(
                    cb.equal(
                            root.get("status"),
                            JobStatus.OPEN
                    )
            );

            /*
             * Keyword search
             */
            if (req != null &&
                    req.getKeyword() != null &&
                    !req.getKeyword().isBlank()) {

                String pattern =
                        "%" +
                                req.getKeyword()
                                        .trim()
                                        .toLowerCase() +
                                "%";

                Predicate titleMatch =
                        cb.like(
                                cb.lower(
                                        root.get("title")
                                ),
                                pattern
                        );

                Predicate descriptionMatch =
                        cb.like(
                                cb.lower(
                                        root.get("description")
                                ),
                                pattern
                        );

                Predicate requirementsMatch =
                        cb.like(
                                cb.lower(
                                        root.get("requirements")
                                ),
                                pattern
                        );

                predicates.add(
                        cb.or(
                                titleMatch,
                                descriptionMatch,
                                requirementsMatch
                        )
                );
            }

            /*
             * Job type
             */
            if (req != null &&
                    req.getJobType() != null) {

                predicates.add(
                        cb.equal(
                                root.get("jobType"),
                                req.getJobType()
                        )
                );
            }

            /*
             * Work mode
             */
            if (req != null &&
                    req.getWorkMode() != null) {

                predicates.add(
                        cb.equal(
                                root.get("workMode"),
                                req.getWorkMode()
                        )
                );
            }

            /*
             * Experience level
             */
            if (req != null &&
                    req.getExperienceLevel() != null) {

                predicates.add(
                        cb.equal(
                                root.get("experienceLevel"),
                                req.getExperienceLevel()
                        )
                );
            }

            /*
             * Company
             */
            if (req != null &&
                    req.getCompanyId() != null) {

                predicates.add(
                        cb.equal(
                                root.get("companyId"),
                                req.getCompanyId()
                        )
                );
            }

            /*
             * Category
             */
            if (req != null &&
                    req.getCategoryId() != null) {

                predicates.add(
                        cb.equal(
                                root
                                        .get("category")
                                        .get("id"),
                                req.getCategoryId()
                        )
                );
            }

            /*
             * Location
             */
            if (req != null &&
                    req.getLocation() != null &&
                    !req.getLocation().isBlank()) {

                String pattern =
                        "%" +
                                req.getLocation()
                                        .trim()
                                        .toLowerCase() +
                                "%";

                Path<String> city =
                        root
                                .get("location")
                                .get("city");

                Path<String> state =
                        root
                                .get("location")
                                .get("state");

                Path<String> country =
                        root
                                .get("location")
                                .get("country");

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(city),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(state),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(country),
                                        pattern
                                )
                        )
                );
            }

            /*
             * Minimum salary
             */
            if (req != null &&
                    req.getMinSalary() != null) {

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root
                                        .get("salaryRange")
                                        .get("minSalary"),
                                req.getMinSalary()
                        )
                );
            }

            /*
             * Maximum salary
             */
            if (req != null &&
                    req.getMaxSalary() != null) {

                predicates.add(
                        cb.lessThanOrEqualTo(
                                root
                                        .get("salaryRange")
                                        .get("maxSalary"),
                                req.getMaxSalary()
                        )
                );
            }

            /*
             * Minimum openings
             */
            if (req != null &&
                    req.getMinOpenings() != null) {

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("openings"),
                                req.getMinOpenings()
                        )
                );
            }

            /*
             * Maximum openings
             */
            if (req != null &&
                    req.getMaxOpenings() != null) {

                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("openings"),
                                req.getMaxOpenings()
                        )
                );
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}