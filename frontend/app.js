const tabs = document.querySelectorAll(".tab");
const pages = document.querySelectorAll(".page");

let currentAnalysis = {
    code: "",
    lines: 0,
    errors: [],
    syntaxErrors: [],
    warnings: 0,
    time: "--",
    timestamp: null
};

/* =========================================================
   TAB NAVIGATION
========================================================= */

tabs.forEach(tab => {
    tab.addEventListener("click", () => {
        switchTab(tab.dataset.tab);
    });
});

function switchTab(tabName) {
    tabs.forEach(tab => {
        tab.classList.toggle(
            "active",
            tab.dataset.tab === tabName
        );
    });

    pages.forEach(page => {
        page.classList.toggle(
            "active-page",
            page.id === tabName
        );
    });

    if (tabName === "history") {
        displayHistory();
    }

    if (tabName === "report") {
        updateReport();
    }
}

/* =========================================================
   CODE INPUT
========================================================= */

function updateLineNumbers() {
    const textarea = document.getElementById("codeInput");
    const lineNumbers = document.getElementById("lineNumbers");
    const lineCount = document.getElementById("lineCount");

    if (!textarea) {
        return;
    }

    const code = textarea.value;
    const lines = code === "" ? 1 : code.split("\n").length;

    if (lineNumbers) {
        lineNumbers.textContent = Array.from(
            { length: lines },
            (_, i) => i + 1
        ).join("\n");
    }

    if (lineCount) {
        lineCount.textContent =
            `${code === "" ? 0 : lines} ${lines === 1 ? "line" : "lines"}`;
    }
}

/* =========================================================
   EXAMPLE PROGRAM
========================================================= */

function loadExample() {
    const textarea = document.getElementById("codeInput");

    if (!textarea) {
        return;
    }

    const example = `public class TestProgram {

public static void main(String[] args) {

    int x = 5;

    if (x > 10 && x < 3) {
        System.out.println("This condition is impossible");
    }

}

}`;

    textarea.value = example;

    updateLineNumbers();
}

/* =========================================================
   MAIN ANALYSIS
========================================================= */

function startAnalysis() {
    const textarea = document.getElementById("codeInput");

    if (!textarea) {
        return;
    }

    const code = textarea.value.trim();

    if (!code) {
        alert("Please enter or paste Java code first.");
        return;
    }

    const startTime = performance.now();

    currentAnalysis.code = code;
    currentAnalysis.lines = code.split("\n").length;
    currentAnalysis.timestamp = new Date();

    /* FIRST: SYNTAX CHECK */

    const syntaxErrors = detectSyntaxErrors(code);

    currentAnalysis.syntaxErrors = syntaxErrors;

    /* STOP LOGICAL ANALYSIS IF SYNTAX IS INVALID */

    if (syntaxErrors.length > 0) {
        currentAnalysis.errors = syntaxErrors;

        const elapsed = performance.now() - startTime;

        currentAnalysis.time =
            `${elapsed.toFixed(2)} ms`;

        updateDashboard();
        updateErrors();
        updateReport();

        saveHistory();

        switchTab("flow");

        runSyntaxErrorAnimation(syntaxErrors);

        return;
    }

    /* SECOND: LOGICAL ANALYSIS */

    const errors = detectLogicalErrors(code);

    currentAnalysis.errors = errors;

    const elapsed = performance.now() - startTime;

    currentAnalysis.time =
        elapsed < 1
            ? `${elapsed.toFixed(2)} ms`
            : `${elapsed.toFixed(1)} ms`;

    updateDashboard();
    updateErrors();
    updateReport();

    saveHistory();

    switchTab("flow");

    runAnalysisAnimation(errors);
    animateAstClassification(code, errors);
}

/* =========================================================
   DASHBOARD
========================================================= */

function updateDashboard() {
    setText(
        "dashboardLines",
        currentAnalysis.lines
    );

    setText(
        "dashboardErrors",
        currentAnalysis.errors.length
    );

    setText(
        "dashboardTime",
        currentAnalysis.time
    );
}

/* =========================================================
   BASIC JAVA SYNTAX CHECKING
========================================================= */

function detectSyntaxErrors(code) {
    const errors = [];
    const lines = code.split("\n");

    let braceBalance = 0;
    let parenthesisBalance = 0;
    let insideString = false;

    lines.forEach((line, index) => {
        const trimmed = line.trim();

        for (let i = 0; i < line.length; i++) {
            const character = line[i];
            const previous = line[i - 1];

            if (
                character === '"' &&
                previous !== "\\"
            ) {
                insideString = !insideString;
            }

            if (!insideString) {
                if (character === "{") {
                    braceBalance++;
                }

                if (character === "}") {
                    braceBalance--;

                    if (braceBalance < 0) {
                        errors.push({
                            line: index + 1,
                            type: "Syntax Error",
                            code: trimmed,
                            explanation:
                                "A closing brace appears without a matching opening brace."
                        });

                        braceBalance = 0;
                    }
                }

                if (character === "(") {
                    parenthesisBalance++;
                }

                if (character === ")") {
                    parenthesisBalance--;

                    if (parenthesisBalance < 0) {
                        errors.push({
                            line: index + 1,
                            type: "Syntax Error",
                            code: trimmed,
                            explanation:
                                "A closing parenthesis appears without a matching opening parenthesis."
                        });

                        parenthesisBalance = 0;
                    }
                }
            }
        }

        /* Missing closing parenthesis after if/while */

        if (
            /\b(if|while|for)\s*\(/.test(trimmed) &&
            !trimmed.includes(")")
        ) {
            errors.push({
                line: index + 1,
                type: "Syntax Error",
                code: trimmed,
                explanation:
                    "The control statement is missing a closing parenthesis."
            });
        }

        /* Basic incomplete declaration */

        if (
            /\bint\s+[A-Za-z_$][\w$]*\s*=/.test(trimmed) &&
            !trimmed.endsWith(";") &&
            !trimmed.endsWith("{")
        ) {
            errors.push({
                line: index + 1,
                type: "Syntax Error",
                code: trimmed,
                explanation:
                    "The variable declaration appears to be missing a semicolon."
            });
        }

        /* Basic incomplete if statement */

        if (
            /^if\s*\(/.test(trimmed) &&
            trimmed.includes(")") &&
            !trimmed.includes("{") &&
            !trimmed.endsWith(";")
        ) {
            const nextLine =
                lines[index + 1]
                    ? lines[index + 1].trim()
                    : "";

            if (
                nextLine !== "{" &&
                nextLine !== ""
            ) {
                errors.push({
                    line: index + 1,
                    type: "Syntax Error",
                    code: trimmed,
                    explanation:
                        "The if statement does not contain a valid block or statement."
                });
            }
        }
    });

    if (braceBalance > 0) {
        errors.push({
            line: lines.length,
            type: "Syntax Error",
            code: "Program structure",
            explanation:
                "One or more opening braces are missing their closing braces."
        });
    }

    if (parenthesisBalance > 0) {
        errors.push({
            line: lines.length,
            type: "Syntax Error",
            code: "Program structure",
            explanation:
                "One or more opening parentheses are missing their closing parentheses."
        });
    }

    if (insideString) {
        errors.push({
            line: lines.length,
            type: "Syntax Error",
            code: "String literal",
            explanation:
                "A string literal was opened but not properly closed."
        });
    }

    return removeDuplicateErrors(errors);
}

/* =========================================================
   LOGICAL ANALYSIS
========================================================= */

function detectLogicalErrors(code) {
    const errors = [];
    const variables = {};
    const lines = code.split("\n");

    /* Find integer assignments */

    lines.forEach(line => {
        const match = line.match(
            /\bint\s+([A-Za-z_$][\w$]*)\s*=\s*(-?\d+)\s*;/
        );

        if (match) {
            variables[match[1]] =
                Number(match[2]);
        }

        /* Also track later assignments */

        const assignment =
            line.match(
                /^\s*([A-Za-z_$][\w$]*)\s*=\s*(-?\d+)\s*;/
            );

        if (assignment) {
            variables[assignment[1]] =
                Number(assignment[2]);
        }
    });

    /* IF CONDITIONS */

    lines.forEach((line, index) => {
        const ifMatch =
            line.match(/\bif\s*\((.*)\)/);

        if (!ifMatch) {
            return;
        }

        const condition =
            ifMatch[1].trim();

        const result =
            evaluateCondition(
                condition,
                variables
            );

        if (result === false) {
            errors.push({
                line: index + 1,
                type: "Unreachable TRUE Branch",
                code: line.trim(),
                explanation:
                    `The condition "${condition}" is false for the known variable values. The TRUE branch cannot execute.`
            });
        }

        if (
            condition.includes("&&") &&
            isContradictoryCondition(
                condition,
                variables
            )
        ) {
            errors.push({
                line: index + 1,
                type: "Contradictory Condition",
                code: line.trim(),
                explanation:
                    `The condition "${condition}" cannot become true with the known variable values.`
            });
        }
    });

    /* WHILE CONDITIONS */

    lines.forEach((line, index) => {
        const whileMatch =
            line.match(/\bwhile\s*\((.*)\)/);

        if (!whileMatch) {
            return;
        }

        const condition =
            whileMatch[1].trim();

        if (condition === "true") {
            const loopBody =
                getBlockAfterLine(
                    lines,
                    index
                );

            if (!/\bbreak\s*;/.test(loopBody)) {
                errors.push({
                    line: index + 1,
                    type: "Infinite Loop",
                    code: line.trim(),
                    explanation:
                        "The while condition is always true and no break statement was found."
                });
            }
        }

        const result =
            evaluateCondition(
                condition,
                variables
            );

        if (result === false) {
            errors.push({
                line: index + 1,
                type: "Unreachable While Loop",
                code: line.trim(),
                explanation:
                    `The loop condition "${condition}" is false for the known variable values, so the loop body cannot execute.`
            });
        }
    });

    return removeDuplicateErrors(errors);
}

/* =========================================================
   CONDITION EVALUATION
========================================================= */

function evaluateCondition(
    condition,
    variables
) {
    try {
        let expression = condition;

        Object.keys(variables).forEach(variable => {
            const value =
                variables[variable];

            const regex =
                new RegExp(
                    `\\b${escapeRegex(variable)}\\b`,
                    "g"
                );

            expression =
                expression.replace(
                    regex,
                    String(value)
                );
        });

        if (expression.includes("&&")) {
            return expression
                .split("&&")
                .every(part =>
                    evaluateSimpleCondition(
                        part.trim()
                    )
                );
        }

        if (expression.includes("||")) {
            return expression
                .split("||")
                .some(part =>
                    evaluateSimpleCondition(
                        part.trim()
                    )
                );
        }

        return evaluateSimpleCondition(
            expression
        );

    } catch {
        return null;
    }
}

function evaluateSimpleCondition(
    expression
) {
    const match =
        expression.match(
            /^(-?\d+)\s*(>=|<=|==|!=|>|<)\s*(-?\d+)$/
        );

    if (!match) {
        return null;
    }

    const left =
        Number(match[1]);

    const operator =
        match[2];

    const right =
        Number(match[3]);

    switch (operator) {
        case ">":
            return left > right;

        case "<":
            return left < right;

        case ">=":
            return left >= right;

        case "<=":
            return left <= right;

        case "==":
            return left === right;

        case "!=":
            return left !== right;

        default:
            return null;
    }
}

/* =========================================================
   CONTRADICTION DETECTION
========================================================= */

function isContradictoryCondition(
    condition,
    variables
) {
    const parts =
        condition.split("&&");

    if (parts.length < 2) {
        return false;
    }

    const results =
        parts.map(part =>
            evaluateCondition(
                part.trim(),
                variables
            )
        );

    return results.some(
        result => result === false
    );
}

/* =========================================================
   BLOCK EXTRACTION
========================================================= */

function getBlockAfterLine(
    lines,
    index
) {
    let block = "";

    for (
        let i = index;
        i < Math.min(
            index + 20,
            lines.length
        );
        i++
    ) {
        block +=
            lines[i] + "\n";

        if (
            i > index &&
            lines[i].includes("}")
        ) {
            break;
        }
    }

    return block;
}

/* =========================================================
   ERROR DISPLAY
========================================================= */

function updateErrors() {
    const list =
        document.getElementById(
            "errorList"
        );

    const badge =
        document.getElementById(
            "errorBadge"
        );

    if (!list || !badge) {
        return;
    }

    const errors =
        currentAnalysis.errors;

    badge.textContent =
        `${errors.length} ${
            errors.length === 1
                ? "Error"
                : "Errors"
        }`;

    if (errors.length === 0) {
        list.innerHTML = `
            <div class="empty-state">
                <div>✅</div>
                <h3>No errors detected</h3>
                <p>The analyzed program passed the available checks.</p>
            </div>
        `;

        return;
    }

    list.innerHTML =
        errors.map(error => `
            <div class="error-card"
                 title="Hover over this error to understand why it happened">

                <div class="error-top">

                    <div>
                        <div class="error-type">
                            🔴 ${escapeHtml(error.type)}
                        </div>
                    </div>

                    <div class="error-line">
                        Line ${error.line}
                    </div>

                </div>

                <div class="error-code">
                    ${escapeHtml(error.code)}
                </div>

                <div class="error-explanation">
                    💡 ${escapeHtml(error.explanation)}
                </div>

            </div>
        `).join("");
}

/* =========================================================
   ANALYSIS FLOW ANIMATION
========================================================= */

function runAnalysisAnimation(errors) {
    const stages = [
        "stage-source",
        "stage-ast",
        "stage-data",
        "stage-z3",
        "stage-result"
    ];

    stages.forEach(id => {
        const element =
            document.getElementById(id);

        if (element) {
            element.classList.remove(
                "active",
                "success",
                "error"
            );
        }
    });

    const trace =
        document.getElementById(
            "analysisTrace"
        );

    if (trace) {
        trace.innerHTML = "";
    }

    createASTAnimationArea();

    const steps = [
        {
            stage: "stage-source",
            label: "SOURCE",
            value: "Reading Java code...",
            message:
                "Source code received."
        },

        {
            stage: "stage-ast",
            label: "AST",
            value: "Building syntax tree...",
            message:
                "Variables, values, conditions and operators are being classified."
        },

        {
            stage: "stage-data",
            label: "DATA FLOW",
            value: "Moving values...",
            message:
                "Following x = 5 through the condition."
        },

        {
            stage: "stage-z3",
            label: "Z3",
            value: "Checking constraints...",
            message:
                "Testing whether the logical condition can be satisfied."
        },

        {
            stage: "stage-result",
            label: "RESULT",
            value:
                errors.length
                    ? "Logical error found!"
                    : "Condition is satisfiable.",
            message:
                errors.length
                    ? "Logical analysis identified a possible problem."
                    : "The analyzed condition is satisfiable."
        }
    ];

    let delay = 0;

    steps.forEach(step => {
        setTimeout(() => {
            stages.forEach(id => {
                const element =
                    document.getElementById(id);

                if (element) {
                    element.classList.remove(
                        "active"
                    );
                }
            });

            const element =
                document.getElementById(
                    step.stage
                );

            if (!element) {
                return;
            }

            element.classList.add(
                "active"
            );

            const value =
                element.querySelector(
                    ".stage-value"
                );

            if (value) {
                value.textContent =
                    step.value;
            }

            if (
                step.stage ===
                "stage-result"
            ) {
                element.classList.remove(
                    "active"
                );

                element.classList.add(
                    errors.length
                        ? "error"
                        : "success"
                );
            }

            addTraceLine(
                step.label,
                step.message,
                errors.length &&
                step.stage ===
                    "stage-result"
            );

            if (
                step.stage ===
                "stage-ast"
            ) {
                animateAST();
            }

        }, delay);

        delay += 1100;
    });
}

/* =========================================================
   SYNTAX ERROR ANIMATION
========================================================= */

function runSyntaxErrorAnimation(
    errors
) {
    const trace =
        document.getElementById(
            "analysisTrace"
        );

    if (trace) {
        trace.innerHTML = "";
    }

    createASTAnimationArea();

    const steps = [
        [
            "SOURCE",
            "Source code received."
        ],
        [
            "PARSER",
            "Checking Java syntax..."
        ],
        [
            "ERROR",
            "Syntax error detected. AST and logical analysis stopped."
        ]
    ];

    steps.forEach((step, index) => {
        setTimeout(() => {
            addTraceLine(
                step[0],
                step[1],
                index === 2
            );
        }, index * 900);
    });

    const astArea =
        document.getElementById(
            "astAnimation"
        );

    if (astArea) {
        astArea.innerHTML = `
            <div class="ast-error-message">
                <div>🚨</div>
                <strong>Syntax Error</strong>
                <p>
                    The program must be syntactically valid
                    before the AST and Z3 analysis can continue.
                </p>
            </div>
        `;
    }
}

/* =========================================================
   AST VISUALIZATION
========================================================= */

function createASTAnimationArea() {
    const existing =
        document.getElementById(
            "astAnimation"
        );

    if (existing) {
        return existing;
    }

    const trace =
        document.getElementById(
            "analysisTrace"
        );

    if (!trace) {
        return null;
    }

    const area =
        document.createElement("div");

    area.id =
        "astAnimation";

    area.innerHTML = `
        <div class="ast-title">
            🌳 Live AST Visualization
        </div>

        <div class="ast-subtitle">
            Watch the program break into values,
            variables and operators.
        </div>

        <div class="ast-tree">

            <div class="ast-node ast-root">
                IF CONDITION
            </div>

            <div class="ast-connector"></div>

            <div class="ast-node ast-operator">
                &&
            </div>

            <div class="ast-branches">

                <div class="ast-branch">

                    <div class="ast-node ast-operator">
                        &gt;
                    </div>

                    <div class="ast-small-branches">

                        <div
                            class="ast-node ast-variable"
                            data-token="x1"
                        >
                            x
                        </div>

                        <div
                            class="ast-node ast-value"
                            data-token="5"
                        >
                            10
                        </div>

                    </div>

                </div>

                <div class="ast-branch">

                    <div class="ast-node ast-operator">
                        &lt;
                    </div>

                    <div class="ast-small-branches">

                        <div
                            class="ast-node ast-variable"
                            data-token="x2"
                        >
                            x
                        </div>

                        <div
                            class="ast-node ast-value"
                            data-token="3"
                        >
                            3
                        </div>

                    </div>

                </div>

            </div>

            <div class="ast-assignment">

                <span>Variable assignment:</span>

                <span class="ast-node ast-variable">
                    x
                </span>

                <span> = </span>

                <span
                    class="ast-node ast-value"
                >
                    5
                </span>

            </div>

        </div>

        <div
            id="astResult"
            class="ast-result"
        >
            Waiting for AST analysis...
        </div>
    `;

    trace.parentNode.insertBefore(
        area,
        trace
    );

    return area;
}

function animateAST() {
    const area =
        document.getElementById(
            "astAnimation"
        );

    if (!area) {
        return;
    }

    const tokens =
        area.querySelectorAll(
            ".ast-node"
        );

    tokens.forEach(token => {
        token.classList.remove(
            "ast-moving",
            "ast-found",
            "ast-error"
        );
    });

    const result =
        document.getElementById(
            "astResult"
        );

    if (result) {
        result.textContent =
            "Scanning AST nodes...";
    }

    tokens.forEach((token, index) => {
        setTimeout(() => {
            token.classList.add(
                "ast-moving"
            );

            setTimeout(() => {
                token.classList.remove(
                    "ast-moving"
                );

                token.classList.add(
                    "ast-found"
                );

                if (result) {
                    result.textContent =
                        `AST node classified: ${token.textContent.trim()}`;
                }
            }, 450);

        }, index * 650);
    });

    setTimeout(() => {
        if (result) {
            result.innerHTML =
                `5 &gt; 10 → <strong>FALSE</strong> &nbsp; | &nbsp; ` +
                `5 &lt; 3 → <strong>FALSE</strong> &nbsp; | &nbsp; ` +
                `FALSE && FALSE → <strong>FALSE</strong>`;
        }

        const operators =
            area.querySelectorAll(
                ".ast-operator"
            );

        operators.forEach(operator => {
            operator.classList.add(
                "ast-error"
            );
        });
    }, tokens.length * 650 + 600);
}

/* =========================================================
   REPLAY
========================================================= */

function replayAnalysis() {
    if (!currentAnalysis.code) {
        alert(
            "Please analyze a program first."
        );

        switchTab("code");

        return;
    }

    switchTab("flow");

    if (
        currentAnalysis.syntaxErrors.length
    ) {
        runSyntaxErrorAnimation(
            currentAnalysis.syntaxErrors
        );

        return;
    }

    runAnalysisAnimation(
        currentAnalysis.errors
    );
}

/* =========================================================
   TRACE
========================================================= */

function addTraceLine(
    label,
    message,
    error
) {
    const trace =
        document.getElementById(
            "analysisTrace"
        );

    if (!trace) {
        return;
    }

    const line =
        document.createElement("div");

    line.className =
        `trace-line ${
            error
                ? "trace-error"
                : ""
        }`;

    const time =
        new Date().toLocaleTimeString();

    line.innerHTML =
        `<span class="trace-time">[${time}]</span> ` +
        `<span class="trace-label">${escapeHtml(label)}</span> ` +
        `${escapeHtml(message)}`;

    trace.appendChild(line);

    trace.scrollTop =
        trace.scrollHeight;
}

/* =========================================================
   HISTORY
========================================================= */

function saveHistory() {
    if (!currentAnalysis.timestamp) {
        return;
    }

    const history =
        JSON.parse(
            localStorage.getItem(
                "logicLensHistory"
            ) || "[]"
        );

    history.unshift({
        timestamp:
            currentAnalysis.timestamp
                .toLocaleString(),

        lines:
            currentAnalysis.lines,

        errors:
            currentAnalysis.errors.length,

        time:
            currentAnalysis.time
    });

    localStorage.setItem(
        "logicLensHistory",
        JSON.stringify(
            history.slice(0, 10)
        )
    );
}

function displayHistory() {
    const list =
        document.getElementById(
            "historyList"
        );

    if (!list) {
        return;
    }

    const history =
        JSON.parse(
            localStorage.getItem(
                "logicLensHistory"
            ) || "[]"
        );

    if (!history.length) {
        list.innerHTML = `
            <div class="empty-state">
                <div>🕘</div>
                <h3>No analysis history</h3>
                <p>Your previous analyses will appear here.</p>
            </div>
        `;

        return;
    }

    list.innerHTML =
        history.map(item => `
            <div class="history-item">

                <div>
                    <strong>
                        Analysis Session
                    </strong>

                    <small>
                        ${escapeHtml(
                            item.timestamp
                        )}
                    </small>
                </div>

                <div>
                    ${item.lines} lines
                </div>

                <div class="history-count">
                    ${item.errors} errors
                </div>

            </div>
        `).join("");
}

function clearHistory() {
    if (
        !confirm(
            "Clear all analysis history?"
        )
    ) {
        return;
    }

    localStorage.removeItem(
        "logicLensHistory"
    );

    displayHistory();
}

/* =========================================================
   FINAL REPORT
========================================================= */

function updateReport() {
    setText(
        "reportFile",
        "TestProgram.java"
    );

    setText(
        "reportTime",
        currentAnalysis.time
    );

    setText(
        "reportLines",
        currentAnalysis.lines
    );

    setText(
        "reportErrors",
        currentAnalysis.errors.length
    );

    setText(
        "reportWarnings",
        currentAnalysis.warnings
    );

    const result =
        document.getElementById(
            "reportResult"
        );

    const details =
        document.getElementById(
            "reportDetails"
        );

    if (!result || !details) {
        return;
    }

    if (!currentAnalysis.code) {
        result.className =
            "report-result";

        result.innerHTML = `
            <div class="report-icon">
                🔍
            </div>

            <div>
                <span>ANALYSIS STATUS</span>
                <h3>Waiting for analysis</h3>
            </div>
        `;

        details.textContent =
            "Run an analysis to generate the final report.";

        return;
    }

    if (
        currentAnalysis.syntaxErrors.length
    ) {
        result.className =
            "report-result error";

        result.innerHTML = `
            <div class="report-icon">
                🚨
            </div>

            <div>
                <span>ANALYSIS STATUS</span>
                <h3>
                    Syntax error detected
                </h3>
            </div>
        `;

        details.innerHTML =
            `The program contains ${
                currentAnalysis.syntaxErrors.length
            } syntax error(s). ` +
            `Logical analysis was stopped until the Java syntax is corrected.`;

        return;
    }

    if (
        currentAnalysis.errors.length
    ) {
        result.className =
            "report-result error";

        result.innerHTML = `
            <div class="report-icon">
                🚨
            </div>

            <div>
                <span>ANALYSIS STATUS</span>

                <h3>
                    ${currentAnalysis.errors.length}
                    logical error(s) detected
                </h3>
            </div>
        `;

        details.innerHTML =
            `The analysis found ${
                currentAnalysis.errors.length
            } logical issue(s). ` +
            `The Errors tab provides the affected line, error type and explanation.`;

    } else {
        result.className =
            "report-result success";

        result.innerHTML = `
            <div class="report-icon">
                ✅
            </div>

            <div>
                <span>ANALYSIS STATUS</span>

                <h3>
                    No logical errors detected
                </h3>
            </div>
        `;

        details.innerHTML =
            "The analyzed program passed the available frontend logical checks.";
    }
}

/* =========================================================
   PRINT REPORT
========================================================= */

function printReport() {
    if (!currentAnalysis.code) {
        alert(
            "Please analyze a program first."
        );

        return;
    }

    window.print();
}

/* =========================================================
   HELPERS
========================================================= */

function setText(
    id,
    value
) {
    const element =
        document.getElementById(id);

    if (element) {
        element.textContent =
            String(value);
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeRegex(value) {
    return value.replace(
        /[.*+?^${}()|[\]\\]/g,
        "\\$&"
    );
}

function removeDuplicateErrors(
    errors
) {
    const unique = [];

    errors.forEach(error => {
        const exists =
            unique.some(item =>
                item.line === error.line &&
                item.type === error.type
            );

        if (!exists) {
            unique.push(error);
        }
    });

    return unique;
}

/* =========================================================
   INITIAL SETUP
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    () => {
        updateLineNumbers();
        loadExample();
        displayHistory();

        const textarea =
            document.getElementById(
                "codeInput"
            );

        if (textarea) {
            textarea.addEventListener(
                "input",
                updateLineNumbers
            );
        }
    }
);
/* =========================================================
   LIVE AST CLASSIFICATION ANIMATION
   ========================================================= */

function animateAstClassification(code, errors) {
    const animation = document.getElementById("astAnimation");

    if (!animation) {
        return;
    }

    const variableItems = document.getElementById("variableItems");
    const valueItems = document.getElementById("valueItems");
    const operatorItems = document.getElementById("operatorItems");
    const logicalItems = document.getElementById("logicalItems");
    const satisfiedItems = document.getElementById("satisfiedItems");
    const unsatisfiedItems = document.getElementById("unsatisfiedItems");
    const errorItems = document.getElementById("errorItems");

    animation.innerHTML = "";

    if (variableItems) variableItems.innerHTML = "Waiting...";
    if (valueItems) valueItems.innerHTML = "Waiting...";
    if (operatorItems) operatorItems.innerHTML = "Waiting...";
    if (logicalItems) logicalItems.innerHTML = "Waiting...";
    if (satisfiedItems) satisfiedItems.innerHTML = "Waiting...";
    if (unsatisfiedItems) unsatisfiedItems.innerHTML = "Waiting...";
    if (errorItems) errorItems.innerHTML = "Waiting...";

    const assignmentMatches = [
        ...code.matchAll(
            /\bint\s+([A-Za-z_$][\w$]*)\s*=\s*(-?\d+)\s*;/g
        )
    ];

    const variables = assignmentMatches.map(match => match[1]);
    const values = assignmentMatches.map(match => match[2]);

    const comparisonOperators = [
        ...code.matchAll(/(>=|<=|==|!=|>|<)/g)
    ].map(match => match[1]);

    const logicalOperators = [
        ...code.matchAll(/(&&|\|\|)/g)
    ].map(match => match[1]);

    const conditions = [
        ...code.matchAll(/\bif\s*\((.*?)\)/g)
    ].map(match => match[1]);

    let step = 0;

    function showStep(callback, delay) {
        setTimeout(callback, delay);
    }

    showStep(() => {
        animation.innerHTML = `
            <div class="ast-stage">
                <div class="ast-stage-title">
                    STEP 1 — BUILDING AST
                </div>

                <div class="ast-tree">

                    <div class="ast-tree-row">
                        <div class="ast-node operator">IF CONDITION</div>
                    </div>

                    <div class="ast-arrow">↓</div>

                    <div class="ast-tree-row">
                        <div class="ast-node logical">
                            ${logicalOperators[0] || "&&"}
                        </div>
                    </div>

                </div>
            </div>
        `;

        setClassification(
            variableItems,
            variables,
            "classification-token"
        );

        setClassification(
            valueItems,
            values,
            "classification-token"
        );

    }, step++ * 900);

    showStep(() => {
        const condition = conditions[0] || "x > 10 && x < 3";

        const parts = condition
            .split(/(&&|\|\|)/)
            .map(part => part.trim())
            .filter(Boolean);

        animation.innerHTML = `
            <div class="ast-stage">

                <div class="ast-stage-title">
                    STEP 2 — CLASSIFYING AST NODES
                </div>

                <div class="ast-tree">

                    <div class="ast-tree-row">

                        ${parts.map(part => {

                            if (part === "&&" || part === "||") {
                                return `
                                    <div class="ast-node logical">
                                        ${escapeHtml(part)}
                                    </div>
                                `;
                            }

                            const match = part.match(
                                /^([A-Za-z_$][\w$]*)\s*(>=|<=|==|!=|>|<)\s*(-?\d+)$/
                            );

                            if (!match) {
                                return `
                                    <div class="ast-node">
                                        ${escapeHtml(part)}
                                    </div>
                                `;
                            }

                            return `
                                <div class="ast-tree-row">

                                    <div class="ast-node variable">
                                        ${escapeHtml(match[1])}
                                    </div>

                                    <div class="ast-node operator">
                                        ${escapeHtml(match[2])}
                                    </div>

                                    <div class="ast-node value">
                                        ${escapeHtml(match[3])}
                                    </div>

                                </div>
                            `;
                        }).join("")}

                    </div>

                </div>

            </div>
        `;

        setClassification(
            operatorItems,
            comparisonOperators,
            "classification-token warning"
        );

        setClassification(
            logicalItems,
            logicalOperators,
            "classification-token warning"
        );

    }, step++ * 900);

    showStep(() => {

        const variable =
            variables[0] || "x";

        const value =
            values[0] || "5";

        const condition =
            conditions[0] || "x > 10 && x < 3";

        const evaluatedParts =
            evaluateAstCondition(
                condition,
                {
                    [variable]: Number(value)
                }
            );

        animation.innerHTML = `
            <div class="ast-stage">

                <div class="ast-stage-title">
                    STEP 3 — DATA FLOW & VALUE EVALUATION
                </div>

                <div class="ast-tree">

                    <div class="ast-tree-row">

                        <div class="ast-node variable">
                            ${escapeHtml(variable)}
                        </div>

                        <div class="ast-arrow">→</div>

                        <div class="ast-node value">
                            ${escapeHtml(value)}
                        </div>

                    </div>

                    <div class="ast-arrow">↓</div>

                    <div class="ast-tree-row">

                        ${
                            evaluatedParts
                                .map(item => `
                                    <div class="ast-node ${
                                        item.result
                                            ? "operator"
                                            : "value"
                                    }">
                                        ${escapeHtml(item.expression)}
                                    </div>
                                `)
                                .join("")
                        }

                    </div>

                </div>

            </div>
        `;

        const satisfied =
            evaluatedParts
                .filter(item => item.result === true)
                .map(item => item.expression);

        const unsatisfied =
            evaluatedParts
                .filter(item => item.result === false)
                .map(item => item.expression);

        setClassification(
            satisfiedItems,
            satisfied,
            "classification-token good"
        );

        setClassification(
            unsatisfiedItems,
            unsatisfied,
            "classification-token bad"
        );

    }, step++ * 900);

    showStep(() => {

        const hasErrors =
            errors && errors.length > 0;

        animation.innerHTML = `
            <div class="ast-stage">

                <div class="ast-stage-title">
                    STEP 4 — FINAL LOGICAL DECISION
                </div>

                <div class="ast-tree">

                    <div class="ast-node logical">
                        LOGICAL CONDITION
                    </div>

                    <div class="ast-arrow">↓</div>

                    <div class="ast-result ${
                        hasErrors
                            ? "unsatisfied"
                            : "satisfied"
                    }">

                        ${
                            hasErrors
                                ? "🚨 LOGICAL ERROR PREDICTED"
                                : "✅ CONDITION SATISFIED"
                        }

                    </div>

                </div>

            </div>
        `;

        if (errorItems) {
            errorItems.innerHTML = hasErrors
                ? `<span class="classification-token bad">
                       ${errors.length} issue(s) detected
                   </span>`
                : `<span class="classification-token good">
                       No logical issue detected
                   </span>`;
        }

    }, step++ * 900);
}


function setClassification(element, values, className) {
    if (!element) {
        return;
    }

    if (!values || values.length === 0) {
        element.innerHTML = "None";
        return;
    }

    const uniqueValues = [...new Set(values)];

    element.innerHTML =
        uniqueValues
            .map((value, index) => `
                <span
                    class="${className}"
                    style="animation-delay:${index * 100}ms"
                >
                    ${escapeHtml(value)}
                </span>
            `)
            .join("");
}


function evaluateAstCondition(condition, variables) {
    const results = [];

    const parts = condition
        .split(/(&&|\|\|)/)
        .map(part => part.trim())
        .filter(part => part !== "&&" && part !== "||");

    parts.forEach(part => {

        let expression = part;

        Object.keys(variables).forEach(variable => {

            const regex =
                new RegExp(
                    `\\b${escapeRegex(variable)}\\b`,
                    "g"
                );

            expression =
                expression.replace(
                    regex,
                    String(variables[variable])
                );
        });

        const match =
            expression.match(
                /^(-?\d+)\s*(>=|<=|==|!=|>|<)\s*(-?\d+)$/
            );

        if (!match) {
            return;
        }

        const left = Number(match[1]);
        const operator = match[2];
        const right = Number(match[3]);

        let result = false;

        switch (operator) {

            case ">":
                result = left > right;
                break;

            case "<":
                result = left < right;
                break;

            case ">=":
                result = left >= right;
                break;

            case "<=":
                result = left <= right;
                break;

            case "==":
                result = left === right;
                break;

            case "!=":
                result = left !== right;
                break;
        }

        results.push({
            expression:
                `${left} ${operator} ${right} → ${
                    result ? "TRUE" : "FALSE"
                }`,
            result
        });

    });

    return results;
}