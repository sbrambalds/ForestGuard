water_tank(100).
status(idle).

target(X, Y) :- extinguish_fire(X, Y) & not obstacle(X, Y).
target(X, Y) :- cell(X, Y, burning) & not obstacle(X, Y).

at_station :- home(X, Y) & position(X, Y, _).

!start.

+!start <- !check_next.

+!check_next : status(idle) & target(Xf, Yf) <-
    -extinguish_fire(Xf, Yf)[source(_)];
    -+status(fight_fire);
    !go_to(Xf, Yf);
    !fight_fire;
    -+status(idle);
    !check_next.

+!check_next <-
    if (not at_station) {
        .send(station, achieve, waiting);
    };
    .wait(extinguish_fire(_, _) | cell(_, _, burning));
    !check_next.

+!consume_battery : battery_level(Level, Steps) & Level > 0 & position(X, Y, _) & moves_per_level(K) <-
    if(Steps == 0) {
        -+battery_level(Level - 1, K - 1);
    } else {
        -+battery_level(Level, Steps - 1);
    };
    .send(station, achieve, drone_state(X, Y, Level, Steps)).

+!use_water : water_tank(Lt) & battery_level(Battery, _) & position(X, Y, _) & wait_time(T) <-
    NewLt = Lt - 20;
    wait(T);
    -+water_tank(NewLt);
    .print("Water level: ", NewLt);
    if(NewLt == 0) {
        .send(station, achieve, water_state(X, Y, Battery));
    };
    .print("Fire extinguished!");
    .send(station, achieve, extinguished(X, Y));
    .findall(N, neighbour(N), Neighbours);
    .send(Neighbours, achieve, fire_handled(X, Y));
    -extinguish_fire(X, Y)[source(_)].

+!water_refilling : wait_time(T) <-
    .print("Refilling water tank...");
    wait(T);
    -+water_tank(100).

+!fight_fire : water_tank(W) & W > 0 & position(X, Y, burning) <-
    !use_water;
    !fight_fire.

+!fight_fire : water_tank(W) & W > 0 & cell(Xb, Yb, burning) & not obstacle(Xb, Yb) <-
    !go_to(Xb, Yb);
    !fight_fire.

+!fight_fire : water_tank(W) & W > 0 <- true.

+!fight_fire <- !check_next.

+!go_to(XD, YD) : position(X, Y, water_station) & X == XD & Y == YD & wait_time(T) & moves_per_level(K)<-
    .print("Recharging battery and refilling water tank...");
    .send(station, achieve, arrived_home);
    wait(T);
    -+battery_level(100, K - 1);
    -+water_tank(100).

+!go_to(XD, YD) : position(X, Y, _) & X == XD & Y == YD <- true.

+!go_to(XD, YD) : position(X, Y, _) <-
    .findall(
        opt(D, NX, NY),
        (direction(Dx, Dy) & NX = X+Dx & NY = Y+Dy & not border(NX, NY) & not obstacle(NX, NY) & dist(NX, NY, XD, YD, D)),
        Options
    );
    if (Options == []) {
        wait(1);
    } else {
        .sort(Options, [opt(_, NextX, NextY) | _]);
        move(NextX, NextY);
        !consume_battery;
    };
    !go_to(XD, YD).

+!back_to_station(XD, YD) : not status(recharge) <-
    .drop_intention(check_next);
    .drop_intention(go_refill(_, _));
    -+status(recharge);
    !go_to(XD, YD);
    -+status(idle);
    !!check_next.

+!back_to_station(_, _) <- true.

+!go_refill(X, Y) : not status(recharge) <-
    .drop_intention(check_next);
    -+status(refill);
    !go_to(X, Y);
    !water_refilling;
    -+status(idle);
    !!check_next.

+!go_refill(_, _) <- true.

+!fire_handled(X, Y) <-
    -extinguish_fire(X, Y)[source(_)];
    .succeed_goal(go_to(X, Y)).

+cell(X, Y, burning): not extinguish_fire(X, Y) <-
    +extinguish_fire(X, Y)[source(station)].
