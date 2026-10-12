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
    ?dispatch_counter(C0);
    -+dispatch_counter(C0 + 1);
    .findall(F, (firefighter_name(_, F) & not sent_home(F)), Available);
    if (Available == []) {
        .findall(F, firefighter_name(_, F), Available2);
        .length(Available2, N);
        .nth(C0 mod N, Available2, FFName);
    } else {
        .length(Available, N);
        .nth(C0 mod N, Available, FFName);
    }.

+!water_state(X, Y)[source(A)] : cell(_, _, water) <-
    .findall(water(D, Xw, Yw), (cell(Xw, Yw, water) & dist(Xw, Yw, X, Y, D)), WBlocks);
    .min(WBlocks, water(_, BestXw, BestYw));
    .send(A, achieve, refill_at(BestXw, BestYw)).

+!water_state(X, Y)[source(A)] : charge_station(A, Xh, Yh) <-
    .send(A, achieve, back_to_station(Xh, Yh)).

+!map_update(L) <-
    !save_pos(L).

+!going_home[source(A)] <-
    +sent_home(A);
    -at_base(A).

+!arrived_home[source(A)] <-
    -sent_home(A);
    +at_base(A).

+!waiting[source(A)] : charge_station(A, Xh, Yh) <-
    .send(A, achieve, back_to_station(Xh, Yh)).

+!extinguished(X, Y)[source(A)] <-
    -cell(X, Y, _);
    +cell(X, Y, wet_tree);
    -dispatched(X, Y);
    .findall(FF, (firefighter_name(_, FF) & FF \== A), Others);
    .send(Others, achieve, fire_handled(X, Y)).