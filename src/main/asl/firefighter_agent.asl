battery_level(100, 1).
water_tank(100).
status(idle).

direction(1, 0).
direction(-1, 0).
direction(0, 1).
direction(0, -1).

!start.

+!start <- !check_next.

+!check_next : extinguish_fire(Xf, Yf) & status(idle) & not obstacle(Xf, Yf) <-
    -extinguish_fire(Xf, Yf)[source(station)];
    -+status(fight_fire);
    !go_to(Xf, Yf);
    !fight_fire;
    -+status(idle);
    !check_next.

+!check_next : cell(Xf, Yf, burning) & not obstacle(Xf, Yf) & status(idle) <-
    -+status(fight_fire);
    !go_to(Xf, Yf);
    !fight_fire;
    -+status(idle);
    !check_next.

+!check_next : home(Xh, Yh) & position(Xh, Yh, _) <-
    .wait(extinguish_fire(_, _) | cell(_, _, burning));
    !check_next.

+!check_next <-
    .send(station, tell, idle);
    .wait(extinguish_fire(_, _) | cell(_, _, burning));
    !check_next.

+!consume_battery : battery_level(Level, Steps) & Level > 0 & position(X, Y, _) <-
    if(Steps == 0) {
        -+battery_level(Level - 1, 1);
    } else {
        -+battery_level(Level, Steps - 1);
    };
    .send(station, tell, drone_state(X, Y, Level, Steps)).

+!use_water : water_tank(Lt) & battery_level(Battery, _) & position(X, Y, _) & wait_time(T) <-
    NewLt = Lt - 10;
    .wait(T);
    -+water_tank(NewLt);
    .print("Water level: ", NewLt);
    if(NewLt == 0) {
        .send(station, tell, water_state(X, Y, Battery));
    };
    .print("Fire extinguished!");
    fire_extinguished;
    .send(station, tell, extinguished(X, Y));
    .findall(N, neighbour(N), Neighbours);
    .send(Neighbours, tell, fire_handled(X, Y));
    -extinguish_fire(X, Y)[source(_)].

+!water_refilling : wait_time(T) <-
    .print("Refilling water tank...");
    .wait(T);
    -+water_tank(100).

+!fight_fire : water_tank(W) & W > 0 & position(X, Y, burning) <-
    !use_water;
    !fight_fire.

+!fight_fire : water_tank(W) & W > 0 & position(X, Y, _) <-
    .findall(
        opt(Xb, Yb),
        (cell(Xb, Yb, burning) & not obstacle(Xb, Yb)),
        Burning
    );
    if(Burning \== []){
        .sort(Burning, [opt(Xb, Yb) | _]);
        !go_to(Xb, Yb);
        !fight_fire;
    }.

+!fight_fire <- !check_next.

+!go_to(XD, YD) : position(X, Y, water_station) & X == XD & Y == YD & wait_time(T) <-
    .print("Recharging battery and refilling water tank...");
    .send(station, tell, arrived_home);
    .wait(T);
    -+battery_level(100, 1);
    -+water_tank(100).

+!go_to(XD, YD) : position(X, Y, _) & X == XD & Y == YD <- true.

+!go_to(XD, YD) : position(X, Y, _) <-
    .findall(
        opt(D, NX, NY),
        (direction(Dx, Dy) & NX = X+Dx & NY = Y+Dy & not border(NX, NY) & not obstacle(NX, NY) & D = math.abs(NX-XD) + math.abs(NY-YD)),
        Options
    );
    .sort(Options, [opt(_, NextX, NextY) | _]);
    move(NextX, NextY);
    !consume_battery;
    !go_to(XD, YD).

+back_to_station(XD, YD)[source(A)] : not status(recharge) <-
    .drop_intention(check_next);
    .drop_intention(go_refill(_, _));
    .abolish(go_refill(_, _));
    -+status(recharge);
    !go_to(XD, YD);
    -+status(idle);
    -back_to_station(XD, YD)[source(A)];
    !!check_next.

+back_to_station(XD, YD)[source(A)] <-
    -back_to_station(XD, YD)[source(A)].

+go_refill(X, Y)[source(A)] : not status(recharge) <-
    .drop_intention(check_next);
    -+status(refill);
    !go_to(X, Y);
    !water_refilling;
    -+status(idle);
    -go_refill(X, Y)[source(A)];
    !!check_next.

+go_refill(X, Y)[source(A)] <-
    -go_refill(X, Y)[source(A)].

+cell(X, Y, burning): not extinguish_fire(X, Y) <-
    +extinguish_fire(X, Y)[source(station)].

+fire_handled(X, Y)[source(A)] <-
    -fire_handled(X, Y)[source(A)];
    -extinguish_fire(X, Y)[source(_)];
    .succeed_goal(go_to(X, Y)).