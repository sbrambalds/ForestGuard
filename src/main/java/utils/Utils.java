package utils;

import jason.NoValueException;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

public class Utils {
    public static Coord2D literalToCoord2D(Literal literal) {
        if (!literal.getTerm(0).isNumeric() || !literal.getTerm(1).isNumeric()) {
            throw new IllegalArgumentException("Cannot parse as Vector2D: " + literal);
        }
        try {
            return new Coord2D(
                    termToInteger(literal.getTerm(0)),
                    termToInteger(literal.getTerm(1))
            );
        } catch (NoValueException e) {
            throw new IllegalArgumentException("Cannot parse as Vector2D: " + literal);
        }
    }

    public static double termToNumber(Term term) throws NoValueException {
        if (!term.isNumeric()) {
            throw new IllegalArgumentException("Cannot parse as number: " + term);
        }
        return ((NumberTerm)term).solve();
    }

    public static int termToInteger(Term term) throws NoValueException {
        return (int) termToNumber(term);
    }

    public static Term numberToTerm(int value) {
        return new NumberTermImpl(value);
    }

    public static Term numberToTerm(double value) {
        return new NumberTermImpl(value);
    }
}
