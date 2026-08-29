(function () {
  "use strict";

  const pasteRadio = document.querySelector('input[name="inputMode"][value="paste"]');
  const fileRadio = document.querySelector('input[name="inputMode"][value="file"]');
  const pasteDiv = document.getElementById("paste-input");
  const fileDiv = document.getElementById("file-input");
  const pasteText = document.getElementById("pasteText");
  const fileInput = document.getElementById("fileInput");
  const hladbVersion = document.getElementById("hladbVersion");
  const frequencySet = document.getElementById("frequencySet");
  const useCustomFrequencies = document.getElementById("useCustomFrequencies");
  const customFrequenciesDiv = document.getElementById("custom-frequencies");
  const frequencyFilesInput = document.getElementById("frequencyFiles");
  const allelesFileInput = document.getElementById("allelesFile");
  const submitBtn = document.getElementById("submitBtn");
  const progressSection = document.getElementById("progress-section");
  const progressPhase = document.getElementById("progress-phase");
  const progressBar = document.getElementById("progress-bar");
  const progressCount = document.getElementById("progress-count");
  const errorSection = document.getElementById("error-section");
  const errorMessage = document.getElementById("error-message");
  const resultsSection = document.getElementById("results-section");
  const resultsList = document.getElementById("results-list");
  const downloadBtn = document.getElementById("downloadBtn");

  // Populated once a job reaches DONE; used only by the download button.
  let lastResult = null;

  function toggleInputMode() {
    const isPaste = pasteRadio.checked;
    pasteDiv.hidden = !isPaste;
    fileDiv.hidden = isPaste;
  }
  pasteRadio.addEventListener("change", toggleInputMode);
  fileRadio.addEventListener("change", toggleInputMode);

  useCustomFrequencies.addEventListener("change", function () {
    customFrequenciesDiv.hidden = !useCustomFrequencies.checked;
  });

  function resetOutput() {
    errorSection.hidden = true;
    resultsSection.hidden = true;
    progressSection.hidden = true;
    resultsList.innerHTML = "";
    lastResult = null;
  }

  function showError(message) {
    resetOutput();
    errorSection.hidden = false;
    errorMessage.textContent = message;
  }

  submitBtn.addEventListener("click", function () {
    submitJob().catch(function (e) {
      showError("Request failed: " + e.message);
    });
  });

  // Deliberately routes every submission -- even a single pasted genotype -- through the same
  // async POST /genotypes/file + job-polling path, by wrapping pasted text in a Blob and
  // uploading it exactly like a real file. The alternative (a separate, synchronous code path
  // for "just a paste") would let anyone paste a few hundred lines into the textarea and
  // silently reintroduce the exact blocking-request problem the backend's async redesign
  // exists to prevent -- there would be nothing in the UI to stop it. One path that's always
  // progress-tracked can't regress that, at the minor cost of a poll cycle even for a trivial
  // single-genotype submission.
  async function submitJob() {
    resetOutput();

    let fileBlob;
    let fileName;
    if (pasteRadio.checked) {
      const text = pasteText.value.trim();
      if (!text) {
        showError("Paste at least one GL string first.");
        return;
      }
      fileBlob = new Blob([text + "\n"], { type: "text/plain" });
      fileName = "pasted.txt";
    } else {
      if (!fileInput.files.length) {
        showError("Choose a file to upload first.");
        return;
      }
      fileBlob = fileInput.files[0];
      fileName = fileInput.files[0].name;
    }

    const formData = new FormData();
    formData.append("file", fileBlob, fileName);
    if (hladbVersion.value.trim()) {
      formData.append("hladbVersion", hladbVersion.value.trim());
    }

    // frequencyFiles takes precedence over frequencySet server-side too (see
    // GenotypesApiController#configureWithOptionalCustomFrequencies) -- only sending one or the
    // other here just avoids an unused field cluttering the request.
    if (useCustomFrequencies.checked && frequencyFilesInput.files.length) {
      for (const f of frequencyFilesInput.files) {
        formData.append("frequencyFiles", f, f.name);
      }
      if (allelesFileInput.files.length) {
        formData.append("allelesFile", allelesFileInput.files[0], allelesFileInput.files[0].name);
      }
    } else if (frequencySet.value) {
      formData.append("frequencySet", frequencySet.value);
    }

    submitBtn.disabled = true;
    try {
      const response = await fetch("/genotypes/file", { method: "POST", body: formData });
      if (!response.ok) {
        const body = await safeReadText(response);
        showError("Request rejected (HTTP " + response.status + "): " + (body || response.statusText));
        return;
      }
      const submitted = await response.json();
      progressSection.hidden = false;
      await pollJob(submitted.jobId);
    } finally {
      submitBtn.disabled = false;
    }
  }

  async function safeReadText(response) {
    try {
      return await response.text();
    } catch (e) {
      return "";
    }
  }

  async function pollJob(jobId) {
    for (;;) {
      const response = await fetch("/genotypes/jobs/" + encodeURIComponent(jobId));
      if (!response.ok) {
        showError("Couldn't check job status (HTTP " + response.status + ")");
        return;
      }
      const status = await response.json();
      updateProgress(status);

      if (status.phase === "DONE") {
        showResults(status.result);
        return;
      }
      if (status.phase === "FAILED") {
        showError(status.error || "Job failed with no error message.");
        return;
      }

      await sleep(750);
    }
  }

  function sleep(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  // LOADING_REFERENCE_DATA has no processed/total/percent (the parsing loop itself has no
  // progress signal -- see JobStatus's phase description in the spec), so that phase shows an
  // indeterminate <progress> bar (no value attribute) rather than a fake/frozen percentage.
  function updateProgress(status) {
    if (status.phase === "QUEUED") {
      progressPhase.textContent = "Queued…";
      progressBar.removeAttribute("value");
      progressCount.textContent = "";
    } else if (status.phase === "LOADING_REFERENCE_DATA") {
      progressPhase.textContent = "Loading reference data… (large custom frequency files can take several minutes)";
      progressBar.removeAttribute("value");
      progressCount.textContent = "";
    } else if (status.phase === "ANALYZING_GENOTYPES") {
      progressPhase.textContent = "Analyzing genotypes…";
      progressBar.value = status.percent || 0;
      progressCount.textContent = (status.processed || 0) + " of " + (status.total || 0);
    }
  }

  function showResults(samples) {
    progressSection.hidden = true;
    resultsSection.hidden = false;
    lastResult = samples;

    const list = (samples && samples.sample) || [];
    if (list.length === 0) {
      resultsList.innerHTML = "<p>No samples in the result.</p>";
      return;
    }

    resultsList.innerHTML = list.map(renderSample).join("");
  }

  function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value === null || value === undefined ? "" : String(value);
    return div.innerHTML;
  }

  function renderSample(sample) {
    const anomalyBadge = sample.hasAnomalies
      ? '<span class="badge badge-warning">Anomaly</span>'
      : '<span class="badge badge-ok">Clean</span>';

    const warningsHtml = (sample.warnings || [])
      .map(function (w) {
        return "<li>" + escapeHtml(w) + "</li>";
      })
      .join("");

    const pairsHtml = (sample.haplotypePair || []).map(renderHaplotypePair).join("");

    const reports = [
      ["Linkage report", sample.linkageReport],
      ["Haplotype pair report", sample.haplotypePairReport],
      ["Non-CWD/CIWD report", sample.nonCommonWellDocumentedReport],
      ["Detected findings (CSV)", sample.detectedFindingsReport],
    ]
      .filter(function (entry) {
        return entry[1] && entry[1].trim();
      })
      .map(function (entry) {
        return "<details><summary>" + entry[0] + "</summary><pre>" + escapeHtml(entry[1]) + "</pre></details>";
      })
      .join("");

    return (
      '<article class="sample">' +
      "<header><h3>" +
      escapeHtml(sample.id) +
      "</h3>" +
      anomalyBadge +
      "</header>" +
      '<p class="gl-string">' +
      escapeHtml(sample.glString) +
      "</p>" +
      (warningsHtml ? '<ul class="warnings">' + warningsHtml + "</ul>" : "") +
      (pairsHtml || '<p class="no-pairs">No haplotype pairs detected.</p>') +
      reports +
      "</article>"
    );
  }

  function renderHaplotypePair(pair) {
    const findingsRows = (pair.finding || [])
      .map(function (f) {
        return (
          "<tr><td>" +
          escapeHtml(f.race || "—") +
          "</td><td>" +
          escapeHtml(f.frequency) +
          "</td><td>" +
          escapeHtml(f.relativeFrequency) +
          "</td><td>" +
          escapeHtml(f.haplotype1Frequency) +
          "</td><td>" +
          escapeHtml(f.haplotype2Frequency) +
          "</td></tr>"
        );
      })
      .join("");

    const table = findingsRows
      ? '<table class="findings"><thead><tr><th>Race</th><th>Frequency</th><th>Relative</th><th>Hap 1</th><th>Hap 2</th></tr></thead><tbody>' +
        findingsRows +
        "</tbody></table>"
      : "";

    return (
      '<div class="haplotype-pair"><p><strong>' +
      escapeHtml(pair.haplotype1) +
      "</strong> &times; <strong>" +
      escapeHtml(pair.haplotype2) +
      "</strong></p>" +
      table +
      "</div>"
    );
  }

  downloadBtn.addEventListener("click", function () {
    if (!lastResult) {
      return;
    }

    const list = lastResult.sample || [];
    const text = list
      .map(function (sample) {
        return (
          "=== " +
          sample.id +
          " ===\n\n" +
          [sample.linkageReport, sample.haplotypePairReport, sample.nonCommonWellDocumentedReport, sample.detectedFindingsReport]
            .filter(Boolean)
            .join("\n\n")
        );
      })
      .join("\n\n");

    const blob = new Blob([text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "hlahapv-reports.txt";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  });
})();
