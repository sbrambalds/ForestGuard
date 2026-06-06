package utils;

import java.util.Arrays;
import java.util.List;

public record Coord(int x, int y) {
    
    public boolean isValid(int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public List<Coord> neighbours() {
        List<Coord> list = Arrays.asList(
            new Coord(x + 1, y + 1),
            new Coord(x - 1, y - 1),
            new Coord(x - 1, y + 1),
            new Coord(x + 1, y - 1)
        );

        list.addAll(cardinalNeighbours());
        
        return list;
    }

    public List<Coord> cardinalNeighbours() {
        return Arrays.asList(
            new Coord(x, y + 1),
            new Coord(x - 1, y),
            new Coord(x, y - 1),
            new Coord(x + 1, y)
        );
    }
}