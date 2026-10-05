package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.Config;

public record Coord2D(int x, int y) {

    private static final int RADIUS = 3;
    
    public boolean isValid() {
        return x >= 0 && x < Config.GRID_WIDTH && y >= 0 && y < Config.GRID_HEIGHT;
    }

    public List<Coord2D> neighbours() {
        List<Coord2D> list = new ArrayList<>(Arrays.asList(
            new Coord2D(x + 1, y + 1),
            new Coord2D(x - 1, y - 1),
            new Coord2D(x - 1, y + 1),
            new Coord2D(x + 1, y - 1)
        ));

        list.addAll(cardinalNeighbours());

        return list;
    }

    public List<Coord2D> cardinalNeighbours() {
        return Arrays.asList(
            new Coord2D(x, y + 1),
            new Coord2D(x - 1, y),
            new Coord2D(x, y - 1),
            new Coord2D(x + 1, y)
        );
    }

    public List<Coord2D> visionRadius() {
        List<Coord2D> list = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                if (dx == 0 && dy == 0) continue;
                if (dx * dx + dy * dy <= RADIUS * RADIUS) {
                    list.add(new Coord2D(x + dx, y + dy));
                }
            }
        }
        return list;
    }

        @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }

    public Coord2D times(int factor) {
        return new Coord2D(x * factor, y * factor);
    }

    public Coord2D plus(Coord2D other) {
        return new Coord2D(x + other.x, y + other.y);
    }

    public Coord2D minus(Coord2D other) {
        return new Coord2D(x - other.x, y - other.y);
    }

    public Coord2D plus(int x, int y) {
        return new Coord2D(this.x + x, this.y + y);
    }

    public Coord2D minus(int x, int y) {
        return new Coord2D(this.x - x, this.y - y);
    }
}