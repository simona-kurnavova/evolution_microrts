package rts;

import gui.MouseController;
import gui.PhysicalGameStateMouseJFrame;
import gui.PhysicalGameStatePanel;

import java.util.List;

public class MouseGame extends Game {

    private PhysicalGameStateMouseJFrame w;

    MouseGame(GameSettings gameSettings) throws Exception {
        super(gameSettings);

        PhysicalGameStatePanel pgsp = new PhysicalGameStatePanel(gs);
        w = new PhysicalGameStateMouseJFrame("Game State Visualizer (Mouse)", 640, 640, pgsp);

        this.ai1 = new MouseController(w);
    }

    @Override
    public List<ActionStatistics> start() throws Exception {
        return super.start(w);
    }
}
