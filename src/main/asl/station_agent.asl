margin(20).
fires_per_firefighter(3).
dispatch_counter(0).

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
    .count(cell(_, _, burning), N);
    ?fires_per_firefighter(FPF);
    Active = math.min(4, 1 + math.floor(N / FPF)).

+!dispatch_ff(Xc, Yc) <-
    .findall(d(D, F), (firefighter_name(_, F) & at_base(F) & charge_station(F, Xh, Yh) & dist(Xc, Yc, Xh, Yh, D)), Docked);
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
        .nth(C0 mod N, Available, FFName);
    }.

+!drone_state(X, Y, Battery, Steps)[source(A)] : charge_station(A, X2, Y2) & margin(M) & moves_per_level(A, K) <-
    ?dist(X, Y, X2, Y2, S);
    if (Battery >= 100 & sent_home(A)) { 
        -sent_home(A); 
    };
    if (S > 0) { 
        -at_base(A); 
    };
    if (Battery * K + Steps <= S + M & not sent_home(A)) {
        .print(A, " battery low, come back! battery=", Battery, " dist=", S);
        +sent_home(A);
        .send(A, achieve, back_to_station(X2, Y2));
    }.

+!water_state(X, Y, Battery)[source(A)] : cell(_, _, water) & margin(M) & charge_station(A, Xh, Yh) & moves_per_level(A, K) <-
    .findall(water(D, Xw, Yw), (cell(Xw, Yw, water) & dist(Xw, Yw, X, Y, D)), WBlocks);
    .min(WBlocks, water(Nw, BestXw, BestYw));
    ?dist(X, Y, Xh, Yh, Dh);
    if(Nw < Dh & (Battery * K) > ((Nw * 2) + Dh + M)) {
        .print("Go to refill water at X =", BestXw, " Y= ",  BestYw);
        .send(A, achieve, go_refill(BestXw, BestYw));
    } else {
        .print("Go back to station");
        .send(A, achieve, back_to_station(Xh, Yh));
    }.

+!water_state(X, Y, Battery)[source(A)] : charge_station(A, Xh, Yh) <-
    +sent_home(A);
    .send(A, achieve, back_to_station(Xh, Yh)).

+!map_update(L) <-
    !save_pos(L).

+!arrived_home[source(A)] <-
    +at_base(A).

+!waiting[source(A)] : charge_station(A, Xh, Yh) <-
    .send(A, achieve, back_to_station(Xh, Yh)).

+!extinguished(X, Y)[source(A)] <-
    fire_extinguished(X, Y);
    -cell(X, Y, _);
    +cell(X, Y, wet_tree);
    -dispatched(X, Y);
    .findall(FF, (firefighter_name(_, FF) & FF \== A), Others);
    .send(Others, achieve, fire_handled(X, Y)).