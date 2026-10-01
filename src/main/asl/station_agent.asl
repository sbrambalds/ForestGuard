margin(20).
fires_per_firefighter(3).
dispatch_counter(0).

firefighter_name(1, firefighter1).
firefighter_name(2, firefighter2).
firefighter_name(3, firefighter3).
firefighter_name(4, firefighter4).

+drone_state(X, Y, Battery, Steps)[source(A)] : charge_station(A, X2, Y2) & margin(M) <-
    Dx = math.abs(X2 - X);
    Dy = math.abs(Y2 - Y);
    S = Dx + Dy;
    S2 = Battery;
    if (Battery >= 100) {
        if (sent_home(A)) { -sent_home(A); }
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

+!save_pos([]).

+!save_pos([map(Xc, Yc, State) | T]) <-
    -cell(Xc, Yc, _);
    +cell(Xc, Yc, State);
    if(State == burning & not dispatched(Xc, Yc)) {
        +dispatched(Xc, Yc);
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
        };
        ?dispatch_counter(C0);
        Idx = 1 + (C0 mod Active);
        -+dispatch_counter(C0 + 1);
        ?firefighter_name(Idx, FFName);
        .print("Fire detected! Dispatching to ", FFName);
        .send(FFName, tell, extinguish_fire(Xc, Yc));
    }
    !save_pos(T).

+idle[source(A)] : charge_station(A, Xh, Yh) <-
    -idle[source(A)];
    .send(A, tell, back_to_station(Xh, Yh)).
