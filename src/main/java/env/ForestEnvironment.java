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
import model.Config;
import model.ForestModel;
import utils.Coord2D;

public class ForestEnvironment extends Environment {

    private final int CENTER_X = Config.GRID_WIDTH / 2;
    private final int CENTER_Y = Config.GRID_HEIGHT / 2;
    private final static int FIRE_DELAY = 60_000;
    private final static int SPREAD_DELAY = FIRE_DELAY / 3;

    private ForestModel model;
    private SimulationController controller;
    private final Map<String, Coord2D> agentsPoses = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Coord2D> homePositions = Collections.synchronizedMap(new HashMap<>());
    private final List<Literal> stationPercepts = new ArrayList<>();
    private long lastFireTime = System.currentTimeMillis();
    private long lastSpreadTime = System.currentTimeMillis();
    
    @Override
    public void init(final String[] args) {

        this.model = new ForestModel();

        this.model.initForest();

        initScoutAgents();

        initFFAgents();

        initChargeStations();

        this.controller = new SimulationController(model, agentsPoses, this);
        
        SwingUtilities.invokeLater(() -> controller.startSimulation());

    }

    private void initScoutAgents() {
        agentsPoses.put("scoutN", new Coord2D(CENTER_X + 2, CENTER_Y + 2));
        agentsPoses.put("scoutE", new Coord2D(CENTER_X + 2, CENTER_Y - 2));
        agentsPoses.put("scoutW", new Coord2D(CENTER_X - 2, CENTER_Y + 2));
        agentsPoses.put("scoutS", new Coord2D(CENTER_X - 2, CENTER_Y - 2));
        agentsPoses.forEach((name, pos) -> {
            if(name.contains("scout")) {
                homePositions.put(name, pos);
            }
        });
    }

    private void initChargeStations() {
        homePositions.forEach((name, pos) -> {
            stationPercepts.add(Literal.parseLiteral("charge_station(" + name + ", " + pos.x() + ", " + pos.y() + ")"));
            if (name.startsWith("firefighter")) {
                String index = name.substring("firefighter".length());
                stationPercepts.add(Literal.parseLiteral("firefighter_name(" + index + ", " + name + ")"));
            }
        });
    }

    private void initFFAgents() {
        agentsPoses.put("firefighter1", new Coord2D(CENTER_X + 2, CENTER_Y));
        agentsPoses.put("firefighter2", new Coord2D(CENTER_X - 2, CENTER_Y));
        agentsPoses.put("firefighter3", new Coord2D(CENTER_X, CENTER_Y + 2));
        agentsPoses.put("firefighter4", new Coord2D(CENTER_X, CENTER_Y - 2));
        agentsPoses.forEach((name, pos) -> {
            if(name.contains("firefighter")) {
                homePositions.put(name, pos);
            }
        });    
    }

    private Collection<Literal> mappingPercepts(String agent) {
        Coord2D agentPose = agentsPoses.get(agent);
        return agentPose
            .visionRadius()
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

    private Collection<Literal> neighbourPercepts(String agent) {
        Coord2D agentPose = agentsPoses.get(agent);
        List<Coord2D> perceivedCells = agentPose.visionRadius();
        List<Literal> neighbours = new ArrayList<>();

        agentsPoses.forEach((other, otherPos) -> {
            if (!other.equals(agent) && other.contains("firefighter") && perceivedCells.contains(otherPos)) {
                neighbours.add(Literal.parseLiteral("neighbour(" + other + ")"));
            }
        });

        return neighbours;
    }

    private Collection<Literal> obstaclePercepts(String agent) {
        return agentsPoses.get(agent)
            .neighbours()
            .stream()
            .filter(pos -> !pos.isValid() || isBlockedByAgent(agent, pos))
            .map(pos -> Literal.parseLiteral("obstacle(" + pos.x() + ", " + pos.y() + ")"))
            .collect(Collectors.toList());
    }

    private boolean isBlockedByAgent(String agent, Coord2D pos) {
        return agentsPoses.entrySet()
            .stream()
            .anyMatch(other -> !other.getKey().equals(agent)
                && other.getValue().equals(pos)
                && !pos.equals(homePositions.get(other.getKey())));
    }

    private Literal boundPercept(String agent) {
        Coord2D home = homePositions.get(agent);
        int xMin = home.x() >= CENTER_X ? CENTER_X : 0;
        int xMax = home.x() >= CENTER_X ? Config.GRID_WIDTH - 1 : CENTER_X - 1;
        int yMin = home.y() >= CENTER_Y ? CENTER_Y : 0;
        int yMax = home.y() >= CENTER_Y ? Config.GRID_HEIGHT - 1 : CENTER_Y - 1;
        return Literal.parseLiteral("bound(" + xMin + ", " + xMax + ", " + yMin + ", " + yMax + ")");
    }

    private Collection<Literal> allPercepts(String agent) {
        Collection<Literal> percepts = new ArrayList<>();

        percepts.addAll(obstaclePercepts(agent));
        percepts.addAll(mappingPercepts(agent));

        if(agent.contains("firefighter")) percepts.addAll(neighbourPercepts(agent));

        return percepts;
    }

    @Override
    public Collection<Literal> getPercepts(String agent) {
        List<Literal> percepts = new ArrayList<>();

        if(!agent.contains("station")){
            Coord2D pos = agentsPoses.get(agent);
            String state = model.getGrid()[pos.x()][pos.y()].getState().getName();
            percepts.add(Literal.parseLiteral("position(" + pos.x() + ", " + pos.y() + ", "+ state +")"));
            percepts.add(Literal.parseLiteral("wait_time(" + (5 * 1000 / model.getFPS()) + ")"));
            percepts.addAll(allPercepts(agent));
            if (agent.contains("scout")) {
                percepts.add(boundPercept(agent));
            }
            Coord2D home = homePositions.get(agent);
            if (home != null) {
                percepts.add(Literal.parseLiteral("home(" + home.x() + "," + home.y() + ")"));
            }
        } else {
            percepts.addAll(stationPercepts);
        }
        return percepts;
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        switch (action.getFunctor()) {
            case "move" -> {
                try {
                    int newX = (int)((NumberTerm) action.getTerm(0)).solve();
                    int newY = (int)((NumberTerm) action.getTerm(1)).solve();
                    Coord2D newPos = new Coord2D(newX, newY);
                    if (newPos.isValid()) {
                        agentsPoses.put(agent, newPos);
                    }
                } catch (NoValueException e) {}
                try {
                    Thread.sleep(1000L / model.getFPS());
                } catch (InterruptedException ignored) { }
                return true;
            }
            case "respawn" -> {
                agentsPoses.put(agent, homePositions.get(agent));
                return true;
            }
            case "fire_extinguished" -> {
                Coord2D agentPos = agentsPoses.get(agent);
                model.extinguishFire(agentPos);
                if(!model.isFireActive()) {
                    lastFireTime = System.currentTimeMillis();
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public List<Coord2D> step() {
        long now = System.currentTimeMillis();
        int fps = model.getFPS();

        if (!model.isFireActive() && now - lastFireTime >= FIRE_DELAY / fps) {
            model.startRandomFire();
        }

        if (model.isFireActive() && now - lastSpreadTime >= SPREAD_DELAY / fps) {
            model.spreadFire();
            lastSpreadTime = now;
        }

        model.dryTree();

        List<Coord2D> removed = model.removeTree();
        if (!removed.isEmpty() && !model.isFireActive()) {
            lastFireTime = System.currentTimeMillis();
        }
        return removed;
    }
}


