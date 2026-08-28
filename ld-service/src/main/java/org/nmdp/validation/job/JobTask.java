package org.nmdp.validation.job;

import java.util.List;

import org.dash.valid.Sample;

/**
 * The actual work behind a {@link Job}, given the job so it can report phase/progress as it
 * runs (see {@link Job.Phase}).
 */
@FunctionalInterface
public interface JobTask {
    List<Sample> run(Job job) throws Exception;
}
