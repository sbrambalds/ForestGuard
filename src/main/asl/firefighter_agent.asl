battery_level(100).
water_tank(100).

offset(1, 0).
offset(-1, 0).
offset(0, 1).
offset(0, -1).

!start.

+!start <- !check_next.

+!check_next : extinguish_fire(Xf, Yf) <-
    -extinguish_fire(Xf, Yf)[source(station)];
    !go_to(Xf, Yf);
    !fight_fire;
    !check_next.

+!check_next : cell(Xf, Yf, burning) <-
    !go_to(Xf, Yf);
    !fight_fire;
    !check_next.

+!check_next <-
    .send(station, tell, idle);
    .wait(extinguish_fire(_, _) | cell(_, _, burning));
    !check_next.

+!consume_battery : battery_level(Level) & Level > 0 & position(X, Y, _) <-
    NewLevel = Level - 1;
    -+battery_level(NewLevel);
    .send(station, tell, drone_state(X, Y, NewLevel, 0)).

+back_to_station(XD, YD)[source(A)] <-
    .suspend(check_next);
    !go_to(XD, YD);
    -back_to_station(XD, YD)[source(A)];
    .resume(check_next).

+go_refill(X, Y)[source(A)] <-
    .suspend(check_next);
    !go_to(X, Y);
    !water_refilling;
    -go_refill(X, Y)[source(A)];
    .resume(check_next).

+!use_water : water_tank(Lt) & battery_level(Battery) & position(X, Y, _) & recharge_time(T) <-
    NewLt = Lt - 10;
    .wait(T);
    -+water_tank(NewLt);
    .print("Water level: ", NewLt);
    if(NewLt == 0) {
        .send(station, tell, water_state(X, Y, Battery));
    };
    .print("Fire extinguished!");
    fire_extinguished;
    !communicate(X, Y);
    -extinguish_fire(X, Y)[source(_)].

+!communicate(X, Y) <-
    .findall(N, neighbour(N), Neighbours);
    !notify_all(Neighbours, X, Y).

+!notify_all([], _, _).

+!notify_all([N|Rest], X, Y) <-
    .send(N, tell, fire_handled(X, Y));
    !notify_all(Rest, X, Y).

+!water_refilling : recharge_time(T) <-
    .print("Refilling water tank...");
    .wait(T);
    -+water_tank(100).

+!fight_fire : water_tank(W) & W > 0 & position(X, Y, burning) <-
    !use_water;
    !fight_fire.

+!fight_fire : water_tank(W) & W > 0 & position(X, Y, _) <-
    .findall(
        opt(Xb, Yb),
        cell(Xb, Yb, burning),
        Burning
    );
    if(Burning \== []){
        .sort(Burning, [opt(Xb, Yb) | _]);
        !go_to(Xb, Yb);
        !fight_fire;
    }.

+!fight_fire <- !check_next.

+cell(X, Y, burning): not extinguish_fire(X, Y) <-
    +extinguish_fire(X, Y)[source(station)].

+!go_to(XD, YD) : position(X, Y, water_station) & X == XD & Y == YD & recharge_time(T) <-
    .print("Recharging battery and refilling water tank...");
    .wait(T);
    -+battery_level(100);
    -+water_tank(100).

+!go_to(XD, YD) : position(X, Y, _) & X == XD & Y == YD <- true.

+!go_to(XD, YD) : position(X, Y, _) <-
    .findall(
        opt(D, NX, NY),
        (offset(Dx, Dy) & NX = X+Dx & NY = Y+Dy & not obstacle(NX, NY) & not border(NX, NY) & D = math.abs(NX-XD) + math.abs(NY-YD)),
        Options
    );
    .sort(Options, [opt(_, BestX, BestY) | _]);
    move(BestX, BestY);
    !consume_battery;
    !go_to(XD, YD).

+fire_handled(X, Y)[source(_)] <- -extinguish_fire(X, Y)[source(_)].