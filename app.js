async function analyzeCode() {
const codeInput = document.getElementById("codeInput");
const resultBox = document.getElementById("resultBox");
const button = document.getElementById("analyzeButton");


const code = codeInput.value;

if (!code.trim()) {
    resultBox.innerHTML = `
        <div class="empty-result">
            <div class="empty-icon">⚠</div>
            <h3>Please enter Java code</h3>
            <p>Paste a Java program into the code editor.</p>
        </div>
    `;
    return;
}

button.disabled = true;
button.textContent = "Analyzing...";

resultBox.innerHTML = `
    <div class="loading-result">
        <div class="empty-icon">🔍</div>
        <h3>Analyzing Program...</h3>
        <p>JavaParser → Data Flow → Constraints → Z3 → Error Detection</p>
    </div>
`;

try {
    const response = await fetch(
        "https://logical-error-detector-backend.onrender.com/analyze",
        {
            method: "POST",
            headers: {
                "Content-Type": "text/plain"
            },
            body: code
        }
    );

    const result = await response.text();

    if (!response.ok) {
        throw new Error(result);
    }

    const syntaxErrors = extractSyntaxErrors(result);
    const logicalErrors = extractErrors(result);

    const hasSyntaxErrors =
        syntaxErrors.length > 0 ||
        /Syntax errors detected!/i.test(result);

    const hasLogicalErrors =
        logicalErrors.length > 0;

    displayCombinedResult(
        result,
        hasSyntaxErrors,
        hasLogicalErrors
    );

} catch (error) {
    resultBox.innerHTML = `
        <div class="error-result">
            <div class="result-header error-header">
                <span>⚠</span>
                <div>
                    <h2>Backend Not Connected</h2>
                    <p>The analysis server could not be reached.</p>
                </div>
            </div>

            <p>
                <b>${escapeHtml(error.message)}</b>
            </p>
        </div>
    `;
} finally {
    button.disabled = false;
    button.textContent = "Analyze Code";
}


}

/* =========================================================
DISPLAY COMBINED RESULT
========================================================= */

function displayCombinedResult(
result,
hasSyntaxErrors,
hasLogicalErrors
) {
const resultBox = document.getElementById("resultBox");


const syntaxErrors = extractSyntaxErrors(result);
const logicalErrors = extractErrors(result);

const syntaxCount = syntaxErrors.length;
const logicalCount = logicalErrors.length;
const totalErrors = syntaxCount + logicalCount;

if (!hasSyntaxErrors && logicalCount === 0) {
    displaySuccessResult(result);
    return;
}

let title;
let message;

if (hasSyntaxErrors && logicalCount > 0) {
    title = "Syntax & Logical Errors Detected";
    message =
        "The analyzer found both syntax and logical problems in the Java program.";
} else if (hasSyntaxErrors) {
    title = "Syntax Errors Detected";
    message =
        "The analyzer found syntax errors in the Java program.";
} else {
    title = "Logical Errors Detected";
    message =
        "The analyzer found logical problems in the Java program.";
}


/* =====================================================
   SYNTAX CARDS
   ===================================================== */

let syntaxSection = "";

if (hasSyntaxErrors) {
    let syntaxCards = "";

    if (syntaxErrors.length > 0) {
        syntaxCards = syntaxErrors
            .map((error, index) => {
                return `
                    <div class="detected-error-card">
                        <div class="error-title">
                            <span class="error-badge">
                                ERROR ${index + 1}
                            </span>

                            <h3>Syntax Error</h3>
                        </div>

                        <p>
                            📍
                            <strong>
                                Line ${escapeHtml(error.line)}
                            </strong>

                            ${
                                error.column !== "Unknown"
                                    ? `, Column ${escapeHtml(error.column)}`
                                    : ""
                            }

                            — Error found at this location.
                        </p>

                        <p>
                            ${escapeHtml(error.description)}
                        </p>
                    </div>
                `;
            })
            .join("");
    } else {
        syntaxCards = `
            <div class="detected-error-card">
                <div class="error-title">
                    <span class="error-badge">
                        ERROR
                    </span>

                    <h3>Syntax Error</h3>
                </div>

                <p>
                    The Java source code contains
                    a syntax or parsing error.
                </p>
            </div>
        `;
    }

    syntaxSection = `
        <div class="detected-errors">
            <h2>Detected Syntax Errors</h2>
            ${syntaxCards}
        </div>
    `;
}


/* =====================================================
   LOGICAL ERROR CARDS
   ===================================================== */

let logicalSection = "";

if (hasLogicalErrors) {
    let logicalCards = "";

    if (logicalErrors.length > 0) {
        logicalCards = logicalErrors
            .map((error, index) => {
                return `
                    <div class="detected-error-card">

                        <div class="error-title">
                            <span class="error-badge">
                                ERROR ${index + 1}
                            </span>

                            <h3>
                                ${escapeHtml(error.type)}
                            </h3>
                        </div>

                        <p>
                            📍
                            <strong>
                                Line ${escapeHtml(error.line)}
                            </strong>

                            — Error found at this line.
                        </p>

                        <p>
                            ${escapeHtml(error.description)}
                        </p>

                        ${
                            error.condition
                                ? `
                                    <p>
                                        Condition:
                                        <code>
                                            ${escapeHtml(error.condition)}
                                        </code>
                                    </p>
                                `
                                : ""
                        }

                    </div>
                `;
            })
            .join("");
    } else {
        logicalCards = `
            <div class="detected-error-card">
                <div class="error-title">
                    <span class="error-badge">
                        ERROR
                    </span>

                    <h3>Logical Error</h3>
                </div>

                <p>
                    The analyzer detected a logical
                    problem in the Java program.
                </p>
            </div>
        `;
    }

    logicalSection = `
        <div class="detected-errors">
            <h2>Detected Logical Errors</h2>
            ${logicalCards}
        </div>
    `;
}


/* =====================================================
   SUMMARY
   ===================================================== */

const syntaxStatus =
    hasSyntaxErrors
        ? `${syntaxCount || 1} Syntax Error${(syntaxCount || 1) === 1 ? "" : "s"} Found`
        : "No Syntax Errors";

const logicalStatus =
    logicalCount > 0
        ? `${logicalCount} Logical Error${logicalCount === 1 ? "" : "s"} Found`
        : "No Logical Errors";


/* =====================================================
   FINAL HTML
   ===================================================== */

resultBox.innerHTML = `
    <div class="result-header error-header">
        <span>✗</span>

        <div>
            <h2>${title}</h2>

            <p>${message}</p>
        </div>
    </div>


    <div class="analysis-summary">

        <div class="summary-card">
            <div class="summary-icon">
                ✗
            </div>

            <div>
                <span>Status</span>

                <strong>
                    ${totalErrors}
                    Error${totalErrors === 1 ? "" : "s"} Found
                </strong>
            </div>
        </div>


        <div class="summary-card">
            <div class="summary-icon">
                ${syntaxCount}
            </div>

            <div>
                <span>Syntax</span>

                <strong>
                    ${syntaxStatus}
                </strong>
            </div>
        </div>


        <div class="summary-card">
            <div class="summary-icon">
                ${logicalCount}
            </div>

            <div>
                <span>Logical</span>

                <strong>
                    ${logicalStatus}
                </strong>
            </div>
        </div>

    </div>


    ${syntaxSection}

    ${logicalSection}


    <details class="technical-report">
        <summary>
            View Complete Technical Analysis Report
        </summary>

        <pre>${escapeHtml(result)}</pre>
    </details>
`;


}

/* =========================================================
EXTRACT SYNTAX ERRORS
========================================================= */

function extractSyntaxErrors(result) {
const errors = [];
const lines = result.split("\n");


for (const line of lines) {
    const currentLine = line.trim();

    const locationMatch = currentLine.match(
        /^Line\s+(\d+),\s*Column\s+(\d+):\s*(.*)$/i
    );

    if (!locationMatch) {
        continue;
    }

    const lineNumber = locationMatch[1];
    const columnNumber = locationMatch[2];

    let description = locationMatch[3].trim();

    if (!description) {
        description = "Invalid Java syntax.";
    }

    if (description.length > 500) {
        description =
            description.substring(0, 500) + "...";
    }

    errors.push({
        line: lineNumber,
        column: columnNumber,
        description: description
    });
}

return removeDuplicateSyntaxErrors(errors);


}

/* =========================================================
REMOVE DUPLICATE SYNTAX ERRORS
========================================================= */

function removeDuplicateSyntaxErrors(errors) {
const uniqueErrors = [];
const seen = new Set();


for (const error of errors) {
    const key =
        error.line +
        "|" +
        error.column +
        "|" +
        error.description;

    if (!seen.has(key)) {
        seen.add(key);
        uniqueErrors.push(error);
    }
}

return uniqueErrors;


}

/* =========================================================
EXTRACT LOGICAL ERRORS
========================================================= */

function extractErrors(result) {
const errors = [];
const lines = result.split("\n");


for (let i = 0; i < lines.length; i++) {
    const current = lines[i].trim();

    if (!current.startsWith("Error Type:")) {
        continue;
    }

    const type =
        current.substring("Error Type:".length).trim();

    let lineNumber = "Unknown";
    let condition = "";
    let description = "";

    /*
     * Read ONLY the current error block.
     */
    for (let j = i + 1; j < lines.length; j++) {
        const text = lines[j].trim();

        /*
         * A new error begins.
         */
        if (
            text.startsWith("Error Type:")
            ||
            text.startsWith("Logical Error Detected!")
        ) {
            break;
        }

        /*
         * Stop at the final report.
         */
        if (
            text.startsWith("FINAL ANALYSIS REPORT")
            ||
            text.startsWith("Only logical errors were detected.")
            ||
            text.startsWith("No logical errors were detected.")
            ||
            text.startsWith("No syntax errors were detected.")
            ||
            text.startsWith("Syntax errors were detected.")
            ||
            text.startsWith("Logical analysis was completed.")
            ||
            text.startsWith("Logical analysis was also performed.")
        ) {
            break;
        }

        /*
         * VERY IMPORTANT:
         *
         * If another IF / ELSE / LOOP analysis line appears,
         * it belongs to the next analysis block, not this error.
         *
         * This fixes:
         *
         * Condition 'age < 10' is false...
         * IF at line 12: marks > 90
         */
        if (
            /^(IF|ELSE IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+\d+:/i.test(text)
        ) {
            break;
        }

        /*
         * Read error line.
         */
        const lineMatch = text.match(
            /^Line:\s*(\d+)/i
        );

        if (lineMatch) {
            lineNumber = lineMatch[1];
            continue;
        }

        /*
         * Read condition.
         */
        const conditionMatch = text.match(
            /^Condition:\s*(.*)$/i
        );

        if (conditionMatch) {
            condition = conditionMatch[1].trim();
            continue;
        }

        /*
         * Ignore technical information.
         */
        if (
            text.startsWith("Known variable values:")
            ||
            text.startsWith("Generated Constraint:")
            ||
            text.startsWith("Z3 Result:")
            ||
            text === "----------------------------------------"
            ||
            text === ""
        ) {
            continue;
        }

        /*
         * Ignore headings.
         */
        if (
            text.startsWith("LOGICAL ERROR ANALYSIS")
            ||
            text.startsWith("LOGICAL ANALYSIS")
            ||
            text.startsWith("BRANCH ANALYSIS")
            ||
            text.startsWith("NESTED IF ANALYSIS")
            ||
            text.startsWith("PROGRAM-ORDER DATA-FLOW ANALYSIS")
            ||
            text.startsWith("IF / ELSE / LOOP ANALYSIS")
            ||
            text.startsWith("STATIC ANALYSIS TOOL")
        ) {
            continue;
        }

        /*
         * Stop at separator.
         */
        if (/^={5,}$/.test(text)) {
            break;
        }

        /*
         * Add description.
         */
        if (description.length === 0) {
            description = text;
        } else {
            description += " " + text;
        }
    }

    description =
        cleanLogicalDescription(description);

    if (!description) {
        description =
            "Logical problem detected for this condition.";
    }

    errors.push({
        line: lineNumber,
        type: type,
        condition: condition,
        description: description
    });
}

return removeDuplicateLogicalErrors(errors);


}

/* =========================================================
REMOVE DUPLICATE LOGICAL ERRORS
========================================================= */

function removeDuplicateLogicalErrors(errors) {
const uniqueErrors = [];
const seen = new Set();


for (const error of errors) {
    const key =
        error.line +
        "|" +
        error.type +
        "|" +
        error.condition;

    if (!seen.has(key)) {
        seen.add(key);
        uniqueErrors.push(error);
    }
}

return uniqueErrors;


}

/* =========================================================
CLEAN LOGICAL DESCRIPTION
========================================================= */

function cleanLogicalDescription(description) {
if (!description) {
return "";
}


let cleaned = description;

/*
 * Remove accidental next-analysis content.
 */
cleaned = cleaned.replace(
    /\s+(IF|ELSE IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+\d+:.*$/i,
    ""
);

/*
 * Remove report sections.
 */
const stopPatterns = [
    "========================================",
    "FINAL ANALYSIS REPORT",
    "Only logical errors were detected.",
    "No logical errors were detected.",
    "No syntax errors were detected.",
    "Syntax errors were detected.",
    "Logical analysis was completed.",
    "Logical analysis was also performed."
];

for (const pattern of stopPatterns) {
    const index = cleaned.indexOf(pattern);

    if (index >= 0) {
        cleaned = cleaned.substring(0, index);
    }
}

return cleaned
    .replace(/\s+/g, " ")
    .trim();


}

/* =========================================================
DISPLAY SUCCESS RESULT
========================================================= */

function displaySuccessResult(result) {
const resultBox =
document.getElementById("resultBox");


resultBox.innerHTML = `
    <div class="result-header success-header">
        <span>✓</span>

        <div>
            <h2>Analysis Completed</h2>

            <p>
                No syntax or logical errors were detected.
            </p>
        </div>
    </div>


    <div class="analysis-summary">

        <div class="summary-card">
            <div class="summary-icon">
                ✓
            </div>

            <div>
                <span>Status</span>

                <strong>
                    No Errors Found
                </strong>
            </div>
        </div>


        <div class="summary-card">
            <div class="summary-icon">
                0
            </div>

            <div>
                <span>Errors</span>

                <strong>
                    0 Errors
                </strong>
            </div>
        </div>


        <div class="summary-card">
            <div class="summary-icon">
                ✓
            </div>

            <div>
                <span>Syntax</span>

                <strong>
                    No Syntax Errors
                </strong>
            </div>
        </div>

    </div>


    <details class="technical-report">
        <summary>
            View Complete Technical Analysis Report
        </summary>

        <pre>${escapeHtml(result)}</pre>
    </details>
`;


}

/* =========================================================
CLEAR CODE
========================================================= */

function clearCode() {
document.getElementById("codeInput").value = "";


document.getElementById("resultBox").innerHTML = `
    <div class="empty-result">

        <div class="empty-icon">
            🔍
        </div>

        <h3>
            Ready for Analysis
        </h3>

        <p>
            Enter Java code above and click
            <b>Analyze Code</b>.
        </p>

    </div>
`;


}

/* =========================================================
HTML ESCAPE
========================================================= */

function escapeHtml(text) {
const div = document.createElement("div");


div.textContent =
    text == null
        ? ""
        : String(text);

return div.innerHTML;


}
