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
  // Whether the in-flight job asked for nmdp-2026 (the controls may change while it runs).
  let lastSubmittedNmdp2026 = false;

  function toggleInputMode() {
    const isPaste = pasteRadio.checked;
    pasteDiv.hidden = !isPaste;
    fileDiv.hidden = isPaste;
  }
  pasteRadio.addEventListener("change", toggleInputMode);
  fileRadio.addEventListener("change", toggleInputMode);

  const nmdp2026Terms = document.getElementById("nmdp2026-terms");
  const acceptNmdp2026Terms = document.getElementById("acceptNmdp2026Terms");

  // The notice shows only when it can matter: nmdp-2026 chosen and not overridden by uploaded
  // files. The server enforces the acknowledgement itself; this is the page's half.
  function usesNmdp2026() {
    return frequencySet.value === "nmdp-2026" && !(useCustomFrequencies.checked && frequencyFilesInput.files.length);
  }
  function toggleNmdp2026Terms() {
    nmdp2026Terms.hidden = !usesNmdp2026();
  }
  frequencySet.addEventListener("change", toggleNmdp2026Terms);
  frequencyFilesInput.addEventListener("change", toggleNmdp2026Terms);

  useCustomFrequencies.addEventListener("change", function () {
    toggleNmdp2026Terms();
    customFrequenciesDiv.hidden = !useCustomFrequencies.checked;
  });

  function resetOutput() {
    errorSection.hidden = true;
    resultsSection.hidden = true;
    progressSection.hidden = true;
    resultsList.innerHTML = "";
    lastResult = null;
  }

  // The IMGT/HLA releases the service can actually use (GET /hladb-versions), newest first. null
  // means the list couldn't be fetched (offline, endpoint unavailable): the field is then not
  // checked here, and the server still rejects a bad version with a clear message.
  let knownVersions = null;
  let lastBlockedVersion = null;
  const hladbHint = document.getElementById("hladbVersionHint");
  const hladbList = document.getElementById("hladbVersionList");

  async function loadHladbVersions() {
    try {
      const response = await fetch("/hladb-versions");
      if (!response.ok) {
        return;
      }
      const body = await response.json();
      const versions = body.versions || [];
      knownVersions = versions.map(function (v) { return v.version; });
      hladbList.innerHTML = "";
      versions.forEach(function (v) {
        const option = document.createElement("option");
        option.value = v.version;
        if (v.released) {
          option.label = v.version + " (" + v.released + ")";
        }
        hladbList.appendChild(option);
      });
      // The user may have typed before the list arrived.
      checkHladbVersion();
    } catch (e) {
      // Leave knownVersions null: no client-side check.
    }
  }

  function clearHladbHint() {
    hladbHint.hidden = true;
    hladbHint.textContent = "";
    hladbVersion.removeAttribute("aria-invalid");
  }

  // True when the field is fine (blank or "latest" mean the latest release, or the list isn't
  // known, or the value is a listed release); otherwise shows what to do and returns false.
  function checkHladbVersion() {
    clearHladbHint();
    const typed = hladbVersion.value.trim();
    if (!typed || typed.toLowerCase() === "latest" || !knownVersions || knownVersions.indexOf(typed) !== -1) {
      return true;
    }

    // Suggest releases that share the start of what was typed ("3.2.0" -> "3.2" -> 3.20.0, 3.21.0...),
    // else the newest ones.
    const prefix = typed.replace(/(\.0)+$/, "");
    // knownVersions is newest first; prefix matches are shown oldest first so "3.2" offers 3.20.0
    // (the likely intent) before 3.29.0.
    let suggestions = knownVersions.filter(function (v) { return v.startsWith(prefix); }).reverse();
    if (!suggestions.length) {
      suggestions = knownVersions;
    }
    suggestions = suggestions.slice(0, 3);

    hladbHint.textContent = "\u201c" + typed + "\u201d isn\u2019t an IMGT/HLA release this service can use. "
      + "Did you mean " + suggestions.join(", ") + "? Or clear the field to use the latest.";
    hladbHint.hidden = false;
    hladbVersion.setAttribute("aria-invalid", "true");
    return false;
  }

  hladbVersion.addEventListener("blur", checkHladbVersion);
  hladbVersion.addEventListener("input", clearHladbHint);
  loadHladbVersions();

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
    // The list is cached server-side for a day, so a release published since could look unknown
    // here. Block the first submit with the hint, but let the same value through if the user submits
    // again: the server remains the authority and rejects a genuinely bad one with a clear message.
    if (!checkHladbVersion()) {
      if (hladbVersion.value.trim() !== lastBlockedVersion) {
        lastBlockedVersion = hladbVersion.value.trim();
        hladbHint.textContent += " Submit again to try it anyway.";
        hladbVersion.focus();
        return;
      }
    }

    if (usesNmdp2026() && !acceptNmdp2026Terms.checked) {
      showError("Tick \u201cI have read the terms\u201d to use the nmdp-2026 frequencies; they are downloaded from NMDP\u2019s Zenodo record under CC BY-NC-ND 4.0.");
      nmdp2026Terms.hidden = false;
      return;
    }

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
      if (usesNmdp2026()) {
        formData.append("acceptNmdp2026Terms", "true");
      }
    }

    lastSubmittedNmdp2026 = usesNmdp2026();
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
  function formatMb(bytes) {
    return (bytes / 1048576).toFixed(bytes >= 10485760 ? 0 : 1) + " MB";
  }

  function updateProgress(status) {
    if (status.phase === "QUEUED") {
      progressPhase.textContent = "Queued…";
      progressBar.removeAttribute("value");
      progressCount.textContent = "";
    } else if (status.phase === "LOADING_REFERENCE_DATA") {
      if (status.downloadFile && status.downloadTotalBytes > 0) {
        // A real percentage while a reference file downloads; the parse that follows has no
        // progress signal, so the bar goes indeterminate again after this.
        const done = status.downloadedBytes || 0;
        progressPhase.textContent = "Downloading " + status.downloadFile + " from NMDP\u2019s Zenodo record\u2026";
        progressBar.value = Math.min(100, Math.floor((done * 100) / status.downloadTotalBytes));
        progressCount.textContent = formatMb(done) + " of " + formatMb(status.downloadTotalBytes);
      } else {
        progressPhase.textContent = lastSubmittedNmdp2026
          ? "Preparing frequencies\u2026 loading the NMDP 2026 files into memory (the first run also downloads them; large files can take several minutes)"
          : "Loading reference data\u2026 (large custom frequency files can take several minutes)";
        progressBar.removeAttribute("value");
        progressCount.textContent = "";
      }
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

  // Best-effort: shows the running build's version in the footer, same rationale as the CLI
  // tools' -h/-a version display (issue #89). Silently leaves the footer empty on any failure
  // (endpoint disabled, network hiccup, unexpected response shape) rather than showing an error
  // for something this cosmetic.
  (function loadVersion() {
    const footer = document.getElementById("app-version");
    fetch("/actuator/info")
      .then(function (response) {
        return response.ok ? response.json() : null;
      })
      .then(function (info) {
        const build = info && info.build;
        if (build && build.name && build.version) {
          footer.textContent = build.name + " " + build.version;
        }
      })
      .catch(function () {
        // Best-effort, see above.
      });
  })();
})();
