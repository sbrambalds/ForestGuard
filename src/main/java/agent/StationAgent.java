package agent;

import jason.asSemantics.Agent;
import jason.asSyntax.Literal;
import model.Config;

public class StationAgent extends Agent {
    
    @Override
    public void loadInitialAS(String asSrc) throws Exception {
        Config.STATIONS.forEach((name, pos) -> {
            addInitialBel(Literal.parseLiteral("charge_station(" + name + ", " + pos.x() + ", " + pos.y() + ")"));
            addInitialBel(Literal.parseLiteral("moves_per_level(" + name + ", " + Config.movesPerLevel(name) + ")"));
        });
        Config.FIREFIGHTER_STATIONS.keySet().forEach(name -> {
            String index = name.substring("firefighter".length());
            addInitialBel(Literal.parseLiteral("firefighter_name(" + index + ", " + name + ")"));
            addInitialBel(Literal.parseLiteral("at_base(" + name + ")"));
        });
        super.loadInitialAS(asSrc);
    }
    
}
