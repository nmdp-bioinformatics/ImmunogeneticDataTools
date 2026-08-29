package org.nmdp.validation.job;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Runs {@link JobTask}s one at a time on a single background thread and tracks their
 * {@link Job} state for polling. Phase 8: {@code POST /genotypes/file} can take minutes against
 * real (large, user-uploaded) frequency reference data -- long past any reasonable HTTP
 * timeout, and with zero progress feedback if run synchronously on the request thread.
 * <p>
 * Deliberately minimal for what's scoped as a personal/local tool, not a general job-processing
 * service: in-memory only (a restart loses in-flight/completed jobs -- acceptable here), a
 * single worker thread (matches the CLI's own single-process model, and preserves the
 * one-analysis-at-a-time invariant {@code GenotypesApiController}'s hladb/frequency-set
 * configuration already depends on), and best-effort time-based eviction on each new submission
 * rather than a dedicated sweep thread.
 */
@Component
public class JobRegistry {

    private static final Logger LOGGER = Logger.getLogger(JobRegistry.class.getName());
    private static final Duration RETENTION = Duration.ofHours(1);

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "genotypes-job-worker");
        thread.setDaemon(true);
        return thread;
    });

    public Job submit(JobTask task) {
        evictOldTerminalJobs();

        Job job = new Job();
        jobs.put(job.getId(), job);

        executor.submit(() -> {
            try {
                job.complete(task.run(job));
            }
            catch (Exception e) {
                LOGGER.log(Level.WARNING, "Job " + job.getId() + " failed", e);
                job.fail(e);
            }
        });

        return job;
    }

    public Job get(String jobId) {
        return jobs.get(jobId);
    }

    private void evictOldTerminalJobs() {
        long cutoff = System.currentTimeMillis() - RETENTION.toMillis();
        jobs.values().removeIf(job -> job.isTerminal() && job.getCreatedAtMillis() < cutoff);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
