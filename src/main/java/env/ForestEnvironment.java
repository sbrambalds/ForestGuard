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
import jason.asSyntax.Structure;
import jason.environment.Environment;
import model.Config;
import model.ForestModel;
import utils.Coord2D;
import static utils.Utils.termToInteger;

public class ForestEnvironment extends Environment {

    private ForestModel model;
    private SimulationController controller;
    private final Map<String, Coord2D> agentsPoses = Collections.synchronizedMap(new HashMap<>(Config.STATIONS));
    private final Object clock = new Object();
    private volatile long tick = 0;
    private long lastFireTick = 0;
    private long lastSpreadTick = 0;

    public ForestEnvironment() {
        super(Config.STATIONS.size());
    }

    @Override
    public void init(final String[] args) {

        this.model = new ForestModel();

        this.model.initForest();

        this.controller = new SimulationController(model, agentsPoses, this);
        
        SwingUtilities.invokeLater(() -> controller.startSimulation());

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
                && !pos.equals(Config.homeOf(other.getKey())));
    }

    private Literal boundPercept(String agent) {
        Coord2D home = Config.homeOf(agent);
        int xMin = home.x() >= Config.CENTER_X ? Config.CENTER_X : 0;
        int xMax = home.x() >= Config.CENTER_X ? Config.GRID_WIDTH - 1 : Config.CENTER_X - 1;
        int yMin = home.y() >= Config.CENTER_Y ? Config.CENTER_Y : 0;
        int yMax = home.y() >= Config.CENTER_Y ? Config.GRID_HEIGHT - 1 : Config.CENTER_Y - 1;
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
            percepts.add(Literal.parseLiteral("wait_time(" + Config.WAIT_TICKS + ")"));
            percepts.addAll(allPercepts(agent));
            if (agent.contains("scout")) {
                percepts.add(boundPercept(agent));
            }
            Coord2D home = Config.homeOf(agent);
            if (home != null) {
                percepts.add(Literal.parseLiteral("home(" + home.x() + "," + home.y() + ")"));
            }
        }
        return percepts;
    }

    @Override
    public boolean executeAction(String agent, Structure action) {
        try {
            switch (action.getFunctor()) {
                case "move" -> {
                    int newX = termToInteger(action.getTerm(0));
                    int newY = termToInteger(action.getTerm(1));
                    Coord2D newPos = new Coord2D(newX, newY);
                    if (newPos.isValid()) {
                        agentsPoses.put(agent, newPos);
                    }
                    awaitTicks(1);
                    return true;
                }
                case "wait" -> {
                    awaitTicks(termToInteger(action.getTerm(0)));
                    return true;
                }
                case "fire_extinguished" -> {
                    int posX = termToInteger(action.getTerm(0));
                    int posY = termToInteger(action.getTerm(1));
                    Coord2D agentPos = new Coord2D(posX, posY);
                    model.extinguishFire(agentPos, tick);
                    if(!model.isFireActive()) {
                        lastFireTick = tick;
                    }
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } catch (NoValueException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void awaitTicks(long n) throws InterruptedException {
        synchronized (clock) {
            long target = tick + n;
            while (tick < target) {
                clock.wait();
            }
        }
    }

    public List<Coord2D> step() {
        synchronized (clock) {
            tick++;
            clock.notifyAll();
        }

        if (!model.isFireActive() && tick - lastFireTick >= Config.FIRE_DELAY) {
            model.startRandomFire(tick);
        }

        if (model.isFireActive() && tick - lastSpreadTick >= Config.SPREAD_DELAY) {
            model.spreadFire(tick);
            lastSpreadTick = tick;
        }

        model.dryTree(tick);

        List<Coord2D> removed = model.removeTree(tick);
        if (!removed.isEmpty() && !model.isFireActive()) {
            lastFireTick = tick;
        }
        return removed;
    }
}


