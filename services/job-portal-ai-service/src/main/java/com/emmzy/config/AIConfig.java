package com.emmzy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AIConfig {

    private Matching matching = new Matching();
    private Recommendations recommendations = new Recommendations();

    public Matching getMatching() {
        return matching;
    }

    public void setMatching(Matching matching) {
        this.matching = matching;
    }

    public Recommendations getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Recommendations recommendations) {
        this.recommendations = recommendations;
    }

    public static class Matching {

        private boolean enabled = true;
        private Weights weights = new Weights();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Weights getWeights() {
            return weights;
        }

        public void setWeights(Weights weights) {
            this.weights = weights;
        }
    }

    public static class Weights {

        private double skills = 0.50;
        private double experience = 0.20;
        private double education = 0.10;
        private double location = 0.10;
        private double jobType = 0.10;

        public double getSkills() {
            return skills;
        }

        public void setSkills(double skills) {
            this.skills = skills;
        }

        public double getExperience() {
            return experience;
        }

        public void setExperience(double experience) {
            this.experience = experience;
        }

        public double getEducation() {
            return education;
        }

        public void setEducation(double education) {
            this.education = education;
        }

        public double getLocation() {
            return location;
        }

        public void setLocation(double location) {
            this.location = location;
        }

        public double getJobType() {
            return jobType;
        }

        public void setJobType(double jobType) {
            this.jobType = jobType;
        }
    }

    public static class Recommendations {

        private int maxResults = 20;
        private double minimumScore = 50.0;

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public double getMinimumScore() {
            return minimumScore;
        }

        public void setMinimumScore(double minimumScore) {
            this.minimumScore = minimumScore;
        }
    }
}