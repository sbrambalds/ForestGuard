battery_level(100, 1).
status(scouting).

offset(1, 0).
offset(-1, 0).
offset(0, 1).
offset(0, -1).

!start.

+!start <-
    .print("Start scouting...");
    !scouting.

+!scouting : status(back_home) <- true.

+!scouting <-
    !move;
    !scouting.

+!move : position(X, Y, _) & status(scouting) <-
    !choose_step(NewX, NewY);
    move(NewX, NewY);
    !send_mapping;
    -+came_from(X, Y);
    !consume_battery.

+!consume_battery : battery_level(Level, Steps) & Level > 0 & position(X, Y, _) <-
    if(Steps == 0) {
        -+battery_level(Level - 1, 1);
    } else {
        -+battery_level(Level, Steps - 1);
    };
    .send(station, tell, drone_state(X, Y, Level, Steps)).

-!move : status(scouting) <- -+status(back_home).

+back_to_station(XD, YD)[source(A)] <-
    .print("Coming back to recharge...");
    -+status(back_home);
    -back_to_station(XD, YD)[source(A)];
    !come_back(XD, YD).

+!come_back(XD, YD) : position(X, Y, _) & X == XD & Y == YD & recharge_time(T) <-
    .print("Arrived. Recharging...");
    .wait(T);
    .print("Fully charged!");
    -+battery_level(100, 1);
    -+status(scouting);
    -+came_from(XD, YD);
    !scouting.

+!come_back(XD, YD) : position(X, Y, _) & X \== XD <-
    if(X > XD) { PrefX = X - 1; } else { PrefX = X + 1; };
    if(not obstacle(PrefX, Y)) {
        !send_mapping;
        move(PrefX, Y);
    } else {
        if(Y > YD) { AltY = Y - 1; } else { AltY = Y + 1; };
        !send_mapping;
        move(X, AltY);
    };
    !consume_battery;
    !come_back(XD, YD).

+!come_back(XD, YD) : position(X, Y, _) & Y \== YD <-
    if(Y > YD) { PrefY = Y - 1; } else { PrefY = Y + 1; };
    if(not obstacle(X, PrefY)) {
        !send_mapping;
        move(X, PrefY);
    } else {
        if(X > XD) { AltX = X - 1; } else { AltX = X + 1; };
        !send_mapping;
        move(AltX, Y);
    };
    !consume_battery;
    !come_back(XD, YD).

+!send_mapping <- 
    .findall(map(Xc, Yc, State), cell(Xc, Yc, State), Cells);
    .send(station, tell, map_update(Cells)).

+!choose_step(NewX, NewY): position(X, Y, _) & came_from(PX, PY) & bound(Xmin, Xmax, Ymin, Ymax) <-
    SX = X + (X - PX);
    SY = Y + (Y - PY);
    .findall(free(NX, NY), (offset(DX, DY) & NX = X + DX & NY = Y + DY & not(obstacle(NX, NY)) & not(border(NX, NY)) & NX < Xmax & NX > Xmin & NY < Ymax & NY > Ymin), L);

    if(.member(free(SX, SY), L)) {
        .concat(L, [free(SX,SY), free(SX,SY), free(SX,SY), free(SX,SY)], WeightedL);
    } else {
        WeightedL = L;
    };

    if(WeightedL \== []) {
        .random(WeightedL, Pos);
        Pos = free(NewX, NewY);
    } else {
        NewX = PX;
        NewY = PY;
    }.

+!choose_step(NewX, NewY): position(X, Y, _) & bound(Xmin, Xmax, Ymin, Ymax) <-
    .findall(free(NX, NY), (offset(DX, DY) & NX = X + DX & NY = Y + DY & not(obstacle(NX, NY)) & not(border(NX, NY)) & NX < Xmax & NX > Xmin & NY < Ymax & NY > Ymin), L);
    .random(L, Pos);
    Pos = free(NewX, NewY).
