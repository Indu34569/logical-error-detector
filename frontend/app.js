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

        const hasSyntaxErrors =
    	   /SYNTAX ANALYSIS[\s\S]*?Syntax errors detected!/i.test(result) &&
    	   !/SYNTAX ANALYSIS[\s\S]*?✓\s*No syntax errors found/i.test(result);

        const hasLogicalErrors =
            /logical\s+errors?\s+detected/i.test(result) ||
            /logical\s+error\s+detected!/i.test(result) ||
            /error\s+type:/i.test(result);

        /*
         * IMPORTANT:
         * Do NOT return immediately when syntax errors exist.
         *
         * The backend is now capable of detecting both syntax
         * and logical errors.
         */
        if (hasSyntaxErrors || hasLogicalErrors) {
            displayCombinedResult(
                result,
                hasSyntaxErrors,
                hasLogicalErrors
            );
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

                        <p>
                            The analysis server could not be reached.
                        </p>
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


/* =====================================================
   DISPLAY COMBINED RESULT
   ===================================================== */

function displayCombinedResult(
    result,
    hasSyntaxErrors,
    hasLogicalErrors
) {

    const resultBox =
        document.getElementById("resultBox");

    const syntaxErrors =
        extractSyntaxErrors(result);

    const logicalErrors =
        extractErrors(result);

    const syntaxCount =
        syntaxErrors.length > 0
            ? syntaxErrors.length
            : hasSyntaxErrors
                ? countSyntaxErrors(result)
                : 0;

    const logicalCount =
        logicalErrors.length;

    let totalErrors =
        syntaxCount + logicalCount;


    /* =================================================
       STATUS
       ================================================= */

    let title = "";
    let message = "";

    if (hasSyntaxErrors && hasLogicalErrors) {

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


    /* =================================================
       SYNTAX CARDS
       ================================================= */

    let syntaxSection = "";

    if (hasSyntaxErrors) {

        let syntaxCards = "";

        if (syntaxErrors.length > 0) {

            syntaxCards = syntaxErrors.map(
                (error, index) => {

                    return `
                        <div class="detected-error-card">

                            <div class="error-title">

                                <span class="error-badge">
                                    ERROR ${index + 1}
                                </span>

                                <h3>
                                    Syntax Error
                                </h3>

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
                }
            ).join("");

        } else {

            syntaxCards = `
                <div class="detected-error-card">

                    <div class="error-title">

                        <span class="error-badge">
                            ERROR
                        </span>

                        <h3>
                            Syntax Error
                        </h3>

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


    /* =================================================
       LOGICAL ERROR CARDS
       ================================================= */

    let logicalSection = "";

    if (hasLogicalErrors) {

        let logicalCards = "";

        if (logicalErrors.length > 0) {

            logicalCards =
                logicalErrors.map(
                    (error, index) => {

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
                                                    ${escapeHtml(
                                                        error.condition
                                                    )}
                                                </code>
                                            </p>
                                        `
                                        : ""
                                }

                            </div>
                        `;
                    }
                ).join("");

        } else {

            logicalCards = `
                <div class="detected-error-card">

                    <div class="error-title">

                        <span class="error-badge">
                            ERROR
                        </span>

                        <h3>
                            Logical Error
                        </h3>

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


    /* =================================================
       SUMMARY
       ================================================= */

    let syntaxStatus =
        hasSyntaxErrors
            ? "Syntax Errors Found"
            : "No Syntax Errors";


    let logicalStatus =
        hasLogicalErrors
            ? "Logical Errors Found"
            : "No Logical Errors";


    resultBox.innerHTML = `

        <div class="result-header error-header">

            <span>✗</span>

            <div>

                <h2>
                    ${title}
                </h2>

                <p>
                    ${message}
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
                        ${totalErrors} Error${totalErrors === 1 ? "" : "s"} Found
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


/* =====================================================
   EXTRACT SYNTAX ERRORS
   ===================================================== */

function extractSyntaxErrors(result) {

    const errors = [];

    const lines = result.split("\n");

    for (let i = 0; i < lines.length; i++) {

        const currentLine =
            lines[i].trim();

        const locationMatch =
            currentLine.match(
                /\(line\s+(\d+)\s*,\s*col\s+(\d+)\)\s*(.*)/i
            );

        if (!locationMatch) {
            continue;
        }

        const lineNumber =
            locationMatch[1];

        const columnNumber =
            locationMatch[2];

        let description =
            locationMatch[3].trim();


        if (!description) {
            description = "Invalid Java syntax.";
        }


        /*
         * Only use the parse-error line.
         * Do NOT consume the entire stack trace.
         */

        if (description.length > 500) {

            description =
                description.substring(0, 500)
                + "...";
        }


        errors.push({

            line: lineNumber,

            column: columnNumber,

            description: description

        });
    }


    /*
     * Remove duplicate syntax errors.
     */

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


/* =====================================================
   COUNT SYNTAX ERRORS
   ===================================================== */

function countSyntaxErrors(result) {

    const matches =
        result.match(
            /\(line\s+\d+\s*,\s*col\s+\d+\)/gi
        );

    if (matches && matches.length > 0) {
        return matches.length;
    }

    return 1;
}


/* =====================================================
   EXTRACT LOGICAL ERRORS
   ===================================================== */

function extractErrors(result) {

    const errors = [];

    const lines = result.split("\n");


    /*
     * We specifically read:
     *
     * Error Type:
     * Line:
     * Condition:
     *
     * This prevents unrelated technical-report text
     * from becoming an error card.
     */

    for (let i = 0; i < lines.length; i++) {

        const current =
            lines[i].trim();


        if (!current.startsWith("Error Type:")) {
            continue;
        }


        const type =
            current
                .substring("Error Type:".length)
                .trim();


        let lineNumber =
            "Unknown";

        let condition =
            "";

        let description =
            "";


        /*
         * Look around the current error block.
         */

        for (
            let j = i + 1;
            j < Math.min(i + 12, lines.length);
            j++
        ) {

            const text =
                lines[j].trim();


            /*
             * Line: 7
             */

            const lineMatch =
                text.match(
                    /^Line:\s*(\d+)/i
                );

            if (lineMatch) {

                lineNumber =
                    lineMatch[1];

                continue;
            }


            /*
             * Condition: age < 10
             */

            const conditionMatch =
                text.match(
                    /^Condition:\s*(.*)/i
                );

            if (conditionMatch) {

                condition =
                    conditionMatch[1].trim();

                continue;
            }


            /*
             * Stop at the next logical error.
             */

            if (
                text.startsWith("Error Type:")
                ||
                text.startsWith("Logical Error Detected!")
            ) {

                break;
            }


            /*
             * Ignore technical noise.
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
             * Add useful description.
             */

            if (
                !text.startsWith("LOGICAL ERROR ANALYSIS")
                &&
                !text.startsWith("BRANCH ANALYSIS")
                &&
                !text.startsWith("NESTED IF ANALYSIS")
            ) {

                if (description.length === 0) {

                    description = text;

                } else {

                    description +=
                        " " + text;
                }
            }
        }


        /*
         * If backend did not explicitly print Line:,
         * find the nearest "Condition at line" above it.
         */

        if (lineNumber === "Unknown") {

            for (
                let k = i - 1;
                k >= Math.max(0, i - 15);
                k--
            ) {

                const previous =
                    lines[k].trim();


                const match =
                    previous.match(
                        /Condition\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (match) {

                    lineNumber =
                        match[1];

                    if (!condition) {

                        condition =
                            match[2].trim();
                    }

                    break;
                }


                const ifMatch =
                    previous.match(
                        /(?:IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (ifMatch) {

                    lineNumber =
                        ifMatch[1];

                    if (!condition) {

                        condition =
                            ifMatch[2].trim();
                    }

                    break;
                }
            }
        }


        /*
         * Avoid empty descriptions.
         */

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


    /*
     * Remove duplicates.
     *
     * Same line + same type + same condition
     * = same logical error.
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
   DISPLAY SUCCESS RESULT
   ===================================================== */

function displaySuccessResult(result) {

    const resultBox =
        document.getElementById("resultBox");

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
   CLEAR CODE
   ===================================================== */

function clearCode() {

    document.getElementById(
        "codeInput"
    ).value = "";

    document.getElementById(
        "resultBox"
    ).innerHTML = `

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
   HTML ESCAPE
   ===================================================== */

function escapeHtml(text) {

    const div =
        document.createElement("div");

    div.textContent =
        text == null
            ? ""
            : String(text);

    return div.innerHTML;
}