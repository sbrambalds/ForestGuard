package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.Coord2D;

public final class Config {
        public static final int GRID_WIDTH = 70;
        public static final int GRID_HEIGHT = 40;
        public static final int CENTER_X = GRID_WIDTH / 2;
        public static final int CENTER_Y = GRID_HEIGHT / 2;
        public static final int TREES_NUMBER = 1000;
        public static final int ROCKS_NUMBER = 800;
        public static final int LAKES_NUMBER = 10;
        public static final int CELL_SIZE = 21;
        public static final int BATTERY_FACTOR = 2;
        public static final int BATTERY_LEVELS = 100;
        public static final double SPREAD_PROB = 0.7;
        public static final int INITIAL_FPS = 1;
        public static final long FIRE_DELAY = 60;
        public static final long SPREAD_DELAY = FIRE_DELAY / 3;
        public static final long BURNING_DELAY = 300;
        public static final long DRY_DELAY = 200;
        public static final long WAIT_TICKS = 5;

        public static final Map<String, Coord2D> SCOUT_STATIONS = Map.of(
                "scoutN", new Coord2D(CENTER_X + 2, CENTER_Y + 2),
                "scoutE", new Coord2D(CENTER_X + 2, CENTER_Y - 2),
                "scoutW", new Coord2D(CENTER_X - 2, CENTER_Y + 2),
                "scoutS", new Coord2D(CENTER_X - 2, CENTER_Y - 2)
        );

        public static final List<Coord2D> DIRECTIONS = List.of(
                new Coord2D(1, 0), 
                new Coord2D(-1, 0), 
                new Coord2D(0, 1), 
                new Coord2D(0, -1)
        );

        public static final Map<String, Coord2D> FIREFIGHTER_STATIONS = Map.of(
                "firefighter1", new Coord2D(CENTER_X + 2, CENTER_Y),
                "firefighter2", new Coord2D(CENTER_X - 2, CENTER_Y),
                "firefighter3", new Coord2D(CENTER_X, CENTER_Y + 2),
                "firefighter4", new Coord2D(CENTER_X, CENTER_Y - 2)
        );

        public static final Map<String, Coord2D> STATIONS = new HashMap<>();
        static {
                STATIONS.putAll(SCOUT_STATIONS);
                STATIONS.putAll(FIREFIGHTER_STATIONS);
        }

        public static Coord2D homeOf(String agent) {
                return STATIONS.get(agent);
        }

        private static int farthestCorner(String agent) {
                Coord2D home = homeOf(agent);
                int dx = Math.max(home.x(), GRID_WIDTH - 1 - home.x());
                int dy = Math.max(home.y(), GRID_HEIGHT - 1 - home.y());
                return dx + dy;
        }

        public static int movesPerLevel(String agent) {
                int ffMoves = (int) Math.ceil(BATTERY_FACTOR * farthestCorner(agent) / (double) BATTERY_LEVELS);
                return SCOUT_STATIONS.containsKey(agent) ? 2 * ffMoves : ffMoves;
        }
}
