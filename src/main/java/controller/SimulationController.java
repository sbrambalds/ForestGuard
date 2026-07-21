package controller;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.Timer;

import env.ForestEnvironment;
import model.Config;
import model.ForestModel;
import utils.Coord2D;
import view.ControlPanel;
import view.ForestPanel;

public class SimulationController {

    private final ForestModel model;
    private final ForestPanel simulationView;
    private final JFrame frame;
    private final ControlPanel controlsView;
    private Timer tickTimer;

    public SimulationController(ForestModel model, Map<String, Coord2D> agentsPoses, ForestEnvironment env) {
        this.model = model;
        this.simulationView = new ForestPanel(this.model, agentsPoses);
        this.frame = new JFrame("ForestGuard");
        this.tickTimer = new Timer(1000 / model.getFPS(), e -> {
            List<Coord2D> changedCells = env.step();
            simulationView.updateCells(changedCells);
            simulationView.repaint();
        });
        this.controlsView = new ControlPanel(model.getFPS(), fps -> {
            model.setFPS(fps);
            tickTimer.setDelay(1000 / fps);
        });
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void startSimulation() {
        simulationView.setPreferredSize(new Dimension(
            Config.GRID_WIDTH * Config.CELL_SIZE,
            Config.GRID_HEIGHT * Config.CELL_SIZE
        ));

        frame.setLayout(new BorderLayout());
        frame.add(controlsView, BorderLayout.NORTH);
        frame.add(simulationView, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        simulationView.initBuffer();

        tickTimer.start();
    }
}
