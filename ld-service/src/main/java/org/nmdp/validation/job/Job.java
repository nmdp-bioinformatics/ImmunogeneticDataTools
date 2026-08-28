package org.nmdp.validation.job;

import java.util.List;
import java.util.UUID;

import org.dash.valid.Sample;

/**
 * State for one background {@code POST /genotypes/file} run, polled via
 * {@code GET /genotypes/jobs/{jobId}}. Fields are volatile rather than fully synchronized: there
 * is exactly one writer (the {@link JobRegistry}'s single background worker thread) and any
 * number of readers (HTTP threads polling status), and none of the reads here are
 * compound/read-modify-write, so volatile's happens-before guarantee is sufficient.
 */
public class Job {

    public enum Phase {
        QUEUED, LOADING_REFERENCE_DATA, ANALYZING_GENOTYPES, DONE, FAILED
    }

    private final String id = UUID.randomUUID().toString();
    private final long createdAtMillis = System.currentTimeMillis();

    private volatile Phase phase = Phase.QUEUED;
    private volatile int processed;
    private volatile int total;
    private volatile List<Sample> result;
    private volatile String error;

    public String getId() {
        return id;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int getProcessed() {
        return processed;
    }

    public int getTotal() {
        return total;
    }

    public void updateProgress(int processed, int total) {
        this.processed = processed;
        this.total = total;
    }

    public List<Sample> getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public boolean isTerminal() {
        return phase == Phase.DONE || phase == Phase.FAILED;
    }

    void complete(List<Sample> result) {
        this.result = result;
        this.phase = Phase.DONE;
    }

    void fail(Throwable cause) {
        this.error = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        this.phase = Phase.FAILED;
    }
}
