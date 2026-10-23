
package com.project.visualization;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VisualizationTab extends JFrame {

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

    private int currentEvent = 0;
    private int iteration = 0;

    public VisualizationTab() {

        setTitle(
                "Static Logical Error Detector - Visualization"
        );

        setSize(1250, 780);

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

        root.setBorder(
                new EmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        setContentPane(root);

        // =====================================================
        // HEADER
        // =====================================================

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

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

        clockLabel.setFont(
                new Font(
                        "Monospaced",
                        Font.BOLD,
                        18
                )
        );

        header.add(
                title,
                BorderLayout.WEST
        );

        header.add(
                clockLabel,
                BorderLayout.EAST
        );

        // =====================================================
        // SOURCE CODE
        // =====================================================

        sourceArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        15
                )
        );

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
                new JScrollPane(
                        sourceArea
                );

        sourceScroll.setBorder(
                BorderFactory.createTitledBorder(
                        "SOURCE CODE"
                )
        );

        // =====================================================
        // ANALYSIS EVENTS
        // =====================================================

        eventArea.setEditable(false);

        eventArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        14
                )
        );

        eventArea.setLineWrap(true);

        eventArea.setWrapStyleWord(true);

        JScrollPane eventScroll =
                new JScrollPane(
                        eventArea
                );

        eventScroll.setBorder(
                BorderFactory.createTitledBorder(
                        "ANALYSIS EVENTS"
                )
        );

        // =====================================================
        // SPLIT SCREEN
        // =====================================================

        JSplitPane split =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        sourceScroll,
                        eventScroll
                );

        split.setDividerLocation(600);

        split.setResizeWeight(0.5);

        root.add(
                split,
                BorderLayout.CENTER
        );

        // =====================================================
        // VISUAL PROGRAM STATE
        // =====================================================

        boxesPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "VISUAL PROGRAM STATE"
                )
        );

        boxesPanel.setBackground(
                UIManager.getColor(
                        "Panel.background"
                )
        );

        JScrollPane boxScroll =
                new JScrollPane(
                        boxesPanel
                );

        boxScroll.setPreferredSize(
                new Dimension(
                        1000,
                        190
                )
        );

        root.add(
                boxScroll,
                BorderLayout.SOUTH
        );

        // =====================================================
        // CONTROLS
        // =====================================================

        JPanel controls =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                10,
                                5
                        )
                );

        JButton analyzeButton =
                new JButton("▶ Analyze");

        JButton nextButton =
                new JButton("⏭ Next");

        JButton loopButton =
                new JButton("🔄 Loop Iteration");

        JButton satButton =
                new JButton("✓ SAT");

        JButton unsatButton =
                new JButton("✗ UNSAT");

        JButton clearButton =
                new JButton("Clear");

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

        controls.add(analyzeButton);
        controls.add(nextButton);
        controls.add(loopButton);
        controls.add(satButton);
        controls.add(unsatButton);
        controls.add(clearButton);
        controls.add(iterationLabel);
        controls.add(resultLabel);

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

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
    }

    // =====================================================
    // ANALYZE
    // =====================================================

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

        /*
         * IMPORTANT:
         *
         * Do NOT create one box for every event.
         *
         * Instead, group all SOURCE events together,
         * all VARIABLE events together, etc.
         */
        addGroupedBoxes();

        boxesPanel.revalidate();

        boxesPanel.repaint();

        eventArea.append(
                "Analysis started.\n"
        );
    }

    // =====================================================
    // GROUP EVENTS INTO CATEGORY BOXES
    // =====================================================

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

        for (VisualizationEvent event : events) {

            switch (event.getType()) {

                case "SOURCE":

                    appendLine(
                            source,
                            event.getValue()
                    );

                    break;

                case "VARIABLE":

                    appendLine(
                            variables,
                            event.getValue()
                    );

                    break;

                case "OPERATOR":

                    appendLine(
                            operators,
                            event.getValue()
                    );

                    break;

                case "LOOP":

                    appendLine(
                            loops,
                            event.getValue()
                    );

                    break;

                case "CONDITION":

                    appendLine(
                            conditions,
                            event.getValue()
                    );

                    break;

                default:

                    break;
            }
        }

        // SOURCE BOX

        if (source.length() > 0) {

            addBox(
                    "SOURCE",
                    source.toString()
            );
        }

        // VARIABLE BOX

        if (variables.length() > 0) {

            addBox(
                    "VARIABLE",
                    variables.toString()
            );
        }

        // OPERATOR BOX

        if (operators.length() > 0) {

            addBox(
                    "OPERATOR",
                    operators.toString()
            );
        }

        // LOOP BOX

        if (loops.length() > 0) {

            addBox(
                    "LOOP",
                    loops.toString()
            );
        }

        // CONDITION BOX

        if (conditions.length() > 0) {

            addBox(
                    "CONDITION",
                    conditions.toString()
            );
        }

        boxesPanel.revalidate();

        boxesPanel.repaint();
    }

    // =====================================================
    // APPEND LINE
    // =====================================================

    private void appendLine(
            StringBuilder builder,
            String value) {

        if (builder.length() > 0) {

            builder.append('\n');
        }

        builder.append(value);
    }

    // =====================================================
    // NEXT EVENT
    // =====================================================

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

        eventArea.append(
                "["
                        + event.getType()
                        + "] "
                        + event.getValue()
                        + "\n"
        );
    }

    // =====================================================
    // LOOP ITERATION
    // =====================================================

    private void nextIteration() {

        iteration++;

        iterationLabel.setText(
                "Iteration: " + iteration
        );

        eventArea.append(
                "\nLOOP ITERATION "
                        + iteration
                        + "\n"
        );

        addIterationBox(
                iteration
        );
    }

    // =====================================================
    // Z3 RESULT
    // =====================================================

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
    }

    // =====================================================
    // ADD CATEGORY BOX
    // =====================================================

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
                        220,
                        130
                )
        );

        box.setBorder(
                BorderFactory.createLineBorder(
                        Color.DARK_GRAY,
                        2
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

        JTextArea valueArea =
                new JTextArea();

        valueArea.setEditable(false);

        valueArea.setFocusable(false);

        valueArea.setLineWrap(true);

        valueArea.setWrapStyleWord(true);

        valueArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        12
                )
        );

        valueArea.setText(value);

        valueArea.setBorder(
                new EmptyBorder(
                        5,
                        5,
                        5,
                        5
                )
        );

        JScrollPane valueScroll =
                new JScrollPane(
                        valueArea
                );

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

    // =====================================================
    // LOOP ITERATION BOX
    // =====================================================

    private void addIterationBox(
            int iterationNumber) {

        JPanel box =
                new JPanel(
                        new BorderLayout()
                );

        box.setPreferredSize(
                new Dimension(
                        220,
                        100
                )
        );

        box.setBorder(
                BorderFactory.createLineBorder(
                        Color.DARK_GRAY,
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

    // =====================================================
    // CLEAR
    // =====================================================

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

    // =====================================================
    // CLOCK
    // =====================================================

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

    // =====================================================
    // MAIN
    // =====================================================

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
