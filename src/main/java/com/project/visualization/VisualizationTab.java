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
            new JPanel(new FlowLayout(
                    FlowLayout.LEFT, 12, 12
            ));

    private List<VisualizationEvent> events;

    private int currentEvent = 0;
    private int iteration = 0;

    public VisualizationTab() {

        setTitle(
                "Static Logical Error Detector - Visualization"
        );

        setSize(1250, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        startClock();
    }

    private void buildUI() {

        JPanel root =
                new JPanel(new BorderLayout(10, 10));

        root.setBorder(
                new EmptyBorder(10, 10, 10, 10)
        );

        setContentPane(root);

        // =========================
        // HEADER
        // =========================

        JPanel header =
                new JPanel(new BorderLayout());

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

        header.add(title, BorderLayout.WEST);
        header.add(clockLabel, BorderLayout.EAST);

        // =========================
        // CONTROLS
        // =========================

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
                new JPanel(new BorderLayout());

        top.add(header, BorderLayout.NORTH);
        top.add(controls, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);

        // =========================
        // SOURCE
        // =========================

        sourceArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        15
                )
        );

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
                BorderFactory.createTitledBorder(
                        "SOURCE CODE"
                )
        );

        // =========================
        // EVENTS
        // =========================

        eventArea.setEditable(false);

        eventArea.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        14
                )
        );

        JScrollPane eventScroll =
                new JScrollPane(eventArea);

        eventScroll.setBorder(
                BorderFactory.createTitledBorder(
                        "ANALYSIS EVENTS"
                )
        );

        JSplitPane split =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        sourceScroll,
                        eventScroll
                );

        split.setDividerLocation(570);

        root.add(split, BorderLayout.CENTER);

        // =========================
        // VISUAL STATE
        // =========================

        boxesPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "VISUAL PROGRAM STATE"
                )
        );

        JScrollPane boxScroll =
                new JScrollPane(boxesPanel);

        boxScroll.setPreferredSize(
                new Dimension(1000, 170)
        );

        root.add(
                boxScroll,
                BorderLayout.SOUTH
        );
    }

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

        boxesPanel.revalidate();
        boxesPanel.repaint();

        eventArea.append(
                "=== VISUALIZATION STARTED ===\n\n"
        );

        nextEvent();
    }

    private void nextEvent() {

        if (events == null || events.isEmpty()) {
            analyze();
            return;
        }

        if (currentEvent >= events.size()) {

            eventArea.append(
                    "\n=== END OF VISUALIZATION ===\n"
            );

            return;
        }

        VisualizationEvent event =
                events.get(currentEvent++);

        eventArea.append(
                "Line " +
                        event.getLineNumber() +
                        " [" +
                        event.getType() +
                        "] " +
                        event.getValue() +
                        "\n"
        );

        addBox(
                event.getType(),
                event.getValue()
        );
    }

    private void nextIteration() {

        iteration++;

        iterationLabel.setText(
                "Iteration: " + iteration
        );

        String message =
                "Loop iteration " +
                        iteration;

        eventArea.append(
                "\n" +
                        message +
                        "\n"
        );

        addBox(
                "LOOP ITERATION",
                "Iteration " +
                        iteration
        );
    }

    private void showZ3Result(
            String result,
            boolean unsat
    ) {

        resultLabel.setText(
                "Z3 Result: " + result
        );

        eventArea.append(
                "\nZ3 SOLVER RESULT: " +
                        result +
                        "\n"
        );

        addBox(
                unsat
                        ? "UNSATISFIABLE"
                        : "SATISFIABLE",
                result
        );
    }

    private void addBox(
            String type,
            String value
    ) {

        JPanel box =
                new JPanel(
                        new BorderLayout()
                );

        box.setPreferredSize(
                new Dimension(205, 85)
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

        JTextArea valueLabel =
                new JTextArea(value);

        valueLabel.setEditable(false);
        valueLabel.setLineWrap(true);
        valueLabel.setWrapStyleWord(true);
        valueLabel.setOpaque(false);

        valueLabel.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        12
                )
        );

        box.add(
                typeLabel,
                BorderLayout.NORTH
        );

        box.add(
                valueLabel,
                BorderLayout.CENTER
        );

        boxesPanel.add(box);

        boxesPanel.revalidate();
        boxesPanel.repaint();
    }

    private void clearVisualization() {

        eventArea.setText("");

        boxesPanel.removeAll();

        boxesPanel.revalidate();
        boxesPanel.repaint();

        currentEvent = 0;
        iteration = 0;

        iterationLabel.setText(
                "Iteration: 0"
        );

        resultLabel.setText(
                "Z3 Result: Not evaluated"
        );
    }

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

        timer.start();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            VisualizationTab window =
                    new VisualizationTab();

            window.setVisible(true);
        });
    }
}
