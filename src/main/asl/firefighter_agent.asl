water_tank(100).
status(idle).
margin(20).

low_battery :- battery_level(L, S) & moves_per_level(K) & margin(M)
               & position(X, Y, _) & home(Xh, Yh) & dist(X, Y, Xh, Yh, D)
               & L * K + S <= D + M.

water_reachable(Xw, Yw) :- battery_level(L, S) & moves_per_level(K) & margin(M)
    & position(X, Y, _) & home(Xh, Yh)
    & dist(X, Y, Xw, Yw, Dw) & dist(Xw, Yw, Xh, Yh, Dwh) & dist(X, Y, Xh, Yh, Dh)
    & Dw < Dh & L * K + S > Dw + Dwh + M.

taken(X, Y) :- claimed(X, Y, _).

target(X, Y) :- extinguish_fire(X, Y) & not obstacle(X, Y) & not taken(X, Y).
target(X, Y) :- cell(X, Y, burning) & not obstacle(X, Y) & not taken(X, Y).

nearest_target(X, Y) :-
    position(Px, Py, _) & target(X, Y) & dist(Px, Py, X, Y, D) &
    not (target(X2, Y2) & dist(Px, Py, X2, Y2, D2) & D2 < D).

nearest_burning(X, Y) :-
    position(Px, Py, _) & cell(X, Y, burning) & not obstacle(X, Y) & not taken(X, Y)
    & dist(Px, Py, X, Y, D) &
    not (cell(X2, Y2, burning) & not obstacle(X2, Y2) & not taken(X2, Y2)
         & dist(Px, Py, X2, Y2, D2) & D2 < D).

at_station :- home(X, Y) & position(X, Y, _).

!start.

+!start <- !check_next.

+!check_next : status(idle) & nearest_target(Xf, Yf) <-
    -extinguish_fire(Xf, Yf)[source(_)];
    -+status(fight_fire);
    !claim(Xf, Yf);
    !go_to(Xf, Yf);
    !fight_fire;
    -my_target(_, _);
    -+status(idle);
    !check_next.

+!check_next <-
    if (not at_station & status(idle)) {
        .send(station, achieve, waiting);
    };
    .wait(extinguish_fire(_, _) | cell(_, _, burning));
    !check_next.

+!consume_battery : battery_level(Level, Steps) & Level > 0 & moves_per_level(K) <-
    if(Steps == 0) {
        -+battery_level(Level - 1, K - 1);
    } else {
        -+battery_level(Level, Steps - 1);
    };
    if (low_battery & not status(recharge) & home(Xh, Yh)) {
        -+status(recharge);
        !!recharge_trip(Xh, Yh);
    }.

+!use_water : water_tank(Lt) & position(X, Y, _) & wait_time(T) <-
    NewLt = Lt - 20;
    wait(T);
    -+water_tank(NewLt);
    fire_extinguished(X, Y);
    .print("Water level: ", NewLt);
    if(NewLt == 0) {
        .send(station, achieve, water_state(X, Y));
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

+!fight_fire : water_tank(W) & W > 0 & nearest_burning(Xb, Yb) <-
    !claim(Xb, Yb);
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

@bts[atomic]
+!back_to_station(XD, YD) : not status(recharge) <-
    -+status(recharge);
    !!recharge_trip(XD, YD).

+!back_to_station(_, _) <- true.

+!recharge_trip(XD, YD) <-
    .send(station, achieve, going_home);
    .drop_intention(check_next);
    .drop_intention(go_refill(_, _));
    !release;
    !go_to(XD, YD);
    -+status(idle);
    !!check_next.

+!refill_at(Xw, Yw) : water_reachable(Xw, Yw) <- !go_refill(Xw, Yw).

+!refill_at(_, _) : home(Xh, Yh) <- !back_to_station(Xh, Yh).

+!go_refill(X, Y) : not status(recharge) <-
    .drop_intention(check_next);
    !release;
    -+status(refill);
    !go_to(X, Y);
    !water_refilling;
    -+status(idle);
    !!check_next.

+!go_refill(_, _) <- true.

+!fire_handled(X, Y) <-
    -extinguish_fire(X, Y)[source(_)];
    .abolish(claimed(X, Y, _));
    -my_target(X, Y);
    .succeed_goal(go_to(X, Y)).

+!claim(X, Y) : position(Px, Py, _) & dist(Px, Py, X, Y, D) <-
    -+my_target(X, Y);
    .findall(N, neighbour(N), Ns);
    .send(Ns, achieve, claimed(X, Y, D)).

+!claimed(X, Y, D)[source(A)] : my_target(X, Y) & position(Px, Py, _)
        & dist(Px, Py, X, Y, MyD) & .my_name(Me) & (MyD < D | (MyD == D & Me < A)) <-
    .send(A, achieve, claimed(X, Y, MyD)).

+!claimed(X, Y, _)[source(A)] <-
    +claimed(X, Y, A);
    -extinguish_fire(X, Y)[source(_)];
    if (my_target(X, Y)) {
        -my_target(X, Y);
        .succeed_goal(go_to(X, Y));
    }.

+!release : my_target(X, Y) <-
    -my_target(X, Y);
    .findall(N, neighbour(N), Ns);
    .send(Ns, achieve, released(X, Y)).

+!release <- true.

+!released(X, Y)[source(A)] : claimed(X, Y, A) <-
    -claimed(X, Y, A);
    +extinguish_fire(X, Y)[source(station)].

+!released(_, _) <- true.

+neighbour(N) : my_target(X, Y) & position(Px, Py, _) & dist(Px, Py, X, Y, D) <-
    .send(N, achieve, claimed(X, Y, D)).

-neighbour(N) <- .abolish(claimed(_, _, N)).

+cell(X, Y, burning): not extinguish_fire(X, Y) & not claimed(X, Y, _) <-
    +extinguish_fire(X, Y)[source(station)].
