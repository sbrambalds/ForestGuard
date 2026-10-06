status(scouting).
came_from(-1, -1).

!start.

+!start <-
    .print("Start scouting...");
    !scouting.

+!scouting : status(back_home) <- true.

+!scouting <-
    !move;
    !scouting.

+!move : position(X, Y, _) & status(scouting) <-
    !choose_step(NextX, NextY);
    move(NextX, NextY);
    !send_mapping;
    -+came_from(X, Y);
    !consume_battery.

+!consume_battery : battery_level(Level, Steps) & Level > 0 & position(X, Y, _) & moves_per_level(K) <-
    if(Steps == 0) {
        -+battery_level(Level - 1, K - 1);
    } else {
        -+battery_level(Level, Steps - 1);
    };
    .send(station, achieve, drone_state(X, Y, Level, Steps)).

+!go_to(XD, YD) : status(back_home) & position(X, Y, _) & X == XD & Y == YD & wait_time(T) & moves_per_level(K) <-
    .print("Arrived. Recharging...");
    wait(T);
    .print("Fully charged!");
    -+battery_level(100, K - 1);
    -+status(scouting);
    -+came_from(XD, YD);
    !go_to(XD, YD);
    -last_pos(_, _);
    !scouting.

+!send_mapping <- 
    .findall(map(Xc, Yc, State), cell(Xc, Yc, State), Cells);
    .send(station, achieve, map_update(Cells));
    .abolish(cell(_, _, _)).

+!choose_step(NewX, NewY): position(X, Y, _) & came_from(PX, PY) & bound(Xmin, Xmax, Ymin, Ymax) <-
    .findall(free(NX, NY), (direction(DX, DY) & NX = X + DX & NY = Y + DY & not(obstacle(NX, NY)) & not(border(NX, NY)) & NX < Xmax & NX > Xmin & NY < Ymax & NY > Ymin), L);
    utils.visit_next(L, NewX, NewY).

+!go_to(XD, YD): status(scouting) & position(X, Y, _) & X == XD & Y == YD <- 
    -+came_from(XD, YD).

+!go_to(XD, YD): position(X, Y, _) <-
    .findall(
        opt(D, NX, NY),
        (direction(Dx, Dy) & NX = X+Dx & NY = Y+Dy & not border(NX, NY) & not obstacle(NX, NY) & dist(NX, NY, XD, YD, D)),
        Options
    );
    if (Options == []) {
        wait(1);
    } else {
        .sort(Options, [opt(_, NextX, NextY) | _]);
        !send_mapping;
        move(NextX, NextY);
        !consume_battery;
    };
    !go_to(XD, YD).

+!back_to_station(XD, YD)[source(station)]: position(X, Y, _) <-
    .print("Coming back to recharge...");
    -+last_pos(X, Y);
    -+status(back_home);
    !go_to(XD, YD).