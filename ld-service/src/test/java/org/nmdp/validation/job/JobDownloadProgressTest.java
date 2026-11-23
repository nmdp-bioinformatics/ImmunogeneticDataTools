package org.nmdp.validation.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class JobDownloadProgressTest {

    @Test
    public void reportsTheFileWhileDownloadingAndNothingOnceItFinishes() {
        Job job = new Job();
        assertNull(job.getDownloadFile());

        job.updateDownload("A~B.xlsx", 0, 100);
        assertEquals("A~B.xlsx", job.getDownloadFile());

        job.updateDownload("A~B.xlsx", 40, 100);
        assertEquals(40, job.getDownloadedBytes());
        assertEquals(100, job.getDownloadTotalBytes());

        job.updateDownload("A~B.xlsx", 100, 100);
        assertNull(job.getDownloadFile(), "a finished download is no longer 'downloading'");
    }

    @Test
    public void clearDownloadRemovesAStaleFile() {
        Job job = new Job();
        job.updateDownload("A~B.xlsx", 10, 100);
        job.clearDownload();
        assertNull(job.getDownloadFile());
    }
}
