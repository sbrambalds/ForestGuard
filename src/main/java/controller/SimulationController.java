package controller;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;

import env.ForestEnvironment;
import model.Config;
import model.ForestModel;
import utils.Coord2D;
import view.ForestPanel;

public class SimulationController {

    private final static int BASE_DELAY = 200;

    private final ForestModel model;
    private final ForestPanel view;
    private final JFrame frame;
    private Timer tickTimer;

    public SimulationController(ForestModel model, Map<String, Coord2D> agentsPoses, ForestEnvironment env) {
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

        tickTimer = new Timer(BASE_DELAY, e -> {
            model.tick();
            model.removeTree().forEach(c -> view.updateCell(c.x(), c.y()));
        });

        frame.setLayout(new BorderLayout());
        frame.add(buildControlPanel(), BorderLayout.NORTH);
        frame.add(view, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        view.initBuffer();

        tickTimer.start();
    }

    private JPanel buildControlPanel() {
        JPanel controls = new JPanel();

        JLabel speedUp = new JLabel("Set speed");
        JSlider speed = new JSlider(JSlider.HORIZONTAL, 1, 60, model.getFPS());
        JLabel speedValue = new JLabel(model.getFPS() + " FPS");
        speed.addChangeListener(e -> {
            int fps = speed.getValue();
            model.setFPS(fps);
            speedValue.setText(fps + " FPS");
        });

        controls.add(speedUp);
        controls.add(speed);
        controls.add(speedValue);

        return controls;
    }

    public void updateView() {
        this.view.update();
    }
}
