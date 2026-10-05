package utils;

import java.util.Random;

import jason.asSemantics.Agent;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import static utils.Utils.literalToCoord2D;

public class visit_next extends DefaultInternalAction {

    private final Random rand = new Random();

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        Agent agent = ts.getAg();
        ListTerm perceivedCells = (ListTerm) args[0];

        Literal cameFrom = agent.findBel(Literal.parseLiteral("came_from(X, Y)"), new Unifier());
        Coord2D prevPos = literalToCoord2D(cameFrom);

        int prevX = prevPos.x();
        int prevY = prevPos.y();

        long bestStep = Long.MAX_VALUE;
        int nextX = 0;
        int nextY = 0;
        int tieCount = 0;
        boolean hasNonBacktrack = false;

        long fallbackStep = Long.MAX_VALUE;
        int fallbackX = 0;
        int fallbackY = 0;
        int fallbackTieCount = 0;

        for (Term term : perceivedCells) {
            Structure cell = (Structure) term;
            int cx = (int) ((NumberTerm) cell.getTerm(0)).solve();
            int cy = (int) ((NumberTerm) cell.getTerm(1)).solve();

            Literal visitedPos = agent.findBel(Literal.parseLiteral("visited(" + "cx, " + "cy, " + new VarTerm("Step")+")"), un.clone());

            long step;
            if (visitedPos != null) {
                step = (long) ((NumberTerm) visitedPos.getTerm(2)).solve() + 1;
                agent.delBel(visitedPos);
            } else {
                step = 0;
            }

            agent.addBel(Literal.parseLiteral("visited(" + cx + ", " + cy + ", " + step + ")"));

            if (step < fallbackStep) {
                fallbackStep = step;
                fallbackX = cx;
                fallbackY = cy;
                fallbackTieCount = 1;
            } else if (step == fallbackStep) {
                fallbackTieCount++;
                if (rand.nextInt(fallbackTieCount) == 0) {
                    fallbackX = cx;
                    fallbackY = cy;
                }
            }

            if (!(cx == prevX && cy == prevY)) {
                hasNonBacktrack = true;
                if (step < bestStep) {
                    bestStep = step;
                    nextX = cx;
                    nextY = cy;
                    tieCount = 1;
                } else if (step == bestStep) {
                    tieCount++;
                    if (rand.nextInt(tieCount) == 0) {
                        nextX = cx;
                        nextY = cy;
                    }
                }
            }
        }

        if (!hasNonBacktrack) {
            nextX = fallbackX;
            nextY = fallbackY;
        }

        return un.unifies(args[1], new NumberTermImpl(nextX))
            && un.unifies(args[2], new NumberTermImpl(nextY));
    }

}
