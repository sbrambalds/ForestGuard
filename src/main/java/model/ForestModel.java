package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import utils.Coord2D;

public final class ForestModel {

    private final Random rand = new Random();
    private ForestCell[][] grid = new ForestCell[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    private final Queue<Coord2D> lakes = new LinkedList<>();
    private static final int CENTER_X = Config.GRID_WIDTH / 2;
    private static final int CENTER_Y = Config.GRID_HEIGHT / 2;
    private int fps = 1;

    public ForestModel() {
        grid = new ForestCell[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    }

    public void initForest() {
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                grid[i][j] = new ForestCell(CellState.EMPTY);
            }
        }
        generateLakes();
        generateTrees();
        generateStation();
    }

    private void generateStation() {
        for (int i = CENTER_X - 4; i <= CENTER_X + 4; i++) {
            for (int j = CENTER_Y - 4; j <= CENTER_Y + 4; j++) {
                grid[i][j].updateState(CellState.EMPTY);
            }
        }
        for (int i = CENTER_X - 2; i <= CENTER_X + 2; i++) {
            for (int j = CENTER_Y - 2; j <= CENTER_Y + 2; j++) {
                grid[i][j].updateState(CellState.STATION);
            }
        }

        grid[CENTER_X + 2][CENTER_Y + 3].updateState(CellState.CHARGE_STATION);
        grid[CENTER_X + 2][CENTER_Y - 3].updateState(CellState.CHARGE_STATION);
        grid[CENTER_X - 2][CENTER_Y - 3].updateState(CellState.CHARGE_STATION);
        grid[CENTER_X - 2][CENTER_Y + 3].updateState(CellState.CHARGE_STATION);
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
            }
        }
    }

    private void setLakeSeeds() {
        for (int i = 0; i < Config.LAKES_NUMBER; i++) {

            int seedX = rand.nextInt(0, Config.GRID_WIDTH);
            int seedY = rand.nextInt(0, Config.GRID_HEIGHT);

            Coord2D seed = new Coord2D(seedX, seedY);

            if(!lakes.contains(seed)) {
                lakes.add(seed);
                grid[seed.x()][seed.y()].updateState(CellState.WATER);
            }
        }
    }

    public ForestCell[][] getGrid() {
        return grid;
    }

    public int getFPS()         { return this.fps; }
    public void setFPS(int fps) { this.fps = fps; }
}
