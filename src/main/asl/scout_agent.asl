battery_level(100, 5).
status(scouting).

!start.

+!start <-
    .print("I am ready");
    !scouting.

+!scouting <-
    !move;
    //!map();
    !scouting.

+!move : battery_level(L, S) & L > 0 & position(X, Y) <-
    if(S == 0) {
        -+battery_level(L - 1, 5);
    } else {
        -+battery_level(L, S - 1);
    }; // batteria deve calare ogni N passi
    NewX = X + 1;
    -+position(NewX, Y);
    move(NewX, Y);
    .print("New Position X = ", X, ", Y = ", Y).

-!move : status(scouting) <-
    -+status(back_home).    // calcolare la distanza da percorrere per tornare
    //!back_to_station().     // alla base per vedere se la carica è sufficiente

//+!back_to_station() : <- ??
