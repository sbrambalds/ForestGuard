package view;

import java.awt.Color;
import java.awt.Graphics;

import model.CellState;
import model.Config;
import model.ForestModel;

public class CellRenderer {

    private static final int STATION_SIZE = 5;
    private static final String[][] STATION_LAYOUT = {
        {"roof_corner_tl",  "roof_edge_top",    "roof_edge_top",    "roof_edge_top",    "roof_corner_tr"},
        {"roof_edge_left",  "roof_full",        "roof_full",        "roof_full",        "roof_edge_right"},
        {"roof_edge_left",  "roof_full",        "roof_full",        "roof_full",        "roof_edge_right"},
        {"roof_corner_bl",  "roof_edge_bottom", "roof_edge_bottom", "roof_edge_bottom", "roof_corner_br"},
        {"wall_window",     "wall_brick",       "wall_garage_door", "wall_brick",       "wall_brick"}
    };

    private final ForestModel model;
    private final SpriteRepository sprites;

    public CellRenderer(ForestModel model, SpriteRepository sprites) {
        this.model = model;
        this.sprites = sprites;
    }

    public void render(Graphics g, int i, int j) {
        int x = i * Config.CELL_SIZE;
        int y = j * Config.CELL_SIZE;
        int s = Config.CELL_SIZE;
        CellState state = model.getGrid()[i][j].getState();

        switch (state) {
            case WATER -> {
                g.setColor(Color.cyan);
                g.fillRect(x, y, s, s);
                g.drawImage(sprites.getWater(), x, y, s, s, null);
            }
            case STATION -> {
                int centerX = Config.GRID_WIDTH  / 2;
                int centerY = Config.GRID_HEIGHT / 2;
                int relX = i - (centerX - STATION_SIZE / 2);
                int relY = j - (centerY - STATION_SIZE / 2);
                g.drawImage(sprites.getStation(STATION_LAYOUT[relY][relX]), x, y, s, s, null);
            }
            case CHARGE_STATION -> {
                g.drawImage(sprites.getCStation(), x, y, s, s, null);
            }
            // case BURNING -> {
            //     g.setColor(Color.red);
            //     g.fillRect(x, y, s, s);
            // }
            default -> {
                g.drawImage(sprites.getGrass(), x, y, s, s, null);
                drawWaterTransitions(g, i, j);
            }
        }
    }

    public void renderTrees(Graphics g, int i, int j) {
        int x = i * Config.CELL_SIZE;
        int y = j * Config.CELL_SIZE;// - Config.CELL_SIZE;
        
        if (model.getGrid()[i][j].getState() == CellState.TREE) {
                g.drawImage(sprites.getTree(i, j), x, y, Config.CELL_SIZE, Config.CELL_SIZE, null);
        }
    }

    public void renderDrone(Graphics g, int posX, int posY) {
        g.drawImage(sprites.getSDrone(), posX * Config.CELL_SIZE, posY * Config.CELL_SIZE, Config.CELL_SIZE, Config.CELL_SIZE, null);
    }

    private void drawWaterTransitions(Graphics g, int i, int j) {
        int x = i * Config.CELL_SIZE;
        int y = j * Config.CELL_SIZE;
        int s = Config.CELL_SIZE;

        boolean N  = isWater(i,     j - 1);
        boolean S  = isWater(i,     j + 1);
        boolean W  = isWater(i - 1, j    );
        boolean E  = isWater(i + 1, j    );

        if (N) draw(g, "edge_top",    x, y, s);
        if (S) draw(g, "edge_bottom", x, y, s);
        if (W) draw(g, "edge_left",   x, y, s);
        if (E) draw(g, "edge_right",  x, y, s);

        if (S && E) draw(g, "corner_inner_tl", x, y, s);
        if (S && W) draw(g, "corner_inner_tr", x, y, s);
        if (N && E) draw(g, "corner_inner_bl", x, y, s);
        if (N && W) draw(g, "corner_inner_br", x, y, s);

        // if (!N && !W && NW) draw(g, "corner_outer_tl", x, y, s);
        // if (!N && !E && NE) draw(g, "corner_outer_tr", x, y, s);
        // if (!S && !W && SW) draw(g, "corner_outer_bl", x, y, s);
        // if (!S && !E && SE) draw(g, "corner_outer_br", x, y, s);

        if (N && S && W && E) draw(g, "island", x, y, s);
    }

    private void draw(Graphics g, String sprite, int x, int y, int s) {
        g.drawImage(sprites.getTransition(sprite), x, y, s, s, null);
    }

    private boolean isWater(int i, int j) {
        if (i < 0 || i >= Config.GRID_WIDTH || j < 0 || j >= Config.GRID_HEIGHT) return false;
        return model.getGrid()[i][j].getState() == CellState.WATER;
    }
}
