package com.emmzy.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
@Data
@Getter
@Setter
public class ServiceUrlConfig {

    private Service user = new Service();
    private Service company = new Service();
    private Service job = new Service();
    private Service resume = new Service();
    private Service application = new Service();

    public Service getUser() {
        return user;
    }

    public void setUser(Service user) {
        this.user = user;
    }

    public Service getCompany() {
        return company;
    }

    public void setCompany(Service company) {
        this.company = company;
    }

    public Service getJob() {
        return job;
    }

    public void setJob(Service job) {
        this.job = job;
    }

    public Service getResume() {
        return resume;
    }

    public void setResume(Service resume) {
        this.resume = resume;
    }

    public Service getApplication() {
        return application;
    }

    public void setApplication(Service application) {
        this.application = application;
    }

    public static class Service {

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}