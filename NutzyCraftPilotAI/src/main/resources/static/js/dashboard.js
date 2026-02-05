const btn = document.getElementById("analyzeBtn");
const attachBtn = document.getElementById("attachBtn");
const input = document.getElementById("videoUrlInput");
const fileInput = document.getElementById("fileInput");
const fileList = document.getElementById("fileList");
const progressSection = document.getElementById("progressSection");
const progressBar = document.getElementById("progressBar");
const percentText = document.getElementById("percentText");
const resultsSection = document.getElementById("resultsSection");

let selectedFiles = [];
let currentAnalysisData = null;

attachBtn.addEventListener("click", () => fileInput.click());
attachBtn.addEventListener(
  "mouseover",
  () => (attachBtn.style.background = "#e8eaed"),
);
attachBtn.addEventListener(
  "mouseout",
  () => (attachBtn.style.background = "#f1f3f4"),
);
fileInput.addEventListener("change", () => {
  if (fileInput.files.length > 0) handleFiles(fileInput.files);
});

function handleFiles(files) {
  selectedFiles = Array.from(files);
  renderFileList();
}

function renderFileList() {
  if (selectedFiles.length === 0) {
    fileList.style.display = "none";
    return;
  }
  fileList.style.display = "flex";
  fileList.style.flexWrap = "wrap";
  fileList.style.gap = "0.5rem";
  fileList.innerHTML = selectedFiles
    .map(
      (file) => `
        <div style="background: #fff; border: 1px solid var(--labs-border); padding: 0.5rem 1rem; border-radius: 20px; font-size: 0.85rem; display: flex; align-items: center; gap: 0.5rem;">
            <span>📄</span>
            <span style="font-weight: 500;">${file.name}</span>
            <span style="color: #d93025; cursor: pointer; font-weight: bold; margin-left: 0.25rem;" onclick="removeFile('${file.name}')">×</span>
        </div>
    `,
    )
    .join("");
}

window.removeFile = function (fileName) {
  selectedFiles = selectedFiles.filter((f) => f.name !== fileName);
  renderFileList();
  fileInput.value = "";
};

async function startAnalysis() {
  const hasUrl = input.value.trim().length > 0;
  const hasFiles = selectedFiles.length > 0;

  if (!hasUrl && !hasFiles) {
    input.focus();
    return;
  }

  const userJson = localStorage.getItem("user");
  const user = userJson ? JSON.parse(userJson) : { id: "anonymous" };

  btn.disabled = true;
  btn.innerText = "Analyzing...";
  btn.classList.add("analyzing-state");
  progressSection.style.display = "block";
  resultsSection.style.display = "none";
  input.disabled = true;
  attachBtn.disabled = true;

  let width = 0;
  const interval = setInterval(() => {
    if (width < 90) width += 1;
    progressBar.style.width = width + "%";
    percentText.innerText = width + "%";
  }, 300);

  try {
    const response = await fetch("http://209.97.161.131/ai/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        youtube_url: hasUrl ? input.value : "",
        user_id: user.id,
      }),
    });

    const data = await response.json();
    clearInterval(interval);

    if (response.ok && data.success) {
      progressBar.style.width = "100%";
      percentText.innerText = "100%";

      setTimeout(() => {
        progressSection.style.display = "none";
        resultsSection.style.display = "block";
        currentAnalysisData = data;

        document.getElementById("videoTitleElement").innerText =
          data.title || "Educational Video Content";
        document.getElementById("subjectAreaElement").innerText =
          "📚 " + (data.subject_area || "General Education");
        document.getElementById("difficultyBadge").innerText =
          data.difficulty_level || "Intermediate";
        document.getElementById("videoSummaryElement").innerText =
          data.summary || "This video covers important educational content.";

        const prereqElement = document.getElementById("prerequisitesElement");
        prereqElement.innerHTML =
          data.prerequisites?.length > 0
            ? data.prerequisites.map((p) => `<li>${p}</li>`).join("")
            : "<li>No specific prerequisites required</li>";

        const objectivesElement = document.getElementById(
          "learningObjectivesElement",
        );
        objectivesElement.innerHTML =
          data.learning_objectives?.length > 0
            ? data.learning_objectives
                .map(
                  (obj, idx) => `
                        <div style="display: flex; gap: 0.75rem; align-items: start;">
                            <span style="background: #667eea; color: white; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: 600; flex-shrink: 0;">${idx + 1}</span>
                            <p style="margin: 0; line-height: 1.6; color: var(--labs-black);">${obj}</p>
                        </div>
                    `,
                )
                .join("")
            : '<p style="margin: 0; color: var(--labs-grey);">Learning objectives not specified</p>';

        const explanationElement = document.getElementById(
          "detailedExplanationElement",
        );
        explanationElement.innerHTML = data.detailed_explanation
          ? data.detailed_explanation
              .split("\n\n")
              .filter((p) => p.trim())
              .map((p) => `<p style="margin-bottom: 1.5rem;">${p.trim()}</p>`)
              .join("")
          : "<p>No detailed explanation available</p>";

        const conceptsElement = document.getElementById("keyConceptsElement");
        conceptsElement.innerHTML =
          data.key_concepts?.length > 0
            ? data.key_concepts
                .map(
                  (c) => `
                        <div style="padding: 1rem 1.25rem; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 8px; border-left: 3px solid #667eea;">
                            <p style="margin: 0; font-weight: 500; color: var(--labs-black);">${c}</p>
                        </div>
                    `,
                )
                .join("")
            : '<div style="padding: 1rem; background: #f8f9fa; border-radius: 8px;"><p style="margin: 0;">No key concepts extracted</p></div>';

        const examplesElement = document.getElementById("examplesElement");
        examplesElement.innerHTML =
          data.examples_covered?.length > 0
            ? data.examples_covered
                .map(
                  (ex, idx) => `
                        <div style="padding: 1rem; background: #f8f9fa; border-left: 3px solid #764ba2; border-radius: 6px;">
                            <p style="margin: 0; line-height: 1.6; color: var(--labs-black);"><strong>Example ${idx + 1}:</strong> ${ex}</p>
                        </div>
                    `,
                )
                .join("")
            : '<p style="margin: 0; color: var(--labs-grey);">No specific examples identified</p>';

        const takeawaysElement = document.getElementById("keyTakeawaysElement");
        takeawaysElement.innerHTML =
          data.key_takeaways?.length > 0
            ? data.key_takeaways
                .map((t) => `<li style="margin-bottom: 0.5rem;">${t}</li>`)
                .join("")
            : "<li>Review the detailed explanation above</li>";

        const reports = JSON.parse(
          localStorage.getItem("savedReports") || "[]",
        );
        reports.unshift({
          id: Date.now(),
          title:
            data.title || "Video Analysis " + new Date().toLocaleTimeString(),
          videoUrl: hasUrl ? input.value : "File Upload",
          createdAt: new Date().toISOString(),
          summary: data.summary,
          subject_area: data.subject_area,
          difficulty_level: data.difficulty_level,
        });
        localStorage.setItem("savedReports", JSON.stringify(reports));

        input.value = "";
        selectedFiles = [];
        renderFileList();
      }, 500);
    } else {
      throw new Error(data.detail || data.error || "Analysis failed");
    }
  } catch (e) {
    clearInterval(interval);
    alert("Analysis failed: " + e.message);
    progressSection.style.display = "none";
  } finally {
    btn.disabled = false;
    btn.innerText = "Analyze";
    btn.classList.remove("analyzing-state");
    input.disabled = false;
    attachBtn.disabled = false;
  }
}

btn.addEventListener("click", startAnalysis);

async function loadUserProfile() {
  const userJson = localStorage.getItem("user");
  if (!userJson) {
    window.location.href = "login.html";
    return;
  }

  const user = JSON.parse(userJson);
  try {
    const res = await fetch(`/api/user/me?userId=${user.id}`);
    if (res.ok) {
      const data = await res.json();
      if (data.success) {
        const profileName = document.querySelector(
          '.navbar a[href="settings.html"] span',
        );
        const profileAvatar = document.querySelector(
          '.navbar a[href="settings.html"] div',
        );

        if (profileName)
          profileName.innerText = data.fullName || user.name || "User";
        if (profileAvatar) {
          if (data.avatarUrl) {
            Object.assign(profileAvatar.style, {
              background: "none",
              backgroundImage: `url('${data.avatarUrl}')`,
              backgroundSize: "cover",
              backgroundPosition: "center",
            });
          } else {
            Object.assign(profileAvatar.style, {
              background: "linear-gradient(135deg, #667eea, #764ba2)",
              backgroundImage: "none",
            });
          }
        }
      }
    }
  } catch (e) {}
}

document.addEventListener("DOMContentLoaded", () => {
  loadUserProfile();
  initializeGoogleAPIs();
});

function initializeGoogleAPIs() {
  const timeout = 10000;
  const startTime = Date.now();

  const checkAPIs = setInterval(() => {
    if (typeof gapi !== "undefined") gapiLoaded();
    if (typeof google !== "undefined" && google.accounts) gisLoaded();

    if ((gapiInited && gisInited) || Date.now() - startTime > timeout) {
      clearInterval(checkAPIs);
    }
  }, 100);
}

document.getElementById("savePdfBtn")?.addEventListener("click", generatePDF);

async function generatePDF() {
  if (!currentAnalysisData) {
    alert("No analysis data available to save.");
    return;
  }

  const { jsPDF } = window.jspdf;
  const doc = new jsPDF();

  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  const margin = 20;
  const maxWidth = pageWidth - 2 * margin;
  let yPosition = margin;

  function addText(text, fontSize, fontStyle = "normal", color = [0, 0, 0]) {
    doc.setFontSize(fontSize);
    doc.setFont("helvetica", fontStyle);
    doc.setTextColor(...color);
    const lines = doc.splitTextToSize(text, maxWidth);

    // Process each line individually to handle page breaks properly
    for (let i = 0; i < lines.length; i++) {
      const lineHeight = fontSize * 0.5;

      // Check if we need a new page before adding this line
      if (yPosition + lineHeight > pageHeight - margin) {
        doc.addPage();
        yPosition = margin;
      }

      // Add the single line
      doc.text(lines[i], margin, yPosition);
      yPosition += lineHeight;
    }

    // Add spacing after the text block
    yPosition += 5;
  }

  function addColorBox(text, bgColor, textColor) {
    const boxHeight = 10;
    const minContentSpace = 30; // Space needed for at least a few lines of content
    
    // Check if there's room for the box AND some content
    // If not, start the section on a new page
    if (yPosition + boxHeight + minContentSpace > pageHeight - margin) {
      doc.addPage();
      yPosition = margin;
    }
    
    doc.setFillColor(...bgColor);
    doc.setDrawColor(...bgColor);
    doc.roundedRect(margin, yPosition - 7, maxWidth, boxHeight, 2, 2, "F");
    doc.setTextColor(...textColor);
    doc.setFontSize(12);
    doc.setFont("helvetica", "bold");
    doc.text(text, margin + 3, yPosition);
    yPosition += boxHeight + 3;
  }

  doc.setFillColor(102, 126, 234);
  doc.rect(0, 0, pageWidth, 35, "F");
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(24);
  doc.setFont("helvetica", "bold");
  doc.text("Learning Summary", pageWidth / 2, 20, { align: "center" });
  yPosition = 45;

  doc.setFillColor(245, 247, 250);
  doc.roundedRect(margin, yPosition - 5, maxWidth, 20, 3, 3, "F");
  doc.setTextColor(0, 0, 0);
  doc.setFontSize(16);
  doc.setFont("helvetica", "bold");
  const titleLines = doc.splitTextToSize(
    currentAnalysisData.title || "Educational Video Analysis",
    maxWidth - 10,
  );
  doc.text(titleLines, margin + 5, yPosition + 5);
  yPosition += 25;

  doc.setFillColor(102, 126, 234);
  doc.setDrawColor(102, 126, 234);
  doc.roundedRect(margin, yPosition, 60, 8, 2, 2, "FD");
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(9);
  doc.setFont("helvetica", "bold");
  doc.text("SUBJECT", margin + 3, yPosition + 5);

  doc.setTextColor(0, 0, 0);
  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");
  doc.text(
    currentAnalysisData.subject_area || "General",
    margin + 65,
    yPosition + 5,
  );

  yPosition += 10;

  doc.setFillColor(118, 75, 162);
  doc.setDrawColor(118, 75, 162);
  doc.roundedRect(margin, yPosition, 60, 8, 2, 2, "FD");
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(9);
  doc.setFont("helvetica", "bold");
  doc.text("DIFFICULTY", margin + 3, yPosition + 5);

  doc.setTextColor(0, 0, 0);
  doc.setFontSize(10);
  doc.setFont("helvetica", "normal");
  doc.text(
    currentAnalysisData.difficulty_level || "Intermediate",
    margin + 65,
    yPosition + 5,
  );
  yPosition += 15;

  addColorBox("SUMMARY", [102, 126, 234], [255, 255, 255]);
  addText(currentAnalysisData.summary || "", 10, "normal");
  yPosition += 5;

  if (currentAnalysisData.prerequisites?.length > 0) {
    addColorBox("PREREQUISITES", [255, 193, 7], [0, 0, 0]);
    currentAnalysisData.prerequisites.forEach((prereq, idx) => {
      addText(`${idx + 1}. ${prereq}`, 10, "normal");
    });
    yPosition += 5;
  }

  if (currentAnalysisData.learning_objectives?.length > 0) {
    addColorBox("LEARNING OBJECTIVES", [76, 175, 80], [255, 255, 255]);
    currentAnalysisData.learning_objectives.forEach((obj, idx) => {
      addText(`${idx + 1}. ${obj}`, 10, "normal");
    });
    yPosition += 5;
  }

  if (currentAnalysisData.key_concepts?.length > 0) {
    addColorBox("KEY CONCEPTS", [33, 150, 243], [255, 255, 255]);
    currentAnalysisData.key_concepts.forEach((concept) => {
      addText(`• ${concept}`, 10, "normal");
    });
    yPosition += 5;
  }

  if (currentAnalysisData.detailed_explanation) {
    addColorBox("DETAILED EXPLANATION", [156, 39, 176], [255, 255, 255]);
    addText(currentAnalysisData.detailed_explanation, 10, "normal");
    yPosition += 5;
  }

  if (currentAnalysisData.examples_covered?.length > 0) {
    addColorBox("EXAMPLES & DEMONSTRATIONS", [255, 152, 0], [255, 255, 255]);
    currentAnalysisData.examples_covered.forEach((example, idx) => {
      addText(`Example ${idx + 1}: ${example}`, 10, "normal");
    });
    yPosition += 5;
  }

  if (currentAnalysisData.key_takeaways?.length > 0) {
    addColorBox("KEY TAKEAWAYS", [233, 30, 99], [255, 255, 255]);
    currentAnalysisData.key_takeaways.forEach((takeaway, idx) => {
      addText(`${idx + 1}. ${takeaway}`, 10, "normal");
    });
  }

  const footerY = pageHeight - 15;
  doc.setFillColor(102, 126, 234);
  doc.rect(0, footerY, pageWidth, 15, "F");
  doc.setFontSize(8);
  doc.setTextColor(255, 255, 255);
  doc.text(
    `Generated on ${new Date().toLocaleString()} | PilotAI Learning Assistant`,
    pageWidth / 2,
    footerY + 10,
    { align: "center" },
  );

  const fileName = `${currentAnalysisData.title?.replace(/[^a-z0-9]/gi, "_").substring(0, 50) || "learning_summary"}.pdf`;
  doc.save(fileName);

  showSaveStatus("PDF saved successfully! ✅", "success");
}

const CLIENT_ID =
  "613986529272-sn9pj1apoer7vl9jaehjmoabs7uplefu.apps.googleusercontent.com";
const DISCOVERY_DOCS = [
  "https://www.googleapis.com/discovery/v1/apis/drive/v3/rest",
];
const SCOPES = "https://www.googleapis.com/auth/drive.file";

let gapiInited = false;
let gisInited = false;
let tokenClient;
let accessToken = null;

document
  .getElementById("saveDriveBtn")
  ?.addEventListener("click", handleDriveSave);

function gapiLoaded() {
  gapi.load("client", async () => {
    try {
      await gapi.client.init({ discoveryDocs: DISCOVERY_DOCS });
      gapiInited = true;
    } catch (error) {
      gapiInited = false;
    }
  });
}

function gisLoaded() {
  if (typeof google !== "undefined" && google.accounts) {
    tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: CLIENT_ID,
      scope: SCOPES,
      callback: "",
    });
    gisInited = true;
  }
}

async function handleDriveSave() {
  if (!currentAnalysisData) {
    alert("No analysis data available to save.");
    return;
  }

  if (!gapiInited || !gisInited) {
    showSaveStatus("Initializing Google Drive connection...", "info");

    let attempts = 0;
    while ((!gapiInited || !gisInited) && attempts < 10) {
      await new Promise((resolve) => setTimeout(resolve, 500));
      attempts++;
    }

    if (!gapiInited || !gisInited) {
      showSaveStatus(
        "Failed to load Google Drive APIs. Please refresh the page and try again.",
        "error",
      );
      return;
    }
  }

  showSaveStatus("Authenticating with Google Drive...", "info");

  tokenClient.callback = async (resp) => {
    if (resp.error) {
      showSaveStatus("Authentication failed: " + resp.error, "error");
      return;
    }
    accessToken = resp.access_token;
    await uploadToDrive();
  };

  tokenClient.requestAccessToken({ prompt: accessToken ? "" : "consent" });
}

async function uploadToDrive() {
  try {
    showSaveStatus("Creating document...", "info");

    const { jsPDF } = window.jspdf;
    const doc = new jsPDF();

    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const margin = 20;
    const maxWidth = pageWidth - 2 * margin;
    let yPosition = margin;

        function addText(text, fontSize, fontStyle = 'normal', color = [0, 0, 0]) {
            doc.setFontSize(fontSize);
            doc.setFont('helvetica', fontStyle);
            doc.setTextColor(...color);
            const lines = doc.splitTextToSize(text, maxWidth);
            
            // Process each line individually to handle page breaks properly
            for (let i = 0; i < lines.length; i++) {
                const lineHeight = fontSize * 0.5;
                
                // Check if we need a new page before adding this line
                if (yPosition + lineHeight > pageHeight - margin) {
                    doc.addPage();
                    yPosition = margin;
                }
                
                // Add the single line
                doc.text(lines[i], margin, yPosition);
                yPosition += lineHeight;
            }
            
            // Add spacing after the text block
            yPosition += 5;
        }

    function addColorBox(text, bgColor, textColor) {
      const boxHeight = 10;
      const minContentSpace = 30; // Space needed for at least a few lines of content
      
      // Check if there's room for the box AND some content
      // If not, start the section on a new page
      if (yPosition + boxHeight + minContentSpace > pageHeight - margin) {
        doc.addPage();
        yPosition = margin;
      }
      
      doc.setFillColor(...bgColor);
      doc.roundedRect(margin, yPosition - 7, maxWidth, boxHeight, 2, 2, "F");
      doc.setTextColor(...textColor);
      doc.setFontSize(12);
      doc.setFont("helvetica", "bold");
      doc.text(text, margin + 3, yPosition);
      yPosition += boxHeight + 3;
    }

    doc.setFillColor(102, 126, 234);
    doc.rect(0, 0, pageWidth, 35, "F");
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(24);
    doc.setFont("helvetica", "bold");
    doc.text("Learning Summary", pageWidth / 2, 20, { align: "center" });
    yPosition = 45;

    doc.setFillColor(245, 247, 250);
    doc.roundedRect(margin, yPosition - 5, maxWidth, 20, 3, 3, "F");
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(16);
    doc.setFont("helvetica", "bold");
    const titleLines = doc.splitTextToSize(
      currentAnalysisData.title || "Educational Video Analysis",
      maxWidth - 10,
    );
    doc.text(titleLines, margin + 5, yPosition + 5);
    yPosition += 25;

    doc.setFillColor(102, 126, 234);
    doc.roundedRect(margin, yPosition, 60, 8, 2, 2, "FD");
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.setFont("helvetica", "bold");
    doc.text("SUBJECT", margin + 3, yPosition + 5);
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.text(
      currentAnalysisData.subject_area || "General",
      margin + 65,
      yPosition + 5,
    );
    yPosition += 10;

    doc.setFillColor(118, 75, 162);
    doc.roundedRect(margin, yPosition, 60, 8, 2, 2, "FD");
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.setFont("helvetica", "bold");
    doc.text("DIFFICULTY", margin + 3, yPosition + 5);
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.text(
      currentAnalysisData.difficulty_level || "Intermediate",
      margin + 65,
      yPosition + 5,
    );
    yPosition += 15;

    if (currentAnalysisData.summary) {
      addColorBox("SUMMARY", [102, 126, 234], [255, 255, 255]);
      addText(currentAnalysisData.summary, 10, "normal");
      yPosition += 5;
    }

    if (currentAnalysisData.prerequisites?.length > 0) {
      addColorBox("PREREQUISITES", [255, 193, 7], [0, 0, 0]);
      currentAnalysisData.prerequisites.forEach((p, i) =>
        addText(`${i + 1}. ${p}`, 10, "normal"),
      );
      yPosition += 5;
    }

    if (currentAnalysisData.learning_objectives?.length > 0) {
      addColorBox("LEARNING OBJECTIVES", [76, 175, 80], [255, 255, 255]);
      currentAnalysisData.learning_objectives.forEach((o, i) =>
        addText(`${i + 1}. ${o}`, 10, "normal"),
      );
      yPosition += 5;
    }

    if (currentAnalysisData.key_concepts?.length > 0) {
      addColorBox("KEY CONCEPTS", [33, 150, 243], [255, 255, 255]);
      currentAnalysisData.key_concepts.forEach((c) =>
        addText(`• ${c}`, 10, "normal"),
      );
      yPosition += 5;
    }

    if (currentAnalysisData.detailed_explanation) {
      addColorBox("DETAILED EXPLANATION", [156, 39, 176], [255, 255, 255]);
      addText(currentAnalysisData.detailed_explanation, 10, "normal");
      yPosition += 5;
    }

    if (currentAnalysisData.examples_covered?.length > 0) {
      addColorBox("EXAMPLES & DEMONSTRATIONS", [255, 152, 0], [255, 255, 255]);
      currentAnalysisData.examples_covered.forEach((e, i) =>
        addText(`Example ${i + 1}: ${e}`, 10, "normal"),
      );
      yPosition += 5;
    }

    if (currentAnalysisData.key_takeaways?.length > 0) {
      addColorBox("KEY TAKEAWAYS", [233, 30, 99], [255, 255, 255]);
      currentAnalysisData.key_takeaways.forEach((t, i) =>
        addText(`${i + 1}. ${t}`, 10, "normal"),
      );
    }

    const footerY = pageHeight - 15;
    doc.setFillColor(102, 126, 234);
    doc.rect(0, footerY, pageWidth, 15, "F");
    doc.setFontSize(8);
    doc.setTextColor(255, 255, 255);
    doc.text(
      `Generated on ${new Date().toLocaleString()} | PilotAI Learning Assistant`,
      pageWidth / 2,
      footerY + 10,
      { align: "center" },
    );

    const pdfBlob = doc.output("blob");
    const metadata = {
      name: `${currentAnalysisData.title?.replace(/[^a-z0-9]/gi, "_").substring(0, 50) || "learning_summary"}.pdf`,
      mimeType: "application/pdf",
    };

    const form = new FormData();
    form.append(
      "metadata",
      new Blob([JSON.stringify(metadata)], { type: "application/json" }),
    );
    form.append("file", pdfBlob);

    showSaveStatus("Uploading to Google Drive...", "info");

    const response = await fetch(
      "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
      {
        method: "POST",
        headers: new Headers({ Authorization: "Bearer " + accessToken }),
        body: form,
      },
    );

    const result = await response.json();

    if (response.ok) {
      showSaveStatus("Successfully saved to Google Drive! ✅", "success");
    } else {
      showSaveStatus(
        "Failed to save to Google Drive: " +
          (result.error?.message || "Unknown error"),
        "error",
      );
    }
  } catch (error) {
    showSaveStatus("Error: " + error.message, "error");
  }
}

function showSaveStatus(message, type) {
  const statusEl = document.getElementById("saveStatus");
  if (statusEl) {
    statusEl.style.display = "block";
    statusEl.innerText = message;
    statusEl.style.color =
      type === "success" ? "#137333" : type === "error" ? "#d93025" : "#5f6368";

    if (type === "success") {
      setTimeout(() => {
        statusEl.style.display = "none";
      }, 5000);
    }
  }
}
