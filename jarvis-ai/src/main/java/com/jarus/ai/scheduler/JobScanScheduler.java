package com.jarus.ai.scheduler;

import com.jarus.ai.model.JobPost;
import com.jarus.ai.model.UserProfile;
import com.jarus.ai.model.UserSettings;
import com.jarus.ai.repository.JobRepository;
import com.jarus.ai.repository.UserRepository;
import com.jarus.ai.service.JobAggregatorService;
import com.jarus.ai.service.PushSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScanScheduler.class);

    @Autowired private UserRepository userRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobAggregatorService aggregatorService;
    @Autowired private PushSubscriptionService pushService;

    // Runs every day at 8:00 AM server time
    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledScan() {
        log.info("Starting scheduled job scan for all users");
        List<UserProfile> users = userRepository.getAllUsers();
        for (UserProfile user : users) {
            try {
                scanForUser(user.getUserId());
            } catch (Exception e) {
                log.error("Job scan failed for user {}: {}", user.getUserId(), e.getMessage());
            }
        }
    }

    public int scanForUser(String userId) {
        UserSettings settings = userRepository.getSettings(userId);
        if (settings.getJobKeywords() == null || settings.getJobKeywords().isEmpty()) {
            return 0;
        }
        List<JobPost> jobs = aggregatorService.fetchJobs(
                settings.getJobKeywords(), settings.getLocation(), userId);
        int saved = 0;
        for (JobPost job : jobs) {
            try {
                jobRepository.save(userId, job);
                saved++;
            } catch (Exception e) {
                log.warn("Failed to save job for user {}: {}", userId, e.getMessage());
            }
        }
        if (saved > 0) {
            String payload = "{\"title\":\"JARUS Job Alert\",\"body\":\"" + saved + " new jobs found for your search!\"}";
            pushService.notifyAll(userId, payload);
        }
        log.info("Saved {} new jobs for user {}", saved, userId);
        return saved;
    }
}
