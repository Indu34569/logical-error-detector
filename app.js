/* =========================================================
   JAVA CODE ANALYSIS
   ========================================================= */

async function analyzeCode() {

    const code =
        document.getElementById("codeInput").value;

    const resultBox =
        document.getElementById("resultBox");

    const button =
        document.getElementById("analyzeButton");


    /* -----------------------------------------------------
       CHECK EMPTY INPUT
       ----------------------------------------------------- */

    if (!code.trim()) {

        resultBox.innerHTML = `
            <div class="empty-result">

                <div class="empty-icon">⚠</div>

                <h3>Please enter Java code</h3>

                <p>
                    Paste a Java program into the code editor.
                </p>

            </div>
        `;

        return;
    }


    /* -----------------------------------------------------
       LOADING STATE
       ----------------------------------------------------- */

    button.disabled = true;
    button.textContent = "Analyzing...";


    resultBox.innerHTML = `
        <div class="loading-result">

            <div class="empty-icon">🔍</div>

            <h3>Analyzing Program...</h3>

            <p>
                JavaParser → Data Flow → Constraints → Z3 → Error Detection
            </p>

        </div>
    `;


    try {

        /* -------------------------------------------------
           SEND CODE TO BACKEND
           ------------------------------------------------- */

        const response =
            await fetch(
                "https://logical-error-detector-backend.onrender.com/analyze",
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "text/plain"
                    },

                    body: code
                }
            );


        const result =
            await response.text();


        if (!response.ok) {

            throw new Error(result);
        }


        /* -------------------------------------------------
           DETERMINE SYNTAX STATUS
           ------------------------------------------------- */

        const syntaxSectionMatch =
            result.match(
                /SYNTAX ANALYSIS\s*-+\s*([\s\S]*?)(?=\n\s*LOGICAL ERROR ANALYSIS)/i
            );


        const syntaxSection =
            syntaxSectionMatch
                ? syntaxSectionMatch[1]
                : "";


        /*
         * IMPORTANT:
         *
         * Only the syntax section is inspected.
         *
         * This prevents text inside the logical detector's
         * technical report from being mistaken for a syntax
         * error.
         */

        const noSyntaxErrors =
            /No syntax errors found\./i.test(
                syntaxSection
            );


        const syntaxErrors =
            extractSyntaxErrors(
                syntaxSection
            );


        const hasSyntaxErrors =
            !noSyntaxErrors &&
            syntaxErrors.length > 0;


        /*
         * If the backend explicitly says there are syntax
         * errors but does not provide a location, still show
         * one generic syntax error card.
         */

        const backendReportsSyntaxError =
            /Syntax errors detected!/i.test(
                syntaxSection
            ) ||
            /Syntax Error Details:/i.test(
                syntaxSection
            ) ||
            /Parse error/i.test(
                syntaxSection
            );


        const finalSyntaxStatus =
            hasSyntaxErrors ||
            (
                !noSyntaxErrors &&
                backendReportsSyntaxError
            );


        /* -------------------------------------------------
           EXTRACT LOGICAL ERRORS
           ------------------------------------------------- */

        const logicalErrors =
            extractLogicalErrors(
                result
            );


        const finalLogicalStatus =
            logicalErrors.length > 0;


        /* -------------------------------------------------
           DISPLAY
           ------------------------------------------------- */

        displayCombinedResult(
            result,
            finalSyntaxStatus,
            finalLogicalStatus
        );


    } catch (error) {

        resultBox.innerHTML = `
            <div class="error-result">

                <div class="result-header error-header">

                    <span>⚠</span>

                    <div>

                        <h2>
                            Backend Not Connected
                        </h2>

                        <p>
                            The analysis server could not be reached.
                        </p>

                    </div>

                </div>

                <p>
                    <b>
                        ${escapeHtml(error.message)}
                    </b>
                </p>

            </div>
        `;

    } finally {

        button.disabled = false;

        button.textContent =
            "Analyze Code";
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

    const resultBox =
        document.getElementById("resultBox");


    const syntaxErrors =
        extractSyntaxErrors(
            getSyntaxSection(result)
        );


    const logicalErrors =
        extractLogicalErrors(
            result
        );


    /*
     * -----------------------------------------------------
     * COUNTS
     * -----------------------------------------------------
     */

    let syntaxCount = 0;


    if (hasSyntaxErrors) {

        syntaxCount =
            syntaxErrors.length > 0
                ? syntaxErrors.length
                : 1;
    }


    const logicalCount =
        logicalErrors.length;


    const totalErrors =
        syntaxCount + logicalCount;


    /*
     * -----------------------------------------------------
     * SUCCESS
     * -----------------------------------------------------
     */

    if (
        !hasSyntaxErrors &&
        !hasLogicalErrors
    ) {

        displaySuccessResult(
            result
        );

        return;
    }


    /*
     * -----------------------------------------------------
     * TITLE + MESSAGE
     * -----------------------------------------------------
     */

    let title = "";

    let message = "";


    if (
        hasSyntaxErrors &&
        hasLogicalErrors
    ) {

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


    /* =====================================================
       SYNTAX SECTION
       ===================================================== */

    let syntaxSectionHTML = "";


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


        syntaxSectionHTML = `

            <div class="detected-errors">

                <h2>
                    Detected Syntax Errors
                </h2>

                ${syntaxCards}

            </div>

        `;
    }


    /* =====================================================
       LOGICAL SECTION
       ===================================================== */

    let logicalSectionHTML = "";


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


        logicalSectionHTML = `

            <div class="detected-errors">

                <h2>
                    Detected Logical Errors
                </h2>

                ${logicalCards}

            </div>

        `;
    }


    /* =====================================================
       STATUS TEXT
       ===================================================== */

    const syntaxStatus =
        hasSyntaxErrors
            ? "Syntax Errors Found"
            : "No Syntax Errors";


    const logicalStatus =
        hasLogicalErrors
            ? `${logicalCount} Logical Error${logicalCount === 1 ? "" : "s"} Found`
            : "No Logical Errors";


    /* =====================================================
       FINAL RESULT
       ===================================================== */

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


            <!-- TOTAL -->

            <div class="summary-card">

                <div class="summary-icon">
                    ✗
                </div>

                <div>

                    <span>
                        Status
                    </span>

                    <strong>
                        ${totalErrors}
                        Error${totalErrors === 1 ? "" : "s"} Found
                    </strong>

                </div>

            </div>


            <!-- SYNTAX -->

            <div class="summary-card">

                <div class="summary-icon">
                    ${syntaxCount}
                </div>

                <div>

                    <span>
                        Syntax
                    </span>

                    <strong>
                        ${syntaxStatus}
                    </strong>

                </div>

            </div>


            <!-- LOGICAL -->

            <div class="summary-card">

                <div class="summary-icon">
                    ${logicalCount}
                </div>

                <div>

                    <span>
                        Logical
                    </span>

                    <strong>
                        ${logicalStatus}
                    </strong>

                </div>

            </div>


        </div>


        ${syntaxSectionHTML}

        ${logicalSectionHTML}


        <details class="technical-report">

            <summary>
                View Complete Technical Analysis Report
            </summary>

            <pre>
${escapeHtml(result)}
            </pre>

        </details>

    `;
}


/* =========================================================
   GET SYNTAX SECTION
   ========================================================= */

function getSyntaxSection(result) {

    const match =
        result.match(
            /SYNTAX ANALYSIS\s*-+\s*([\s\S]*?)(?=\n\s*LOGICAL ERROR ANALYSIS)/i
        );


    if (match) {

        return match[1];
    }


    return "";
}


/* =========================================================
   EXTRACT SYNTAX ERRORS
   ========================================================= */

function extractSyntaxErrors(
    syntaxSection
) {

    const errors = [];


    if (!syntaxSection) {

        return errors;
    }


    const lines =
        syntaxSection.split("\n");


    for (
        let i = 0;
        i < lines.length;
        i++
    ) {

        const line =
            lines[i].trim();


        /*
         * Expected backend format:
         *
         * Line 12, Column 22: Parse error...
         */

        const match =
            line.match(
                /^Line\s+(\d+)\s*,\s*Column\s+(\d+)\s*:\s*(.*)$/i
            );


        if (!match) {

            continue;
        }


        const lineNumber =
            match[1];


        const columnNumber =
            match[2];


        let description =
            match[3].trim();


        if (!description) {

            description =
                "Invalid Java syntax.";
        }


        if (description.length > 500) {

            description =
                description.substring(
                    0,
                    500
                ) + "...";
        }


        errors.push({

            line:
                lineNumber,

            column:
                columnNumber,

            description:
                description

        });
    }


    /*
     * -----------------------------------------------------
     * REMOVE DUPLICATES
     * -----------------------------------------------------
     */

    return removeDuplicateSyntaxErrors(
        errors
    );
}


/* =========================================================
   REMOVE DUPLICATE SYNTAX ERRORS
   ========================================================= */

function removeDuplicateSyntaxErrors(
    errors
) {

    const unique = [];

    const seen =
        new Set();


    for (
        const error of errors
    ) {

        const key =
            error.line +
            "|" +
            error.column +
            "|" +
            error.description;


        if (!seen.has(key)) {

            seen.add(key);

            unique.push(error);
        }
    }


    return unique;
}


/* =========================================================
   EXTRACT LOGICAL ERRORS
   ========================================================= */

function extractLogicalErrors(
    result
) {

    const errors = [];


    const lines =
        result.split("\n");


    /*
     * IMPORTANT:
     *
     * Only parse blocks beginning with:
     *
     * Error Type:
     *
     * This prevents the summary text from being counted
     * as another error.
     */

    for (
        let i = 0;
        i < lines.length;
        i++
    ) {

        const current =
            lines[i].trim();


        if (
            !current.startsWith(
                "Error Type:"
            )
        ) {

            continue;
        }


        const type =
            current
                .substring(
                    "Error Type:".length
                )
                .trim();


        let lineNumber =
            "Unknown";


        let condition =
            "";


        let description =
            "";


        /*
         * -------------------------------------------------
         * READ ERROR BLOCK
         * -------------------------------------------------
         */

        for (
            let j = i + 1;
            j < Math.min(
                i + 20,
                lines.length
            );
            j++
        ) {

            const text =
                lines[j].trim();


            /*
             * LINE
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
             * CONDITION
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
             * NEW ERROR BLOCK
             */

            if (
                text.startsWith(
                    "Error Type:"
                )
            ) {

                break;
            }


            /*
             * IGNORE TECHNICAL LINES
             */

            if (
                text.startsWith(
                    "Known variable values:"
                ) ||

                text.startsWith(
                    "Generated Constraint:"
                ) ||

                text.startsWith(
                    "Z3 Result:"
                ) ||

                text.startsWith(
                    "IF at line"
                ) ||

                text.startsWith(
                    "WHILE at line"
                ) ||

                text.startsWith(
                    "FOR at line"
                ) ||

                text ===
                    "Logical Error Detected!"
            ) {

                continue;
            }


            if (
                text ===
                    "----------------------------------------"
                ||
                text === ""
            ) {

                continue;
            }


            /*
             * IGNORE HEADINGS
             */

            if (
                text.startsWith(
                    "LOGICAL ERROR ANALYSIS"
                ) ||

                text.startsWith(
                    "LOGICAL ANALYSIS"
                ) ||

                text.startsWith(
                    "BRANCH ANALYSIS"
                ) ||

                text.startsWith(
                    "NESTED IF ANALYSIS"
                ) ||

                text.startsWith(
                    "PROGRAM-ORDER DATA-FLOW ANALYSIS"
                ) ||

                text.startsWith(
                    "IF / ELSE / LOOP ANALYSIS"
                ) ||

                text.startsWith(
                    "STATIC ANALYSIS TOOL"
                ) ||

                text.startsWith(
                    "FINAL ANALYSIS REPORT"
                ) ||

                text.startsWith(
                    "File:"
                )
            ) {

                continue;
            }


            /*
             * DESCRIPTION
             */

            if (
                description.length === 0
            ) {

                description =
                    text;

            } else {

                description +=
                    " " + text;
            }
        }


        /*
         * -------------------------------------------------
         * FALLBACK FOR LINE / CONDITION
         * -------------------------------------------------
         */

        if (
            lineNumber ===
            "Unknown"
        ) {

            for (
                let k = i - 1;
                k >= Math.max(
                    0,
                    i - 20
                );
                k--
            ) {

                const previous =
                    lines[k].trim();


                const conditionAtLine =
                    previous.match(
                        /Condition\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (
                    conditionAtLine
                ) {

                    lineNumber =
                        conditionAtLine[1];


                    if (!condition) {

                        condition =
                            conditionAtLine[2]
                                .trim();
                    }


                    break;
                }


                const controlMatch =
                    previous.match(
                        /(?:IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+(\d+):\s*(.*)/i
                    );


                if (
                    controlMatch
                ) {

                    lineNumber =
                        controlMatch[1];


                    if (!condition) {

                        condition =
                            controlMatch[2]
                                .trim();
                    }


                    break;
                }
            }
        }


        /*
         * -------------------------------------------------
         * DESCRIPTION FALLBACK
         * -------------------------------------------------
         */

        if (
            !description
        ) {

            description =
                "Logical problem detected for this condition.";
        }


        errors.push({

            line:
                lineNumber,

            type:
                type,

            condition:
                condition,

            description:
                description

        });
    }


    /*
     * -----------------------------------------------------
     * REMOVE DUPLICATES
     * -----------------------------------------------------
     */

    return removeDuplicateLogicalErrors(
        errors
    );
}


/* =========================================================
   REMOVE DUPLICATE LOGICAL ERRORS
   ========================================================= */

function removeDuplicateLogicalErrors(
    errors
) {

    const unique = [];

    const seen =
        new Set();


    for (
        const error of errors
    ) {

        const key =
            error.line +
            "|" +
            error.type +
            "|" +
            error.condition;


        if (!seen.has(key)) {

            seen.add(key);

            unique.push(error);
        }
    }


    return unique;
}


/* =========================================================
   SUCCESS RESULT
   ========================================================= */

function displaySuccessResult(
    result
) {

    const resultBox =
        document.getElementById(
            "resultBox"
        );


    resultBox.innerHTML = `

        <div class="result-header success-header">

            <span>
                ✓
            </span>

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

                    <span>
                        Status
                    </span>

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

                    <span>
                        Errors
                    </span>

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

                    <span>
                        Syntax
                    </span>

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

            <pre>
${escapeHtml(result)}
            </pre>

        </details>

    `;
}


/* =========================================================
   CLEAR CODE
   ========================================================= */

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


/* =========================================================
   HTML ESCAPE
   ========================================================= */

function escapeHtml(
    text
) {

    const div =
        document.createElement(
            "div"
        );


    div.textContent =
        text == null
            ? ""
            : String(text);


    return div.innerHTML;
}