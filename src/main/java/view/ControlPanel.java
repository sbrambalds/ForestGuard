package view;

import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class ControlPanel extends JPanel {

    public ControlPanel(int initialFps, Consumer<Integer> onSpeedChange, Consumer<Boolean> onPauseChange) {
        JLabel speedUp = new JLabel("Set speed");
        JSlider speed = new JSlider(JSlider.HORIZONTAL, 1, 60, initialFps);
        JLabel speedValue = new JLabel(initialFps + " FPS");
        speed.addChangeListener(e -> {
            int fps = speed.getValue();
            onSpeedChange.accept(fps);
            speedValue.setText(fps + " FPS");
        });

        JButton pause = new JButton("Pause");
        pause.addActionListener(e -> {
            boolean paused = pause.getText().equals("Pause");
            onPauseChange.accept(paused);
            pause.setText(paused ? "Resume" : "Pause");
        });

        this.add(speedUp);
        this.add(speed);
        this.add(speedValue);
        this.add(pause);
    }

}
