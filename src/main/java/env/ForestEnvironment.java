package env;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import controller.SimulationController;
import jason.NoValueException;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import model.Config;
import model.ForestModel;
import utils.Coord2D;

public class ForestEnvironment extends Environment {

    private ForestModel model;
    SimulationController controller;
    Semaphore sem;
    private final Map<String, Coord2D> agentsPoses = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void init(final String[] args) {

        this.model = new ForestModel();

        this.model.initForest();

        initAgents();

        this.sem = new Semaphore(0);

        this.controller = new SimulationController(model, agentsPoses, sem);

        SwingUtilities.invokeLater(() -> controller.startSimulation());
    }

    private void initAgents() {
        int centerX = Config.GRID_WIDTH / 2;
        int centerY = Config.GRID_HEIGHT / 2;

        agentsPoses.put("scoutN", new Coord2D(centerX, centerY - 3));
        agentsPoses.put("scoutE", new Coord2D(centerX + 3, centerY));
        agentsPoses.put("scoutW", new Coord2D(centerX - 3, centerY));
        agentsPoses.put("scoutS", new Coord2D(centerX, centerY + 3));
    }

    private Collection<Literal> mappingPercepts(String agent) {
        Coord2D agentPose = agentsPoses.get(agent);
        return agentPose
            .neighbours()
            .stream()
            .map((Coord2D pos) -> {
                if (pos.isValid()) {
                    String state = model.getGrid()[pos.x()][pos.y()].getState().toString().toLowerCase();
                    return Literal.parseLiteral("cell(" + pos.x() + " , " +pos.y() + " , " + state + ")");
                } else {
                    return Literal.parseLiteral("border(" + pos.x() + " , " + pos.y() + ")");
                }
            }).collect(Collectors.toList());
    }

    private Collection<Literal> obstaclePercepts(String agent) {
        Coord2D pose = agentsPoses.get(agent);
        List<Literal> obstacles = new ArrayList<>();

        for (Coord2D pos : pose.neighbours()) {
            if(agentsPoses.containsValue(pos) || !pos.isValid()) {
                obstacles.add(Literal.parseLiteral("obstacle(" + pos.x() + " , " + pos.y() + ")"));
            }
        }

        return obstacles;
    }

    @Override
    public Collection<Literal> getPercepts(String agent) {
        Coord2D pos = agentsPoses.get(agent);

        List<Literal> percepts = new ArrayList<>();
        percepts.add(Literal.parseLiteral("position(" + pos.x() + "," + pos.y() + ")"));
        percepts.addAll(mappingPercepts(agent));
        percepts.addAll(obstaclePercepts(agent));
        return percepts;
    }

    private void notifyChange() {
        controller.updateView();
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        if (action.getFunctor().equals("move")) {
            try {
                sem.acquire();
                int newX = (int)((NumberTerm) action.getTerm(0)).solve();
                int newY = (int)((NumberTerm) action.getTerm(1)).solve();
                Coord2D newPos = new Coord2D(newX, newY);
                if (newPos.isValid() && !agentsPoses.containsValue(newPos)) {
                    agentsPoses.put(agent, newPos);
                    notifyChange();
                }
                return true; // ← sempre true se l'azione è riconosciuta
            } catch (NoValueException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // in ForestEnvironment
    public void tick() { sem.release(agentsPoses.size()); }

}


