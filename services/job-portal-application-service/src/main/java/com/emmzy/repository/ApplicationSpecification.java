package com.emmzy.repository;

import com.emmzy.domain.AiShortListStatus;
import com.emmzy.domain.ApplicationStatus;
import com.emmzy.model.Application;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ApplicationSpecification {

    public static Specification<Application> forCompanyWithFilters(
            Long companyId,
            Long jobId,
            ApplicationStatus status,
            Boolean isStarred,
            AiShortListStatus aiShortListStatus,
            Integer minAiScore
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Always filter by company
            predicates.add(
                    cb.equal(root.get("companyId"), companyId)
            );

            // Filter by job if provided
            if (jobId != null) {
                predicates.add(
                        cb.equal(root.get("jobId"), jobId)
                );
            }

            // Filter by application status if provided
            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            // Filter by starred status if provided
            if (isStarred != null) {
                predicates.add(
                        cb.equal(root.get("isStarred"), isStarred)
                );
            }

            // Filter by AI shortlist status if provided
            if (aiShortListStatus != null) {
                predicates.add(
                        cb.equal(
                                root.get("aiShortListStatus"),
                                aiShortListStatus
                        )
                );
            }

            // Filter by minimum AI score if provided
            if (minAiScore != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("aiScore"),
                                minAiScore
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}

