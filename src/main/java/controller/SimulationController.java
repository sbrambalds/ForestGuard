package controller;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;

import model.Config;
import model.ForestModel;
import utils.Coord2D;
import view.ForestPanel;

public class SimulationController {

    private static final int BASE_DELAY = 1000;

    private final ForestModel model;
    private final ForestPanel view;
    private final JFrame frame;
    private Timer timer;
    private int currentDelay = BASE_DELAY;
    public SimulationController(ForestModel model, Map<String, Coord2D> agentsPoses) {
        this.model = model;
        this.view = new ForestPanel(this.model, agentsPoses);
        this.frame = new JFrame("ForestGuard");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void startSimulation() {
        view.setPreferredSize(new Dimension(
            Config.GRID_WIDTH * Config.CELL_SIZE,
            Config.GRID_HEIGHT * Config.CELL_SIZE
        ));

        timer = new Timer(currentDelay, e -> {
            view.repaint();
        });

        frame.setLayout(new BorderLayout());
        frame.add(buildControlPanel(), BorderLayout.NORTH);
        frame.add(view, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        view.initBuffer();

        timer.start();
    }

    private JPanel buildControlPanel() {
        JPanel controls = new JPanel();

        JButton stopStart = new JButton("Stop");
        stopStart.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                stopStart.setText("Start");
            } else {
                timer.start();
                stopStart.setText("Stop");
            }
        });

        JLabel speedUp = new JLabel("Set speed");
        JSlider speed = new JSlider(100, 200, 100);
        JLabel speedValue = new JLabel("0");
        speed.addChangeListener(e -> {
            currentDelay = BASE_DELAY - (speed.getValue() * (BASE_DELAY - 200) / 200);
            speedValue.setText(speed.getValue() + "%");
            timer.setDelay(currentDelay);
        });

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            timer.stop();
            stopStart.setText("Start");
            currentDelay = BASE_DELAY;
            timer.setDelay(currentDelay);
            model.initForest();
            //panel.updateCell(i, j);
        });

        controls.add(stopStart);
        controls.add(reset);
        controls.add(speedUp);
        controls.add(speed);
        controls.add(speedValue);

        return controls;
    }

    public void updateView() {
        this.view.update();
    }
}
