status(scouting).
came_from(-1, -1).
margin(20).
max_wait(3).
waited(0).

low_battery :- battery_level(L, S) & moves_per_level(K) & margin(M)
               & position(X, Y, _) & home(Xh, Yh) & dist(X, Y, Xh, Yh, D)
               & L * K + S <= D + M.

!start.

+!start <-
    .print("Start scouting...");
    !scouting.

+!scouting : low_battery & home(Xh, Yh) <-
    !back_to_station(Xh, Yh);
    !scouting.

+!scouting <-
    !move;
    !scouting.

+!move : position(X, Y, _) & status(scouting) <-
    !choose_step(NextX, NextY);
    move(NextX, NextY);
    !send_mapping;
    -+came_from(X, Y);
    !consume_battery.

+!consume_battery : battery_level(Level, Steps) & Level > 0 & moves_per_level(K) <-
    if(Steps == 0) {
        -+battery_level(Level - 1, K - 1);
    } else {
        -+battery_level(Level, Steps - 1);
    }.

+!send_mapping <- 
    .findall(map(Xc, Yc, State), cell(Xc, Yc, State), Cells);
    .send(station, achieve, map_update(Cells));
    .abolish(cell(_, _, _)).

+!choose_step(NewX, NewY): position(X, Y, _) & came_from(PX, PY) & bound(Xmin, Xmax, Ymin, Ymax) <-
    .findall(free(NX, NY), (direction(DX, DY) & NX = X + DX & NY = Y + DY & not(obstacle(NX, NY)) & not(border(NX, NY)) & NX < Xmax & NX > Xmin & NY < Ymax & NY > Ymin), L);
    utils.visit_next(L, NewX, NewY).

+!go_to(XD, YD) : status(back_home) & position(X, Y, _) & X == XD & Y == YD & wait_time(T) & moves_per_level(K) <-
    .print("Arrived. Recharging...");
    wait(T);
    .print("Fully charged!");
    -+battery_level(100, K - 1);
    -+status(scouting);
    ?last_pos(Xl, Yl);
    !go_to(Xl, Yl);
    -last_pos(_, _).

+!go_to(XD, YD): status(scouting) & position(X, Y, _) & X == XD & Y == YD <- 
    -+came_from(XD, YD).

+!go_to(XD, YD): position(X, Y, _) & dist(X, Y, XD, YD, Dcur) & waited(W) & max_wait(MW) <-
    .findall(
        opt(D, NX, NY),
        (direction(Dx, Dy) & NX = X+Dx & NY = Y+Dy & not border(NX, NY) & not obstacle(NX, NY) & dist(NX, NY, XD, YD, D)),
        Options
    );
    .findall(opt(D, NX, NY), (.member(opt(D, NX, NY), Options) & D < Dcur), Closer);
    if (Closer \== []) {
        .min(Closer, opt(_, NextX, NextY));
        !step(NextX, NextY);
    } else {
        if (Options \== [] & W >= MW) {
            .min(Options, opt(_, NextX, NextY));
            !step(NextX, NextY);
        } else {
            wait(1);
            -+waited(W + 1);
        };
    };
    !go_to(XD, YD).

+!step(NextX, NextY) <-
    -+waited(0);
    !send_mapping;
    move(NextX, NextY);
    !consume_battery.

+!back_to_station(XD, YD) : position(X, Y, _) <-
    .print("Coming back to recharge...");
    -+last_pos(X, Y);
    -+status(back_home);
    !go_to(XD, YD).