package model;

public enum CellState {
    TREE,
    EMPTY,
    BURNING,
    WATER,
    STATION,
    FF_DRONE,
    S_DRONE,
    CHARGE_STATION,
    WATER_STATION,
    WET_TREE;

    public String getName() {
        return this.toString().toLowerCase();
    }
}
