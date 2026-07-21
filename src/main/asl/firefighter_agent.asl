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

+!check_next : home(Xh, Yh) & position(X, Y, _) & X == Xh & Y == Yh <-
    .wait(500);
    !check_next.

+!check_next : home(Xh, Yh) <-
    !go_to(Xh, Yh);
    !check_next.

+!consume_battery : battery_level(Level) & Level > 0 & position(X, Y, _) <-
    NewLevel = Level - 1;
    -+battery_level(NewLevel);
    .print("Battery level: ", NewLevel);
    .send(station, tell, drone_state(X, Y, NewLevel, 0)).

+back_to_station(_, _)[source(A)] : home(Xh, Yh) <- 
    .suspend(check_next);
    !go_to(Xh, Yh);
    -back_to_station(_, _)[source(A)];
    .resume(check_next).

+go_refill(X, Y)[source(A)] <-
    .suspend(check_next);
    !go_to(X, Y);
    !water_refilling;
    -go_refill(X, Y)[source(A)];
    .resume(check_next).

+!use_water : water_tank(Lt) & battery_level(Battery) & position(X, Y, _) & home(Xh, Yh) & recharge_time(T) <-
    NewLt = Lt - 10;
    .wait(T);
    -+water_tank(NewLt);
    .print("Water level: ", NewLt);
    if(NewLt == 0) {
        .send(station, tell, water_state(X, Y, Xh, Yh, Battery));
    };
    .print("Fire extinguished!");
    fire_extinguished;
    -extinguish_fire(X, Y)[source(_)].

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

+!go_to(XD, YD) : position(X, Y, _) & X == XD & Y == YD & home(Xh, Yh) & recharge_time(T) <- 
    if(XD == Xh & YD == Yh){
        .print("Recharging battery...");
        .wait(T);
        -+battery_level(100);
        -+water_tank(100);
    }.

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
