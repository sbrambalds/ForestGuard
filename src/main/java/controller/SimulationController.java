package controller;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.Timer;

import env.ForestEnvironment;
import jason.runtime.MASConsoleGUI;
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
        this.tickTimer = new Timer(1000 / Config.INITIAL_FPS, e -> {
            if (MASConsoleGUI.hasConsole() && MASConsoleGUI.get().isPause()) return;
            List<Coord2D> changedCells = env.step();
            simulationView.updateCells(changedCells);
            simulationView.repaint();
        });
        this.controlsView = new ControlPanel(
            Config.INITIAL_FPS,
            fps -> tickTimer.setDelay(1000 / fps),
            paused -> {
                if (paused) tickTimer.stop(); else tickTimer.start();
                simulationView.setPaused(paused);
            }
        );
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
