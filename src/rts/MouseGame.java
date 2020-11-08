package rts;

import gui.MouseController;
import gui.PhysicalGameStateMouseJFrame;
import gui.PhysicalGameStatePanel;

public class MouseGame extends Game {

    private PhysicalGameStateMouseJFrame w;

    MouseGame(GameSettings gameSettings) throws Exception {
        super(gameSettings);

        PhysicalGameStatePanel pgsp = new PhysicalGameStatePanel(gs);
        w = new PhysicalGameStateMouseJFrame("Game State Visualizer (Mouse)", 640, 640, pgsp);

        this.ai1 = new MouseController(w);
    }

    @Override
    public ActionStatistics start() throws Exception {
        return super.start(w);
    }
}
