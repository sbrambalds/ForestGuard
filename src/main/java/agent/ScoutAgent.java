package agent;

import jason.asSemantics.Agent;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import model.Config;

public class ScoutAgent extends Agent {

    @Override
    public void loadInitialAS(String asSrc) throws Exception {
        String name = getTS().getAgArch().getAgName();
        int moves = Config.movesPerLevel(name);
        addInitialBel(Literal.parseLiteral("moves_per_level(" + moves + ")"));
        addInitialBel(Literal.parseLiteral("battery_level(" + Config.BATTERY_LEVELS + ", " + (moves - 1) + ")"));
        addInitialBel(ASSyntax.parseRule("dist(X1, Y1, X2, Y2, D) :- D = math.abs(X1 - X2) + math.abs(Y1 - Y2)."));
        Config.DIRECTIONS.forEach(d -> addInitialBel(Literal.parseLiteral("direction(" + d.x() + ", " + d.y() + ")")));
        super.loadInitialAS(asSrc);
    }

}
