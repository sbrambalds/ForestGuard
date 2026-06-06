package model;

class ForestCell {
    private CellState state;

    public ForestCell(CellState state) {
        this.state = state;
    }

    public CellState getState() {
        return state;
    }

    public void updateState(CellState newState) {
        this.state = newState;
    }
}
