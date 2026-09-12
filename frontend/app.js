async function analyzeCode() {
    const code = document.getElementById("codeInput").value;
    const resultBox = document.getElementById("resultBox");
    const button = document.getElementById("analyzeButton");

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

        const hasLogicalErrors =
            result.includes("Logical errors detected") ||
            result.includes("Logical Error Detected!");

        const hasSyntaxErrors =
            result.includes("Syntax errors detected!");

        if (hasLogicalErrors || hasSyntaxErrors) {
            displayErrorResult(result);
        } else {
            displaySuccessResult(result);
        }

    } catch (error) {

        resultBox.innerHTML = `
            <div class="error-result">
                <div class="result-header error-header">
                    <span>⚠</span>
                    <div>
                        <h2>Backend Not Connected</h2>
                        <p>There was a problem connecting to the analysis server.</p>
                    </div>
                </div>

                <p><b>${escapeHtml(error.message)}</b></p>
            </div>
        `;

    } finally {
        button.disabled = false;
        button.textContent = "Analyze Code";
    }
}


/* =====================================================
   DISPLAY ERROR RESULT
   ===================================================== */

function displayErrorResult(result) {

    const resultBox = document.getElementById("resultBox");

    const errors = extractErrors(result);

    const hasSyntaxErrors =
        result.includes("Syntax errors detected!");

    const hasLogicalErrors =
        result.includes("Logical errors detected") ||
        result.includes("Logical Error Detected!");

    /*
     * Count syntax errors separately.
     *
     * JavaParser normally reports syntax errors in a section
     * beginning with "Syntax errors detected!".
     */
    let syntaxErrorCount = 0;

    if (hasSyntaxErrors) {

        const syntaxSection =
            result.substring(
                result.indexOf("Syntax errors detected!")
            );

        /*
         * Count JavaParser error lines.
         * Typical format:
         * line 3:...
         */
        const syntaxMatches =
            syntaxSection.match(/(?:line\s+\d+|Line\s+\d+)/g);

        if (syntaxMatches && syntaxMatches.length > 0) {
            syntaxErrorCount = syntaxMatches.length;
        } else {
            /*
             * If the exact JavaParser format is different,
             * still count at least one syntax error.
             */
            syntaxErrorCount = 1;
        }
    }

    /*
     * Total errors = logical errors + syntax errors.
     */
    const totalErrorCount =
        errors.length + syntaxErrorCount;


    /*
     * Decide the main result heading.
     */
    let resultTitle = "Logical Errors Detected";

    let resultDescription =
        "The analyzer found logical problems in the Java program.";

    if (hasSyntaxErrors && !hasLogicalErrors) {

        resultTitle = "Syntax Errors Detected";

        resultDescription =
            "The analyzer found syntax problems in the Java program.";

    } else if (hasSyntaxErrors && hasLogicalErrors) {

        resultTitle = "Syntax and Logical Errors Detected";

        resultDescription =
            "The analyzer found syntax and logical problems in the Java program.";
    }


    /*
     * Decide the status text.
     */
    let statusText = "Logical Errors Found";

    if (hasSyntaxErrors && !hasLogicalErrors) {

        statusText = "Syntax Errors Found";

    } else if (hasSyntaxErrors && hasLogicalErrors) {

        statusText = "Multiple Errors Found";
    }


    /*
     * Syntax status.
     */
    const syntaxStatus =
        hasSyntaxErrors
        ? "Syntax Errors Found"
        : "No Syntax Errors";


    /*
     * Logical error cards.
     */
    let errorCards = "";

    if (errors.length > 0) {

        errorCards = `
            <div class="detected-errors">

                <h2>
                    ${
                        hasSyntaxErrors && hasLogicalErrors
                        ? "Detected Logical Errors"
                        : "Detected Logical Errors"
                    }
                </h2>

                ${errors.map((error, index) => `
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
                            📍 <strong>Line ${escapeHtml(String(error.line))}</strong>
                            — Error found at this line.
                        </p>

                        ${
                            error.description
                            ? `
                                <p>
                                    ${escapeHtml(error.description)}
                                </p>
                            `
                            : ""
                        }

                        ${
                            error.condition
                            ? `
                                <p>
                                    Condition:
                                    <code>${escapeHtml(error.condition)}</code>
                                </p>
                            `
                            : ""
                        }

                    </div>
                `).join("")}

            </div>
        `;
    }


    /*
     * Syntax error card.
     *
     * Syntax errors are not returned through the normal
     * "Error Type:" logical-error format, so display them
     * separately.
     */
    let syntaxCards = "";

    if (hasSyntaxErrors) {

        syntaxCards = `
            <div class="detected-errors">

                <h2>Detected Syntax Errors</h2>

                <div class="detected-error-card">

                    <div class="error-title">

                        <span class="error-badge">
                            SYNTAX
                        </span>

                        <h3>
                            Java Syntax Error
                        </h3>

                    </div>

                    <p>
                        The JavaParser could not parse the program because
                        the Java source contains a syntax error.
                    </p>

                    <p>
                        Check the technical report below for the
                        exact parser message and line number.
                    </p>

                </div>

            </div>
        `;
    }


    resultBox.innerHTML = `

        <div class="result-header error-header">

            <span>✗</span>

            <div>

                <h2>
                    ${resultTitle}
                </h2>

                <p>
                    ${resultDescription}
                </p>

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
                        ${statusText}
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">
                    #
                </div>

                <div>

                    <span>Total Errors</span>

                    <strong>
                        ${totalErrorCount}
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">
                    ${hasSyntaxErrors ? "✗" : "✓"}
                </div>

                <div>

                    <span>Syntax</span>

                    <strong>
                        ${syntaxStatus}
                    </strong>

                </div>

            </div>

        </div>


        ${syntaxCards}

        ${errorCards}


        <details class="technical-report">

            <summary>
                View Complete Technical Analysis Report
            </summary>

            <pre>${escapeHtml(result)}</pre>

        </details>
    `;
}


/* =====================================================
   DISPLAY SUCCESS RESULT
   ===================================================== */

function displaySuccessResult(result) {

    const resultBox = document.getElementById("resultBox");

    resultBox.innerHTML = `

        <div class="result-header success-header">

            <span>✓</span>

            <div>

                <h2>
                    Analysis Completed
                </h2>

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


/* =====================================================
   EXTRACT LOGICAL ERROR INFORMATION
   ===================================================== */

function extractErrors(result) {

    const errors = [];

    const lines = result.split("\n");

    let currentLine = null;

    let currentCondition = null;


    for (let i = 0; i < lines.length; i++) {

        const line = lines[i].trim();


        /*
         * Find line number and condition.
         *
         * Examples:
         *
         * Condition at line 5: x < 5
         * IF at line 5: x < 5
         * WHILE at line 7: b < 5
         * FOR at line 10: i < 5
         * DO-WHILE at line 15: x > 0
         */
        const lineMatch = line.match(
            /(?:Condition|IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+(\d+):\s*(.*)/i
        );


        if (lineMatch) {

            currentLine = lineMatch[1];

            currentCondition = lineMatch[2];
        }


        /*
         * Find logical error type.
         */
        if (line.startsWith("Error Type:")) {

            const errorType =
                line.substring("Error Type:".length).trim();

            let description = "";


            /*
             * Collect description lines.
             */
            for (let j = i + 1; j < lines.length; j++) {

                const nextLine = lines[j].trim();


                if (
                    nextLine.startsWith("Error Type:") ||
                    nextLine.startsWith("Logical Error Detected!") ||
                    nextLine.startsWith("NESTED IF ANALYSIS") ||
                    nextLine.startsWith("BRANCH ANALYSIS") ||
                    nextLine.startsWith("WHILE LOOP ANALYSIS") ||
                    nextLine.startsWith("DO-WHILE LOOP ANALYSIS") ||
                    nextLine.startsWith("FOR LOOP ANALYSIS") ||
                    nextLine.startsWith("LOOP CONTROL ANALYSIS") ||
                    nextLine.startsWith("INFINITE LOOP ANALYSIS") ||
                    nextLine.startsWith("COMPOUND CONDITION ANALYSIS")
                ) {
                    break;
                }


                if (nextLine.length > 0) {

                    if (description.length > 0) {
                        description += " ";
                    }

                    description += nextLine;
                }
            }


            errors.push({

                line: currentLine || "Unknown",

                type: errorType,

                condition: currentCondition || "",

                description: description

            });
        }
    }


    /*
     * Remove duplicate logical errors.
     */
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


/* =====================================================
   CLEAR CODE
   ===================================================== */

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


/* =====================================================
   PROTECT HTML
   ===================================================== */

function escapeHtml(text) {

    const div = document.createElement("div");

    div.textContent = text;

    return div.innerHTML;
}