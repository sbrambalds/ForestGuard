margin(20).
fires_per_firefighter(3).
dispatch_counter(0).

at_base(firefighter1).
at_base(firefighter2).
at_base(firefighter3).
at_base(firefighter4).

+!save_pos([]).

+!save_pos([map(Xc, Yc, State) | T]) <-
    -cell(Xc, Yc, _);
    +cell(Xc, Yc, State);
    if(State == burning & not dispatched(Xc, Yc)) {
        +dispatched(Xc, Yc);
        !dispatch_ff(Xc, Yc);
    };
    !save_pos(T).

+!compute_active(Active) <-
    .findall(C, cell(_, _, burning), Burning);
    .length(Burning, NumBurning);
    ?fires_per_firefighter(FPF);
    if(NumBurning >= (3 * FPF)) {
        Active = 4;
    } else {
        if(NumBurning >= (2 * FPF)) {
            Active = 3;
        } else {
            if(NumBurning >= FPF) {
                Active = 2;
            } else {
                Active = 1;
            };
        };
    }.

+!dispatch_ff(Xc, Yc) <-
    .findall(d(D, F), (firefighter_name(_, F) & at_base(F) & charge_station(F, Xh, Yh) & D = math.abs(Xh - Xc) + math.abs(Yh - Yc)), Docked);
    if (Docked \== []) {
        .min(Docked, d(_, FFName));
    } else {
        !round_robin(FFName);
    };
    -at_base(FFName);
    .print("Fire detected! Dispatching to ", FFName);
    .send(FFName, tell, extinguish_fire(Xc, Yc)).

+!round_robin(FFName) <-
    !compute_active(Active);
    ?dispatch_counter(C0);
    -+dispatch_counter(C0 + 1);
    .findall(F, (firefighter_name(Idx, F) & Idx <= Active & not sent_home(F)), Available);
    if (Available == []) {
        ?firefighter_name(1 + (C0 mod Active), FFName);
    } else {
        .length(Available, N);
        I = 1 + (C0 mod N);
        .nth(I - 1, Available, FFName);
    }.

+drone_state(X, Y, Battery, Steps)[source(A)] : charge_station(A, X2, Y2) & margin(M) <-
    Dx = math.abs(X2 - X);
    Dy = math.abs(Y2 - Y);
    S = Dx + Dy;
    S2 = Battery;
    if (Battery >= 100) {
        if (sent_home(A)) { -sent_home(A); }
    };
    if (S > 0) {
        -at_base(A);
    };
    if (S2 <= (S + M)) {
        if (not sent_home(A)) {
            .print(A, " battery low, come back! battery=", Battery, " dist=", S);
            +sent_home(A);
            .send(A, tell, back_to_station(X2, Y2));
        }
    };
    -drone_state(X, Y, Battery, Steps)[source(A)].

+water_state(X, Y, Battery)[source(A)] : cell(_, _, water) & margin(M) & charge_station(A, Xh, Yh) <-
    .findall(water(D, Xw, Yw), cell(Xw, Yw, water) & Dxw = math.abs(Xw - X) & Dyw = math.abs(Yw - Y) & D = Dxw + Dyw, WBlocks);
    .sort(WBlocks, Sorted);
    Sorted = [water(Nw, BestXw, BestYw) | _];
    Dxh = math.abs(Xh - X);
    Dyh = math.abs(Yh - Y);
    Sh = Dxh + Dyh;
    if(Nw < Sh & Battery > ((Nw * 2) + Sh + M)) {
        .print("Go to refill water at X =", BestXw, " Y= ",  BestYw);
        .send(A, tell, go_refill(BestXw, BestYw));
    } else {
        .print("Go back to station");
        .send(A, tell, back_to_station(Xh, Yh));
    };
    -water_state(X, Y, Battery)[source(A)].

+water_state(X, Y, Battery)[source(A)] : charge_station(A, Xh, Yh) <-
    +sent_home(A);
    .send(A, tell, back_to_station(Xh, Yh)).

+map_update(L)[source(A)] <- 
    !save_pos(L);
    -map_update(L)[source(A)].

+arrived_home[source(A)] <-
    -arrived_home[source(A)];
    +at_base(A).

+idle[source(A)] : charge_station(A, Xh, Yh) <-
    -idle[source(A)];
    .send(A, tell, back_to_station(Xh, Yh)).

+extinguished(X, Y)[source(A)] <-
    -extinguished(X, Y)[source(A)];
    -cell(X, Y, _);
    +cell(X, Y, wet_tree);
    -dispatched(X, Y);
    .findall(FF, (firefighter_name(_, FF) & FF \== A), Others);
    .send(Others, tell, fire_handled(X, Y)).