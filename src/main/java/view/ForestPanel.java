package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import model.Config;
import model.ForestModel;
import utils.Coord2D;

public class ForestPanel extends JPanel {

    private final CellRenderer renderer;
    private BufferedImage buffer;
    private final Map<String, Coord2D> agentsPoses;
    private int fireFrame = 0;

    private final Timer fireTimer = new Timer(200, e -> {
        fireFrame = (fireFrame + 1) % 3;
        repaint();
    });

    public ForestPanel(ForestModel model, Map<String, Coord2D> agentsPoses) {
        SpriteRepository sprites = new SpriteRepository();
        this.renderer = new CellRenderer(model, sprites);
        this.agentsPoses = agentsPoses;
        setPreferredSize(new Dimension(
            Config.GRID_WIDTH * Config.CELL_SIZE,
            Config.GRID_HEIGHT * Config.CELL_SIZE
        ));
        fireTimer.start();
    }

    public void setPaused(boolean paused) {
        if (paused) fireTimer.stop(); else fireTimer.start();
    }

    public void initBuffer() {
        buffer = new BufferedImage(
            Config.GRID_WIDTH * Config.CELL_SIZE,
            Config.GRID_HEIGHT * Config.CELL_SIZE,
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics bg = buffer.getGraphics();
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                renderer.render(bg, i, j);
            }
        }
        initOverlay(bg);
        bg.dispose();
    }

    private void initOverlay(Graphics g) {
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                renderer.renderTrees(g, i, j);
            }
        }
    }

    public void updateCells(List<Coord2D> cells) {
        if (buffer == null || cells.isEmpty()) return;
        Graphics bg = buffer.getGraphics();
        for (Coord2D c : cells) {
            renderer.render(bg, c.x(), c.y());
        }
        bg.dispose();
    }

    private void renderDrones(Graphics g) {
        agentsPoses.forEach((String name, Coord2D pos) -> {
            if(name.contains("scout")) renderer.renderDrone(g, pos.x(), pos.y());
            if(name.contains("firefighter")) renderer.renderFireFighter(g, pos.x(), pos.y());
        });
    }

    private void renderFire(Graphics g) {
        for (int i = 0; i < Config.GRID_WIDTH; i++) {
            for (int j = 0; j < Config.GRID_HEIGHT; j++) {
                renderer.renderFireTrees(g, i, j, fireFrame);
            }
        }
    }

    private void renderVisionOverlay(Graphics g) {
        Set<Coord2D> visible = new HashSet<>();
        agentsPoses.values().forEach(pos -> visible.addAll(pos.visionRadius()));
        for (Coord2D cell : visible) {
            if (cell.isValid()) {
                renderer.renderVisionHighlight(g, cell.x(), cell.y());
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (buffer != null) {
            g.drawImage(buffer, 0, 0, null);
            renderFire(g);
            renderVisionOverlay(g);
            renderDrones(g);
        }
    }

    public void update() {
        try {
            SwingUtilities.invokeAndWait(this::repaint);
        } catch (InterruptedException | InvocationTargetException e) {
        }
    }
}
