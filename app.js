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


        /* =================================================
           DETECT SYNTAX AND LOGICAL ERRORS
           ================================================= */

        const syntaxDetected =
            /syntax\s+errors?\s+detected/i.test(result) ||
            /syntax\s+error\s+details/i.test(result) ||
            /parse\s+error/i.test(result) ||
            /parsing\s+failed/i.test(result) ||
            /javaparser.*exception/i.test(result) ||
            /syntax\s+errors?\s+were\s+detected/i.test(result);


        const logicalDetected =
            /logical\s+errors?\s+detected/i.test(result) ||
            /logical\s+error\s+detected!/i.test(result) ||
            /error\s+type:/i.test(result);


        /*
         * IMPORTANT:
         *
         * Always use the combined display when either
         * syntax or logical errors are detected.
         */

        if (syntaxDetected || logicalDetected) {

            displayCombinedResult(
                result,
                syntaxDetected,
                logicalDetected
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


    const totalErrors =
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

    }

    else if (hasSyntaxErrors) {

        title = "Syntax Errors Detected";

        message =
            "The analyzer found syntax errors in the Java program.";

    }

    else {

        title = "Logical Errors Detected";

        message =
            "The analyzer found logical problems in the Java program.";

    }


    /* =================================================
       SYNTAX SECTION
       ================================================= */

    let syntaxSection = "";


    if (hasSyntaxErrors) {

        let syntaxCards = "";


        if (syntaxErrors.length > 0) {

            syntaxCards =
                syntaxErrors.map(
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

        }

        else {

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

                <h2>
                    Detected Syntax Errors
                </h2>

                ${syntaxCards}

            </div>

        `;

    }


    /* =================================================
       LOGICAL ERROR SECTION
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

        }

        else {

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

                <h2>
                    Detected Logical Errors
                </h2>

                ${logicalCards}

            </div>

        `;

    }


    /* =================================================
       SUMMARY
       ================================================= */

    const syntaxStatus =
        hasSyntaxErrors
            ? "Syntax Errors Found"
            : "No Syntax Errors";


    const logicalStatus =
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


/* =====================================================
   EXTRACT SYNTAX ERRORS
   ===================================================== */

function extractSyntaxErrors(result) {

    const errors = [];

    const lines = result.split("\n");


    for (let i = 0; i < lines.length; i++) {

        const currentLine =
            lines[i].trim();


        /*
         * Format 1:
         *
         * (line 11,col 21) Parse error.
         */

        let match =
            currentLine.match(
                /\(line\s+(\d+)\s*,\s*col\s+(\d+)\)\s*(.*)/i
            );


        if (match) {

            let description =
                match[3].trim();


            /*
             * Remove stack trace from description.
             */

            const stackIndex =
                description.search(
                    /Problem stacktrace/i
                );


            if (stackIndex !== -1) {

                description =
                    description.substring(
                        0,
                        stackIndex
                    ).trim();

            }


            if (!description) {

                description =
                    "Invalid Java syntax.";

            }


            errors.push({

                line: match[1],

                column: match[2],

                description: description

            });


            continue;
        }


        /*
         * Format 2:
         *
         * Line 11, Column 21: Parse error.
         */

        match =
            currentLine.match(
                /^Line\s+(\d+)\s*,\s*Column\s+(\d+)\s*:\s*(.*)$/i
            );


        if (match) {

            let description =
                match[3].trim();


            const stackIndex =
                description.search(
                    /Problem stacktrace/i
                );


            if (stackIndex !== -1) {

                description =
                    description.substring(
                        0,
                        stackIndex
                    ).trim();

            }


            if (!description) {

                description =
                    "Invalid Java syntax.";

            }


            errors.push({

                line: match[1],

                column: match[2],

                description: description

            });

        }

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

    const parenthesisMatches =
        result.match(
            /\(line\s+\d+\s*,\s*col\s+\d+\)/gi
        );


    const lineColumnMatches =
        result.match(
            /Line\s+\d+\s*,\s*Column\s+\d+/gi
        );


    const count =
        (parenthesisMatches
            ? parenthesisMatches.length
            : 0)
        +
        (lineColumnMatches
            ? lineColumnMatches.length
            : 0);


    return count > 0
        ? count
        : 1;
}


/* =====================================================
   EXTRACT LOGICAL ERRORS
   ===================================================== */

function extractErrors(result) {

    const errors = [];

    const lines =
        result.split("\n");


    for (let i = 0; i < lines.length; i++) {

        const current =
            lines[i].trim();


        /*
         * Only start an error card at:
         *
         * Error Type:
         */

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
         * Read only this error block.
         */

        for (
            let j = i + 1;
            j < Math.min(i + 10, lines.length);
            j++
        ) {

            const text =
                lines[j].trim();


            /*
             * Stop at another error.
             */

            if (
                text.startsWith("Error Type:")
                ||
                text.startsWith("Logical Error Detected!")
            ) {

                break;
            }


            /*
             * Stop at a new analysis section.
             */

            if (
                text.startsWith("IF / ELSE")
                ||
                text.startsWith("IF / ELSE / LOOP")
                ||
                text.startsWith("WHILE LOOP")
                ||
                text.startsWith("DO-WHILE LOOP")
                ||
                text.startsWith("FOR LOOP")
                ||
                text.startsWith("BRANCH ANALYSIS")
                ||
                text.startsWith("NESTED IF ANALYSIS")
                ||
                text.startsWith("FINAL ANALYSIS REPORT")
                ||
                text.startsWith("========================================")
            ) {

                break;
            }


            /*
             * Line: 8
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
             * Add description.
             */

            if (!description) {

                description =
                    text;

            }

        }


        /*
         * If Line: was not printed,
         * find the nearest condition above.
         */

        if (lineNumber === "Unknown") {

            for (
                let k = i - 1;
                k >= Math.max(0, i - 15);
                k--
            ) {

                const previous =
                    lines[k].trim();


                const conditionMatch =
                    previous.match(
                        /Condition\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (conditionMatch) {

                    lineNumber =
                        conditionMatch[1];


                    if (!condition) {

                        condition =
                            conditionMatch[2].trim();

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