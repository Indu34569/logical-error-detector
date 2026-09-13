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

        /*
         * =====================================================
         * DETERMINE SYNTAX STATUS
         * =====================================================
         *
         * IMPORTANT:
         * Do NOT simply search for the words
         * "syntax errors" because the backend can also say:
         *
         * "No syntax errors found."
         *
         * Instead, explicitly check whether the syntax section
         * reports an actual error.
         */

        const syntaxSectionMatch =
            result.match(
                /SYNTAX ANALYSIS\s*-+\s*([\s\S]*?)(?=\n\s*LOGICAL ERROR ANALYSIS)/i
            );

        const syntaxSection =
            syntaxSectionMatch
                ? syntaxSectionMatch[1]
                : result;


        const hasSyntaxErrors =
            /Syntax errors detected!/i.test(syntaxSection) ||
            /Syntax Error Details:/i.test(syntaxSection) ||
            /Parse error/i.test(syntaxSection);


        /*
         * Explicitly override the status when the backend
         * confirms that there are no syntax errors.
         */

        const backendSaysNoSyntaxErrors =
            /No syntax errors found\./i.test(syntaxSection);


        const finalSyntaxStatus =
            backendSaysNoSyntaxErrors
                ? false
                : hasSyntaxErrors;


        /*
         * =====================================================
         * DETERMINE LOGICAL STATUS
         * =====================================================
         */

        const logicalDetected =
            /Logical Error Detected!/i.test(result) ||
            /Logical errors detected\./i.test(result) ||
            /Error Type:/i.test(result);


        /*
         * =====================================================
         * DISPLAY RESULT
         * =====================================================
         */

        const logicalErrors =
            extractErrors(result);


        const finalLogicalStatus =
            logicalDetected &&
            logicalErrors.length > 0;


        /*
         * If the backend says logical errors exist but our
         * structured extraction could not find them, still
         * allow the fallback logical card.
         */

        displayCombinedResult(
            result,
            finalSyntaxStatus,
            logicalDetected
        );

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


    /*
     * =================================================
     * COUNTS
     * =================================================
     */

    const syntaxCount =
        hasSyntaxErrors
            ? (
                syntaxErrors.length > 0
                    ? syntaxErrors.length
                    : countSyntaxErrors(result)
            )
            : 0;


    const logicalCount =
        logicalErrors.length;


    const totalErrors =
        syntaxCount + logicalCount;


    /*
     * =================================================
     * SUCCESS RESULT
     * =================================================
     */

    if (!hasSyntaxErrors && logicalCount === 0) {

        displaySuccessResult(result);

        return;
    }


    /*
     * =================================================
     * STATUS
     * =================================================
     */

    let title = "";
    let message = "";


    if (hasSyntaxErrors && logicalCount > 0) {

        title =
            "Syntax & Logical Errors Detected";

        message =
            "The analyzer found both syntax and logical problems in the Java program.";

    } else if (hasSyntaxErrors) {

        title =
            "Syntax Errors Detected";

        message =
            "The analyzer found syntax errors in the Java program.";

    } else {

        title =
            "Logical Errors Detected";

        message =
            "The analyzer found logical problems in the Java program.";
    }


    /*
     * =================================================
     * SYNTAX CARDS
     * =================================================
     */

    let syntaxSection = "";


    if (hasSyntaxErrors) {

        let syntaxCards = "";


        if (syntaxErrors.length > 0) {

            syntaxCards =
                syntaxErrors
                    .map(
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
                    )
                    .join("");

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


    /*
     * =================================================
     * LOGICAL ERROR CARDS
     * =================================================
     */

    let logicalSection = "";


    if (hasLogicalErrors) {

        let logicalCards = "";


        if (logicalErrors.length > 0) {

            logicalCards =
                logicalErrors
                    .map(
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
                    )
                    .join("");

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


    /*
     * =================================================
     * SUMMARY STATUS
     * =================================================
     */

    const syntaxStatus =
        hasSyntaxErrors
            ? "Syntax Errors Found"
            : "No Syntax Errors";


    const logicalStatus =
        logicalCount > 0
            ? "Logical Errors Found"
            : "No Logical Errors";


    /*
     * =================================================
     * FINAL HTML
     * =================================================
     */

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

    const lines =
        result.split("\n");


    for (let i = 0; i < lines.length; i++) {

        const currentLine =
            lines[i].trim();


        /*
         * Backend format:
         *
         * Line 11, Column 21: Parse error...
         */

        const locationMatch =
            currentLine.match(
                /^Line\s+(\d+)\s*,\s*Column\s+(\d+)\s*:\s*(.*)$/i
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

            description =
                "Invalid Java syntax.";
        }


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
            /^Line\s+\d+\s*,\s*Column\s+\d+\s*:/gim
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

    const lines =
        result.split("\n");


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
         * Read the current logical error block.
         */

        for (
            let j = i + 1;
            j < Math.min(i + 15, lines.length);
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
             * Stop at next logical error.
             */

            if (
                text.startsWith("Error Type:")
                ||
                text.startsWith("Logical Error Detected!")
            ) {

                break;
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
             * Ignore technical headings.
             */

            if (
                text.startsWith("LOGICAL ERROR ANALYSIS")
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
                ||
                text.startsWith("FINAL ANALYSIS REPORT")
            ) {

                continue;
            }


            /*
             * Add useful description.
             */

            if (description.length === 0) {

                description =
                    text;

            } else {

                description +=
                    " " + text;
            }
        }


        /*
         * Fallback: find the condition line above.
         */

        if (lineNumber === "Unknown") {

            for (
                let k = i - 1;
                k >= Math.max(0, i - 20);
                k--
            ) {

                const previous =
                    lines[k].trim();


                const conditionAtLine =
                    previous.match(
                        /Condition\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (conditionAtLine) {

                    lineNumber =
                        conditionAtLine[1];


                    if (!condition) {

                        condition =
                            conditionAtLine[2].trim();
                    }


                    break;
                }


                const controlMatch =
                    previous.match(
                        /(?:IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (controlMatch) {

                    lineNumber =
                        controlMatch[1];


                    if (!condition) {

                        condition =
                            controlMatch[2].trim();
                    }


                    break;
                }
            }
        }


        /*
         * Avoid empty description.
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