package view;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Config;

public class SpriteRepository {

    private static final String RES        = "/Users/rrambaldi/ForestGuard/src/main/resources/";
    private static final String TREE_PATH  = RES + "trees/";
    private static final String WATER_PATH = RES + "water/";
    private static final String STATION_PATH = RES + "station/";
    private static final String DRONES_PATH  = RES + "drones/";

    private final ArrayList<Image> trees = new ArrayList<>();
    private final List<List<Image>> fireSprites = new ArrayList<>();
    private final Map<String, Image> waterSprites = new HashMap<>();
    private final Map<String, Image> stationSprites = new HashMap<>();
    private final int[][] treeVariants = new int[Config.GRID_WIDTH][Config.GRID_HEIGHT];
    private Image grass;
    private Image water;
    private Image ffDrone;
    private Image sDrone;
    private Image cStation;

    public SpriteRepository() {
        loadSprites();
        assignTreeVariants();
    }

    private void loadSprites() {
        File[] treeFiles = new File(TREE_PATH + "singole").listFiles(f -> f.getName().endsWith(".png"));
        if (treeFiles != null) {
            java.util.Arrays.sort(treeFiles);
            for (File f : treeFiles) trees.add(load(f.getAbsolutePath()));
        }

        File[] fireFiles = new File(TREE_PATH + "burned").listFiles(f -> f.getName().endsWith(".png"));
        if (fireFiles != null) {
            java.util.Arrays.sort(fireFiles);
            List<Image> frames = new ArrayList<>();
            for (File f : fireFiles) {
                frames.add(load(f.getAbsolutePath()));
                if (frames.size() == 3) {
                    fireSprites.add(new ArrayList<>(frames));
                    frames.clear();
                }
            }
        }

        grass   = load(RES + "grass/grass_full.png");
        water   = load(RES + "water/water_full.png");
        ffDrone = load(DRONES_PATH + "drone_firefighter.png");
        sDrone  = load(DRONES_PATH + "drone_scout.png");
        cStation = load(STATION_PATH + "drone_charging_pad.png");

        for (String name : new String[]{
            "edge_top", "edge_bottom", "edge_left", "edge_right",
            "corner_inner_tl", "corner_inner_tr", "corner_inner_bl", "corner_inner_br",
            "corner_outer_tl", "corner_outer_tr", "corner_outer_bl", "corner_outer_br",
            "island"
        }) {
            waterSprites.put(name, load(WATER_PATH + name + ".png"));
        }

        for (String name : new String[]{
            "roof_corner_bl", "roof_corner_br", "roof_corner_tl",
            "roof_corner_tr", "roof_edge_bottom", "roof_edge_left",
            "roof_edge_right", "roof_edge_top", "roof_full", "wall_brick",
            "wall_garage_door", "wall_window"
        }) {
            stationSprites.put(name, load(STATION_PATH + name + ".png"));
        }
    }

    private void assignTreeVariants() {
        Random rand = new Random();
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                treeVariants[i][j] = rand.nextInt(trees.size());
            }
        }
    }

    private Image load(String path) {
        try {
            BufferedImage src = javax.imageio.ImageIO.read(new File(path));
            int s = Config.CELL_SIZE;
            BufferedImage scaled = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, s, s, null);
            g.dispose();
            return scaled;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sprite: " + path, e);
        }
    }

    public Image getTree(int i, int j)              { return trees.get(treeVariants[i][j]); }
    public Image getFireTree(int i, int j, int frame) { return fireSprites.get(treeVariants[i][j]).get(frame); }
    public Image getGrass()                         { return grass; }
    public Image getWater()                         { return water; }
    public Image getTransition(String name)         { return waterSprites.get(name); }
    public Image getStation(String name)            { return stationSprites.get(name); }
    public Image getFFDrone()                       { return ffDrone; }
    public Image getSDrone()                        { return sDrone; }
    public Image getCStation()                      { return cStation; }
}
