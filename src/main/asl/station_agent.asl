
+drone_state(X, Y, Battery, Steps)[source(A)] : charge_station(A, X2, Y2) <-
    Dx = math.abs(X2 - X);
    Dy = math.abs(Y2 - Y);
    Margin = 5;
    S = Dx + Dy;
    S2 = Battery;
    if (Battery >= 100) {
        if (sent_home(A)) { -sent_home(A); }
    };
    if (S2 <= (S + Margin)) {
        if (not sent_home(A)) {
            .print(A, " battery low, come back! battery=", Battery, " dist=", S);
            +sent_home(A);
            .send(A, tell, back_to_station(X2, Y2));
        }
    };
    -drone_state(X, Y, Battery, Steps)[source(A)].
