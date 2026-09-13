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
         * IMPORTANT:
         * Check syntax errors FIRST.
         * The backend may successfully return a report even
         * when JavaParser finds syntax errors.
         */
        const hasSyntaxErrors =
            /syntax\s+errors?\s+detected/i.test(result) ||
            /parse\s+error/i.test(result) ||
            /parsing\s+failed/i.test(result) ||
            /javaparser.*exception/i.test(result);

        if (hasSyntaxErrors) {
            displaySyntaxResult(result);
            return;
        }

        /*
         * Check logical errors only after syntax checking.
         */
        const hasLogicalErrors =
            /logical\s+errors?\s+detected/i.test(result) ||
            /logical\s+error\s+detected!/i.test(result) ||
            /error\s+type:/i.test(result);

        if (hasLogicalErrors) {
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
   DISPLAY SYNTAX ERROR RESULT
   ===================================================== */

function displaySyntaxResult(result) {

    const resultBox = document.getElementById("resultBox");

    const syntaxErrors = extractSyntaxErrors(result);

    const errorCount = syntaxErrors.length > 0
        ? syntaxErrors.length
        : countSyntaxErrors(result);

    let syntaxCards = "";

    if (syntaxErrors.length > 0) {

        syntaxCards = `
            <div class="detected-errors">

                <h2>Detected Syntax Errors</h2>

                ${syntaxErrors.map((error, index) => `
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
                `).join("")}

            </div>
        `;

    } else {

        syntaxCards = `
            <div class="detected-errors">

                <h2>Detected Syntax Errors</h2>

                <div class="detected-error-card">

                    <div class="error-title">

                        <span class="error-badge">
                            ${errorCount} ERROR${errorCount === 1 ? "" : "S"}
                        </span>

                        <h3>Java Syntax Error</h3>

                    </div>

                    <p>
                        The Java source code contains a syntax or parsing error.
                    </p>

                </div>

            </div>
        `;
    }


    resultBox.innerHTML = `

        <div class="result-header error-header">

            <span>✗</span>

            <div>

                <h2>Syntax Errors Detected</h2>

                <p>
                    The analyzer could not completely analyze the program
                    because the Java code contains syntax errors.
                </p>

            </div>

        </div>


        <div class="analysis-summary">

            <div class="summary-card">

                <div class="summary-icon">✗</div>

                <div>

                    <span>Status</span>

                    <strong>Syntax Errors Found</strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">
                    ${errorCount}
                </div>

                <div>

                    <span>Errors</span>

                    <strong>
                        ${errorCount}
                        Syntax Error${errorCount === 1 ? "" : "s"}
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">✗</div>

                <div>

                    <span>Syntax</span>

                    <strong>Syntax Errors Found</strong>

                </div>

            </div>

        </div>


        ${syntaxCards}


        <details class="technical-report" open>

            <summary>
                View Complete Technical Analysis Report
            </summary>

            <pre>${escapeHtml(result)}</pre>

        </details>

    `;
}


/* =====================================================
   DISPLAY LOGICAL ERROR RESULT
   ===================================================== */

function displayErrorResult(result) {

    const resultBox = document.getElementById("resultBox");

    const errors = extractErrors(result);

    let errorCards = "";

    if (errors.length > 0) {

        errorCards = `
            <div class="detected-errors">

                <h2>Detected Logical Errors</h2>

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

                `).join("")}

            </div>
        `;

    } else {

        errorCards = `
            <div class="detected-errors">

                <h2>Logical Errors Detected</h2>

                <div class="detected-error-card">

                    <div class="error-title">

                        <span class="error-badge">
                            ERROR
                        </span>

                        <h3>Logical Error</h3>

                    </div>

                    <p>
                        The analyzer detected a logical problem
                        in the Java program.
                    </p>

                </div>

            </div>
        `;
    }


    const syntaxStatus =
        /no\s+syntax\s+errors?\s+found/i.test(result)
        ? "No Syntax Errors"
        : "Check Report";


    resultBox.innerHTML = `

        <div class="result-header error-header">

            <span>✗</span>

            <div>

                <h2>Logical Errors Detected</h2>

                <p>
                    The analyzer found logical problems in the Java program.
                </p>

            </div>

        </div>


        <div class="analysis-summary">

            <div class="summary-card">

                <div class="summary-icon">✗</div>

                <div>

                    <span>Status</span>

                    <strong>
                        Logical Errors Found
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">
                    ${errors.length}
                </div>

                <div>

                    <span>Total Errors</span>

                    <strong>
                        ${errors.length}
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">✓</div>

                <div>

                    <span>Syntax</span>

                    <strong>
                        ${syntaxStatus}
                    </strong>

                </div>

            </div>

        </div>


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

                <h2>Analysis Completed</h2>

                <p>
                    No syntax or logical errors were detected.
                </p>

            </div>

        </div>


        <div class="analysis-summary">

            <div class="summary-card">

                <div class="summary-icon">✓</div>

                <div>

                    <span>Status</span>

                    <strong>
                        No Errors Found
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">0</div>

                <div>

                    <span>Errors</span>

                    <strong>
                        0 Errors
                    </strong>

                </div>

            </div>


            <div class="summary-card">

                <div class="summary-icon">✓</div>

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
   EXTRACT SYNTAX ERROR INFORMATION
   ===================================================== */

function extractSyntaxErrors(result) {

    const errors = [];

    const lines = result.split("\n");

    for (let i = 0; i < lines.length; i++) {

        const currentLine = lines[i].trim();

        /*
         * Example backend output:
         *
         * (line 3,col 17) Parse error.
         */

        const locationMatch = currentLine.match(
            /\(line\s+(\d+)\s*,\s*col\s+(\d+)\)\s*(.*)/i
        );

        if (locationMatch) {

            const lineNumber = locationMatch[1];
            const columnNumber = locationMatch[2];

            let description = locationMatch[3].trim();

            /*
             * If the next lines contain useful parse information,
             * include them in the description.
             */
            for (
                let j = i + 1;
                j < Math.min(i + 4, lines.length);
                j++
            ) {

                const nextLine = lines[j].trim();

                if (!nextLine) {
                    continue;
                }

                if (
                    nextLine.startsWith("Problem stacktrace") ||
                    nextLine.startsWith("com.github.javaparser")
                ) {
                    break;
                }

                if (
                    nextLine.startsWith("Syntax Error Details") ||
                    nextLine.startsWith("SYNTAX ANALYSIS")
                ) {
                    continue;
                }

                if (description.length > 0) {
                    description += " ";
                }

                description += nextLine;
            }


            errors.push({

                line: lineNumber,

                column: columnNumber,

                description:
                    description || "Invalid Java syntax."

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

    const matches = result.match(
        /\(line\s+\d+\s*,\s*col\s+\d+\)/gi
    );

    if (matches && matches.length > 0) {
        return matches.length;
    }

    return 1;
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
         * FOR at line 8: i < 5
         */

        const lineMatch = line.match(
            /(?:Condition|IF|WHILE|FOR|DO-WHILE)\s+at\s+line\s+(\d+):\s*(.*)/i
        );


        if (lineMatch) {

            currentLine = lineMatch[1];

            currentCondition = lineMatch[2];

        }


        /*
         * Find error type.
         */

        if (line.startsWith("Error Type:")) {

            const errorType =
                line.substring("Error Type:".length).trim();

            let description = "";


            /*
             * Read the lines after Error Type.
             */

            for (
                let j = i + 1;
                j < lines.length;
                j++
            ) {

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

            <div class="empty-icon">🔍</div>

            <h3>Ready for Analysis</h3>

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