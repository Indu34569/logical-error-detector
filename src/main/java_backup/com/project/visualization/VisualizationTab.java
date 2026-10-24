package com.project.visualization;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VisualizationTab extends JFrame {

    private static final Color LIGHT_PURPLE = new Color(242, 237, 255);
    private static final Color LIGHT_PURPLE_2 = new Color(235, 225, 250);
    private static final Color BORDER_COLOR = new Color(150, 120, 180);
    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;

    private final JTextArea sourceArea = new JTextArea();
    private final JTextArea eventArea = new JTextArea();

    private final JLabel clockLabel = new JLabel();
    private final JLabel iterationLabel =
            new JLabel("Iteration: 0");

    private final JLabel resultLabel =
            new JLabel("Z3 Result: Not evaluated");

    private final JPanel boxesPanel =
            new JPanel(
                    new FlowLayout(
                            FlowLayout.LEFT,
                            15,
                            15
                    )
            );

    private List<VisualizationEvent> events;

    /*
     * currentEvent always points to the NEXT event
     * that has not yet been displayed.
     */
    private int currentEvent = 0;

    /*
     * This is only the last real iteration received
     * from VisualizationEngine.
     *
     * It is NOT used to control the loop.
     */
    private int iteration = 0;

    public VisualizationTab() {

        setTitle(
                "Static Logical Error Detector - Visualization"
        );

        setSize(1250, 780);

        setMinimumSize(
                new Dimension(1000, 650)
        );

        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        buildUI();

        startClock();
    }

    private void buildUI() {

        JPanel root =
                new JPanel(
                        new BorderLayout(10, 10)
                );

        root.setBackground(LIGHT_PURPLE);

        root.setBorder(
                new EmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        setContentPane(root);

        // ========================================================
        // HEADER
        // ========================================================

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBackground(LIGHT_PURPLE);

        JLabel title =
                new JLabel(
                        "INTERACTIVE LOGIC VISUALIZATION"
                );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        22
                )
        );

        title.setForeground(BLACK);

        clockLabel.setFont(
                new Font(
                        "Monospaced",
                        Font.BOLD,
                        18
                )
        );

        clockLabel.setForeground(BLACK);

        header.add(
                title,
                BorderLayout.WEST
        );

        header.add(
                clockLabel,
                BorderLayout.EAST
        );

        // ========================================================
        // SOURCE CODE
        // ========================================================

        sourceArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        15
                )
        );

        sourceArea.setForeground(BLACK);
        sourceArea.setBackground(WHITE);
        sourceArea.setCaretColor(BLACK);
        sourceArea.setLineWrap(false);

        sourceArea.setText(
                "int age = 10;\n" +
                "int count = 0;\n" +
                "\n" +
                "while (count < 3) {\n" +
                "    count++;\n" +
                "}\n" +
                "\n" +
                "if (age > 20) {\n" +
                "    System.out.println(age);\n" +
                "}"
        );

        JScrollPane sourceScroll =
                new JScrollPane(sourceArea);

        sourceScroll.setBorder(
                createSectionBorder("SOURCE CODE")
        );

        // ========================================================
        // ANALYSIS EVENTS
        // ========================================================

        eventArea.setEditable(false);
        eventArea.setFocusable(false);

        eventArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        14
                )
        );

        eventArea.setForeground(BLACK);
        eventArea.setBackground(WHITE);
        eventArea.setCaretColor(BLACK);

        eventArea.setLineWrap(true);
        eventArea.setWrapStyleWord(true);

        JScrollPane eventScroll =
                new JScrollPane(eventArea);

        eventScroll.setBorder(
                createSectionBorder("ANALYSIS EVENTS")
        );

        // ========================================================
        // SPLIT SCREEN
        // ========================================================

        JSplitPane split =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        sourceScroll,
                        eventScroll
                );

        split.setDividerLocation(600);
        split.setResizeWeight(0.5);
        split.setBorder(
                BorderFactory.createEmptyBorder()
        );

        // ========================================================
        // VISUAL PROGRAM STATE
        // ========================================================

        boxesPanel.setBackground(LIGHT_PURPLE);

        boxesPanel.setBorder(
                createSectionBorder("VISUAL PROGRAM STATE")
        );

        JScrollPane boxScroll =
                new JScrollPane(
                        boxesPanel,
                        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                );

        boxScroll.setPreferredSize(
                new Dimension(
                        1000,
                        200
                )
        );

        boxScroll.setBackground(LIGHT_PURPLE);

        boxScroll.getViewport().setBackground(
                LIGHT_PURPLE
        );

        boxScroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        // ========================================================
        // CONTROLS
        // ========================================================

        JPanel controls =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                10,
                                5
                        )
                );

        controls.setBackground(LIGHT_PURPLE);

        JButton analyzeButton =
                createButton("▶ Analyze");

        JButton nextButton =
                createButton("⏭ Next");

        JButton loopButton =
                createButton("🔄 Loop Iteration");

        JButton satButton =
                createButton("✓ SAT");

        JButton unsatButton =
                createButton("✗ UNSAT");

        JButton clearButton =
                createButton("Clear");

        analyzeButton.addActionListener(
                e -> analyze()
        );

        nextButton.addActionListener(
                e -> nextEvent()
        );

        loopButton.addActionListener(
                e -> nextIteration()
        );

        satButton.addActionListener(
                e -> showZ3Result(
                        "SATISFIABLE",
                        false
                )
        );

        unsatButton.addActionListener(
                e -> showZ3Result(
                        "UNSATISFIABLE",
                        true
                )
        );

        clearButton.addActionListener(
                e -> clearVisualization()
        );

        iterationLabel.setForeground(BLACK);

        iterationLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        resultLabel.setForeground(BLACK);

        resultLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        controls.add(analyzeButton);
        controls.add(nextButton);
        controls.add(loopButton);
        controls.add(satButton);
        controls.add(unsatButton);
        controls.add(clearButton);
        controls.add(iterationLabel);
        controls.add(resultLabel);

        // ========================================================
        // TOP
        // ========================================================

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

        top.setBackground(LIGHT_PURPLE);

        top.add(
                header,
                BorderLayout.NORTH
        );

        top.add(
                controls,
                BorderLayout.SOUTH
        );

        root.add(
                top,
                BorderLayout.NORTH
        );

        root.add(
                split,
                BorderLayout.CENTER
        );

        root.add(
                boxScroll,
                BorderLayout.SOUTH
        );
    }

    // ============================================================
    // SECTION BORDER
    // ============================================================

    private TitledBorder createSectionBorder(
            String title) {

        TitledBorder border =
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(
                                BORDER_COLOR,
                                2
                        ),
                        title
                );

        border.setTitleColor(BLACK);

        border.setTitleFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        return border;
    }

    // ============================================================
    // BUTTON
    // ============================================================

    private JButton createButton(
            String text) {

        JButton button =
                new JButton(text);

        button.setForeground(BLACK);
        button.setBackground(LIGHT_PURPLE_2);
        button.setFocusPainted(false);

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_COLOR
                        ),
                        BorderFactory.createEmptyBorder(
                                6,
                                10,
                                6,
                                10
                        )
                )
        );

        return button;
    }

    // ============================================================
    // ANALYZE
    // ============================================================

    private void analyze() {

        events =
                VisualizationEngine.createEvents(
                        sourceArea.getText()
                );

        currentEvent = 0;
        iteration = 0;

        iterationLabel.setText(
                "Iteration: 0"
        );

        resultLabel.setText(
                "Z3 Result: Not evaluated"
        );

        eventArea.setText("");

        boxesPanel.removeAll();

        addGroupedBoxes();

        boxesPanel.revalidate();
        boxesPanel.repaint();

        eventArea.append(
                "Analysis started.\n"
        );

        eventArea.append(
                "Events detected: "
                        + events.size()
                        + "\n"
        );
    }

    // ============================================================
    // GROUP EVENTS
    // ============================================================

    private void addGroupedBoxes() {

        boxesPanel.removeAll();

        StringBuilder source =
                new StringBuilder();

        StringBuilder variables =
                new StringBuilder();

        StringBuilder operators =
                new StringBuilder();

        StringBuilder loops =
                new StringBuilder();

        StringBuilder conditions =
                new StringBuilder();

        if (events == null) {
            return;
        }

        for (VisualizationEvent event : events) {

            if (event == null) {
                continue;
            }

            String type = event.getType();
            String value = event.getValue();

            if (type == null) {
                continue;
            }

            switch (type) {

                case "SOURCE":
                    appendLine(source, value);
                    break;

                case "VARIABLE":
                    appendLine(variables, value);
                    break;

                case "OPERATOR":
                    appendLine(operators, value);
                    break;

                case "LOOP":
                    appendLine(loops, value);
                    break;

                case "CONDITION":
                    appendLine(conditions, value);
                    break;

                default:
                    break;
            }
        }

        if (source.length() > 0) {
            addBox(
                    "SOURCE",
                    source.toString()
            );
        }

        if (variables.length() > 0) {
            addBox(
                    "VARIABLE",
                    variables.toString()
            );
        }

        if (operators.length() > 0) {
            addBox(
                    "OPERATOR",
                    operators.toString()
            );
        }

        if (loops.length() > 0) {
            addBox(
                    "LOOP",
                    loops.toString()
            );
        }

        if (conditions.length() > 0) {
            addBox(
                    "CONDITION",
                    conditions.toString()
            );
        }

        boxesPanel.revalidate();
        boxesPanel.repaint();
    }

    // ============================================================
    // APPEND LINE
    // ============================================================

    private void appendLine(
            StringBuilder builder,
            String value) {

        if (value == null) {
            return;
        }

        if (builder.length() > 0) {
            builder.append('\n');
        }

        builder.append(value);
    }

    // ============================================================
    // NEXT EVENT
    // ============================================================

    private void nextEvent() {

        if (events == null || events.isEmpty()) {
            analyze();
            return;
        }

        if (currentEvent >= events.size()) {

            eventArea.append(
                    "\n--- END OF VISUALIZATION ---\n"
            );

            return;
        }

        VisualizationEvent event =
                events.get(currentEvent++);

        if (event == null) {
            return;
        }

        eventArea.append(
                "["
                        + event.getType()
                        + "] "
                        + event.getValue()
                        + "\n"
        );

        eventArea.setCaretPosition(
                eventArea.getDocument().getLength()
        );
    }

    // ============================================================
    // NEXT LOOP ITERATION
    // ============================================================

    /*
     * IMPORTANT:
     *
     * This method DOES NOT assume that the loop has 3 iterations.
     *
     * It searches the actual events generated by
     * VisualizationEngine.
     *
     * Therefore:
     *
     *     while (count < 3)
     *
     * produces:
     *
     *     ITERATION 1
     *     ITERATION 2
     *     ITERATION 3
     *
     * while:
     *
     *     while (count < 5)
     *
     * produces:
     *
     *     ITERATION 1
     *     ITERATION 2
     *     ITERATION 3
     *     ITERATION 4
     *     ITERATION 5
     *
     * No hardcoded iteration limit exists here.
     */

    private void nextIteration() {

        if (events == null || events.isEmpty()) {
            analyze();
            return;
        }

        while (currentEvent < events.size()) {

            VisualizationEvent event =
                    events.get(currentEvent++);

            if (event == null) {
                continue;
            }

            if (!"LOOP".equals(event.getType())) {
                continue;
            }

            String value = event.getValue();

            if (value == null) {
                continue;
            }

            // ----------------------------------------------------
            // ACTUAL LOOP ITERATION
            // ----------------------------------------------------

            if (value.startsWith("LOOP ITERATION")) {

                String numberText =
                        value.substring(
                                "LOOP ITERATION".length()
                        ).trim();

                int actualIteration;

                try {

                    actualIteration =
                            Integer.parseInt(numberText);

                } catch (NumberFormatException ex) {

                    /*
                     * If the engine ever produces an unexpected
                     * iteration label, keep the previous number
                     * instead of inventing a loop count.
                     */
                    actualIteration = iteration + 1;
                }

                iteration = actualIteration;

                iterationLabel.setText(
                        "Iteration: " + actualIteration
                );

                eventArea.append(
                        "[LOOP] "
                                + value
                                + "\n"
                );

                addIterationBox(
                        actualIteration
                );

                eventArea.setCaretPosition(
                        eventArea.getDocument().getLength()
                );

                return;
            }

            // ----------------------------------------------------
            // LOOP FINISHED
            // ----------------------------------------------------

            if (value.startsWith("LOOP STOPPED")) {

                eventArea.append(
                        "[LOOP] "
                                + value
                                + "\n"
                );

                eventArea.setCaretPosition(
                        eventArea.getDocument().getLength()
                );

                return;
            }
        }

        eventArea.append(
                "\n--- NO MORE LOOP EVENTS ---\n"
        );

        eventArea.setCaretPosition(
                eventArea.getDocument().getLength()
        );
    }

    // ============================================================
    // Z3 RESULT
    // ============================================================

    private void showZ3Result(
            String result,
            boolean unsat) {

        resultLabel.setText(
                "Z3 Result: " + result
        );

        addBox(
                unsat
                        ? "UNSATISFIABLE"
                        : "SATISFIABLE",
                result
        );

        eventArea.append(
                "\nZ3 SOLVER RESULT: "
                        + result
                        + "\n"
        );

        eventArea.setCaretPosition(
                eventArea.getDocument().getLength()
        );
    }

    // ============================================================
    // ADD CATEGORY BOX
    // ============================================================

    private void addBox(
            String type,
            String value) {

        JPanel box =
                new JPanel(
                        new BorderLayout(
                                5,
                                5
                        )
                );

        box.setPreferredSize(
                new Dimension(
                        230,
                        145
                )
        );

        box.setBackground(
                LIGHT_PURPLE
        );

        box.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_COLOR,
                                2
                        ),
                        BorderFactory.createEmptyBorder(
                                4,
                                4,
                                4,
                                4
                        )
                )
        );

        JLabel typeLabel =
                new JLabel(
                        type,
                        SwingConstants.CENTER
                );

        typeLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        typeLabel.setForeground(BLACK);
        typeLabel.setOpaque(true);
        typeLabel.setBackground(LIGHT_PURPLE_2);

        JTextArea valueArea =
                new JTextArea();

        valueArea.setEditable(false);
        valueArea.setFocusable(false);
        valueArea.setForeground(BLACK);
        valueArea.setBackground(WHITE);
        valueArea.setCaretColor(BLACK);
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);

        valueArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        12
                )
        );

        valueArea.setText(
                value == null
                        ? ""
                        : value
        );

        valueArea.setBorder(
                new EmptyBorder(
                        5,
                        5,
                        5,
                        5
                )
        );

        JScrollPane valueScroll =
                new JScrollPane(valueArea);

        valueScroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        box.add(
                typeLabel,
                BorderLayout.NORTH
        );

        box.add(
                valueScroll,
                BorderLayout.CENTER
        );

        boxesPanel.add(box);
    }

    // ============================================================
    // LOOP ITERATION BOX
    // ============================================================

    private void addIterationBox(
            int iterationNumber) {

        JPanel box =
                new JPanel(
                        new BorderLayout()
                );

        box.setPreferredSize(
                new Dimension(
                        230,
                        100
                )
        );

        box.setBackground(
                LIGHT_PURPLE
        );

        box.setBorder(
                BorderFactory.createLineBorder(
                        BORDER_COLOR,
                        2
                )
        );

        JLabel title =
                new JLabel(
                        "LOOP ITERATION",
                        SwingConstants.CENTER
                );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        title.setForeground(BLACK);
        title.setOpaque(true);
        title.setBackground(LIGHT_PURPLE_2);

        JLabel value =
                new JLabel(
                        "Iteration "
                                + iterationNumber,
                        SwingConstants.CENTER
                );

        value.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        14
                )
        );

        value.setForeground(BLACK);

        box.add(
                title,
                BorderLayout.NORTH
        );

        box.add(
                value,
                BorderLayout.CENTER
        );

        boxesPanel.add(box);

        boxesPanel.revalidate();
        boxesPanel.repaint();
    }

    // ============================================================
    // CLEAR
    // ============================================================

    private void clearVisualization() {

        eventArea.setText("");

        boxesPanel.removeAll();

        boxesPanel.revalidate();
        boxesPanel.repaint();

        currentEvent = 0;
        iteration = 0;
        events = null;

        iterationLabel.setText(
                "Iteration: 0"
        );

        resultLabel.setText(
                "Z3 Result: Not evaluated"
        );
    }

    // ============================================================
    // CLOCK
    // ============================================================

    private void startClock() {

        Timer timer =
                new Timer(
                        1000,
                        e -> clockLabel.setText(
                                LocalTime.now().format(
                                        DateTimeFormatter.ofPattern(
                                                "HH:mm:ss"
                                        )
                                )
                        )
                );

        timer.setInitialDelay(0);
        timer.start();
    }

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    VisualizationTab window =
                            new VisualizationTab();

                    window.setVisible(true);
                }
        );
    }
}