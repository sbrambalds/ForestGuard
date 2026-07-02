package env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import controller.SimulationController;
import jason.NoValueException;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import model.CellState;
import model.Config;
import model.ForestModel;
import utils.Coord2D;

public class ForestEnvironment extends Environment {

    private ForestModel model;
    SimulationController controller;
    private final Map<String, Coord2D> agentsPoses = Collections.synchronizedMap(new HashMap<>());
    private final List<Literal> stationPercepts = new ArrayList<>();

    @Override
    public void init(final String[] args) {

        this.model = new ForestModel();

        this.model.initForest();

        initScoutAgents();

        initStationAgent();

        this.controller = new SimulationController(model, agentsPoses);

        SwingUtilities.invokeLater(() -> controller.startSimulation());
    }

    private void initScoutAgents() {
        int centerX = Config.GRID_WIDTH / 2;
        int centerY = Config.GRID_HEIGHT / 2;

        agentsPoses.put("scoutN", new Coord2D(centerX + 2, centerY + 3));
        agentsPoses.put("scoutE", new Coord2D(centerX + 2, centerY - 3));
        agentsPoses.put("scoutW", new Coord2D(centerX - 2, centerY + 3));
        agentsPoses.put("scoutS", new Coord2D(centerX - 2, centerY - 3));

    }

    private void initStationAgent() {
        agentsPoses.forEach((name, pos) -> {
            stationPercepts.add(Literal.parseLiteral("charge_station(" + name + ", " + pos.x() + ", " + pos.y() + ")"));
        });
    }

    private Collection<Literal> mappingPercepts(String agent) {
        Coord2D agentPose = agentsPoses.get(agent);
        return agentPose
            .neighbours()
            .stream()
            .map((Coord2D pos) -> {
                if (pos.isValid()) {
                    String state = model.getGrid()[pos.x()][pos.y()].getState().toString().toLowerCase();
                    return Literal.parseLiteral("cell(" + pos.x() + ", " + pos.y() + ", " + state + ")");
                } else {
                    return Literal.parseLiteral("border(" + pos.x() + ", " + pos.y() + ")");
                }
            }).collect(Collectors.toList());
    }

    private Collection<Literal> obstaclePercepts(String agent) {
        Coord2D pose = agentsPoses.get(agent);
        List<Literal> obstacles = new ArrayList<>();

        for (Coord2D pos : pose.cardinalNeighbours()) {
            if(pos.isValid()) {
                CellState posState = model.getGrid()[pos.x()][pos.y()].getState();
                if(agentsPoses.containsValue(pos) || posState == CellState.STATION) {
                    obstacles.add(Literal.parseLiteral("obstacle(" + pos.x() + ", " + pos.y() + ")"));
                }
            }
        }

        return obstacles;
    }

    @Override
    public Collection<Literal> getPercepts(String agent) {
        List<Literal> percepts = new ArrayList<>();

        if(agent.contains("scout")){
            Coord2D pos = agentsPoses.get(agent);
            percepts.add(Literal.parseLiteral("position(" + pos.x() + "," + pos.y() + ")"));
            percepts.addAll(mappingPercepts(agent));
            percepts.addAll(obstaclePercepts(agent));
        } else if(agent.equals("station")) {
            percepts.addAll(stationPercepts);
        }
        return percepts;
    }

    private void notifyChange() {
        controller.updateView();
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        if (action.getFunctor().equals("move")) {
            try {
                int newX = (int)((NumberTerm) action.getTerm(0)).solve();
                int newY = (int)((NumberTerm) action.getTerm(1)).solve();
                Coord2D newPos = new Coord2D(newX, newY);
                if (newPos.isValid() && !agentsPoses.containsValue(newPos)) {
                    agentsPoses.put(agent, newPos);
                }
            } catch (NoValueException e) {}
            try {
                Thread.sleep(1000L / Config.FPS);
            } catch (InterruptedException ignored) { }
            notifyChange();
            return true;
        }
        return false;
    }
}


