package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import utils.Coord2D;

public final class ForestModel {

    private final Random rand = new Random();
    private ForestCell[][] grid = new ForestCell[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    private final Queue<Coord2D> lakes = new LinkedList<>();
    private final List<Coord2D> trees = new ArrayList<>();
    private final ConcurrentHashMap<Coord2D, Long> burningTrees = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Coord2D, Long> wetTrees = new ConcurrentHashMap<>();


    public ForestModel() {
        grid = new ForestCell[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    }

    public void initForest() {
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                grid[i][j] = new ForestCell(CellState.EMPTY);
            }
        }
        generateStation();
        generateLakes();
        generateTrees();
    }

    private void generateStation() {
        for (int i = Config.CENTER_X - 2; i <= Config.CENTER_X + 2; i++) {
            for (int j = Config.CENTER_Y - 2; j <= Config.CENTER_Y + 3; j++) {
                grid[i][j].updateState(CellState.STATION);
            }
        }

        Config.SCOUT_STATIONS.values().forEach(pos -> grid[pos.x()][pos.y()].updateState(CellState.CHARGE_STATION));
        Config.FIREFIGHTER_STATIONS.values().forEach(pos -> grid[pos.x()][pos.y()].updateState(CellState.WATER_STATION));
    }

    private void generateLakes() {
        setLakeSeeds();

        while (!lakes.isEmpty()) { 
            double prob = rand.nextDouble();
            double fixedProb = 0.6;
            Coord2D seed = lakes.poll();

            for (Coord2D pos : seed.cardinalNeighbours()) {
                if(pos.isValid()) {
                    ForestCell neighbour = grid[pos.x()][pos.y()];

                    if(prob > fixedProb && neighbour.getState() == CellState.EMPTY) {
                        grid[pos.x()][pos.y()].updateState(CellState.WATER);
                        lakes.add(pos);
                        fixedProb = fixedProb + 0.001;
                    }
                }
            }

            List<CellState> neighbourSelectedCells = new ArrayList<>();

            for (Coord2D neighbour : seed.neighbours()) {
                if(neighbour.isValid()) {
                    neighbourSelectedCells.add(grid[neighbour.x()][neighbour.y()].getState());
                }
            }

            if(!neighbourSelectedCells.contains(CellState.WATER)) {
                grid[seed.x()][seed.y()].updateState(CellState.EMPTY);
            }

        }
        fillLakeHoles();
    }

    private void fillLakeHoles() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < Config.GRID_WIDTH; i++) {
                for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                    if (grid[i][j].getState() == CellState.WATER) continue;
                    long waterNeighbours = new Coord2D(i, j).cardinalNeighbours().stream()
                        .filter(c -> c.isValid() && grid[c.x()][c.y()].getState() == CellState.WATER)
                        .count();
                    if (waterNeighbours >= 3) {
                        grid[i][j].updateState(CellState.WATER);
                        changed = true;
                    }
                }
            }
        }
    }

    private void generateTrees() {
        for (int i = 0; i < Config.TREES_NUMBER; i++) {

            int seedX = rand.nextInt(0, Config.GRID_WIDTH);
            int seedY = rand.nextInt(0, Config.GRID_HEIGHT);

            Coord2D seed = new Coord2D(seedX, seedY);

            if(grid[seed.x()][seed.y()].getState() == CellState.EMPTY) {
                grid[seed.x()][seed.y()].updateState(CellState.TREE);
                trees.add(seed);
            }
        }
    }

    private void setLakeSeeds() {
        for (int i = 0; i < Config.LAKES_NUMBER; i++) {

            int seedX = rand.nextInt(0, Config.GRID_WIDTH);
            int seedY = rand.nextInt(0, Config.GRID_HEIGHT);

            Coord2D seed = new Coord2D(seedX, seedY);

            if(!lakes.contains(seed) && grid[seed.x()][seed.y()].getState() == CellState.EMPTY) {
                lakes.add(seed);
                grid[seed.x()][seed.y()].updateState(CellState.WATER);
            }
        }
    }

    public List<Coord2D> removeTree(long tick) {
        List<Coord2D> toRemove = new ArrayList<>();
        for (Coord2D tree : burningTrees.keySet()) {
            if (tick - burningTrees.get(tree) >= Config.BURNING_DELAY) {
                toRemove.add(tree);
            }
        }
        for (Coord2D tree : toRemove) {
            grid[tree.x()][tree.y()].updateState(CellState.EMPTY);
            burningTrees.remove(tree);
            trees.remove(tree);
        }
        return toRemove;
    }

    public void dryTree(long tick) {
        List<Coord2D> driedTree = new ArrayList<>();
        for (Coord2D tree : wetTrees.keySet()) {
            if (tick - wetTrees.get(tree) >= Config.DRY_DELAY) {
                driedTree.add(tree);
            }
        }
        for (Coord2D tree : driedTree) {
            grid[tree.x()][tree.y()].updateState(CellState.TREE);
            wetTrees.remove(tree);
        }
    }

    public void startRandomFire(long tick) {
        Coord2D randTree = this.trees.get(rand.nextInt(0, trees.size()));
        burningTrees.put(randTree, tick);
        this.grid[randTree.x()][randTree.y()].updateState(CellState.BURNING);
    }

    public void spreadFire(long tick) {
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                if(this.grid[i][j].getState() == CellState.BURNING) {
                    Coord2D burningTree = new Coord2D(i, j);
                    double prob = rand.nextDouble();
                    List<Coord2D> neighbourTrees = burningTree.neighbours()
                        .stream()
                        .filter(coord -> coord.isValid() &&
                            this.grid[coord.x()][coord.y()].getState() == CellState.TREE
                        ).collect(Collectors.toList());
                    if(!neighbourTrees.isEmpty() && prob > Config.SPREAD_PROB) {
                        Coord2D newBurningTree = neighbourTrees.get(rand.nextInt(0, neighbourTrees.size()));
                        burningTrees.put(newBurningTree, tick);
                        this.grid[newBurningTree.x()][newBurningTree.y()].updateState(CellState.BURNING);
                    }
                }
            }
        }
    }

    public void extinguishFire(Coord2D treePos, long tick){
        burningTrees.remove(treePos);
        wetTrees.put(treePos, tick);
        grid[treePos.x()][treePos.y()].updateState(CellState.WET_TREE);
    }

    public ForestCell[][] getGrid() {
        return this.grid;
    }

    public boolean isFireActive() { return !burningTrees.isEmpty(); }
}
