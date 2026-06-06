package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import utils.Coord;

public final class ForestGrid {

    private final Random rand = new Random();
    private ForestCell[][] grid = new ForestCell[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    private final Queue<Coord> lakes = new LinkedList<>();

    public ForestGrid() {
        grid = new ForestCell[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    }

    public void initForest() {
        for (ForestCell[] row: grid) {
            Arrays.fill(row, new ForestCell(CellState.EMPTY));
        }
        generateLakes();
        generateRocks();
        generateTrees();
    }

    private void generateLakes() {
        double prob = rand.nextDouble();
        setLakeSeeds();

        while (!lakes.isEmpty()) { 
            Coord seed = lakes.poll();

            for (Coord pos : seed.cardinalNeighbours()) {
                ForestCell neighbour = grid[pos.x()][pos.y()];

                if(prob > 0.6 && neighbour.getState() == CellState.EMPTY && pos.isValid(Config.GRID_WIDTH, Config.GRID_HEIGHT)) {
                    grid[pos.x()][pos.y()].updateState(CellState.WATER);
                    lakes.add(pos);
                }
            }
        }
    }

    private void generateRocks() {
        for (int i = 0; i < Config.ROCKS_NUMBER; i++) {
            Coord seed = new Coord(rand.nextInt(0, Config.GRID_WIDTH), rand.nextInt(0, Config.GRID_HEIGHT));

            ForestCell selectedCell = grid[seed.x()][seed.y()];
            List<CellState> neighbourSelectedCells = new ArrayList<>();

            for (Coord neighbour : seed.neighbours()) {
                neighbourSelectedCells.add(grid[neighbour.x()][neighbour.y()].getState());
            }

            if(selectedCell.getState() == CellState.EMPTY && !neighbourSelectedCells.contains(CellState.ROCK)) {
                selectedCell.updateState(CellState.ROCK);
            }
        }
    }

    private void generateTrees() {
        for (int i = 0; i < Config.TREES_NUMBER; i++) {
            Coord seed = new Coord(rand.nextInt(0, Config.GRID_WIDTH), rand.nextInt(0, Config.GRID_HEIGHT));

            ForestCell selectedCell = grid[seed.x()][seed.y()];

            if(selectedCell.getState() == CellState.EMPTY) {
                selectedCell.updateState(CellState.TREE);
            } 
        }
    }

    private void setLakeSeeds() {
        for (int i = 0; i < Config.LAKES_NUMBER; i++) {
            int seedX = rand.nextInt(0, Config.GRID_WIDTH);
            int seedY = rand.nextInt(0, Config.GRID_HEIGHT);

            Coord seed = new Coord(seedX, seedY);

            if(!lakes.contains(seed)) {
                lakes.add(seed);
                grid[seed.x()][seed.y()].updateState(CellState.WATER);
            }
        }
    }
}
