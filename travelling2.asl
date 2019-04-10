
get_direction_coords(left,DirX,DirY) :- pos(MyX,MyY) & DirX=MyX-1 & DirY=MyY.
get_direction_coords(up,DirX,DirY) :- pos(MyX,MyY) & DirX=MyX & DirY=MyY-1.
get_direction_coords(right,DirX,DirY) :- pos(MyX,MyY) & DirX=MyX+1 & DirY=MyY.
get_direction_coords(down,DirX,DirY) :- pos(MyX,MyY) & DirX=MyX & DirY=MyY+1.

can_move(Direction) :- get_direction_coords(Direction,DirX,DirY) & grid_size(MaxX,MaxY) &
                       DirX > 0 & DirX < MaxX & DirY > 0 & DirX < MaxY & 
                       not(blocked_tile(DirX, DirY)) & not(water(DirX, DirY)) & not(stone(DirX, DirY)).

continue_horizontal_movement(X,DestX) :- X \== DestX.
continue_vertical_movement(Y,DestY) :- Y \== DestY.

get_horizontal_direction(X,DestX,Xdir) :- X < DestX & Xdir=right.
get_horizontal_direction(X,DestX,Xdir) :- X > DestX & Xdir=left.
get_vertical_direction(Y,DestY,Ydir) :- Y < DestY & Ydir=down.
get_vertical_direction(Y,DestY,Ydir) :- Y > DestY & Ydir=up.

dir_is_superior(Dir,BestDir) :- solving_obstacle(PrioDir1, PrioDir2) 
                                & get_opposite_direction(PrioDir2,PrioDir4)
                                & get_opposite_direction(PrioDir1,PrioDir3) &
                                ((Dir==PrioDir1 & BestDir==PrioDir2) |
                                (Dir==PrioDir1 & BestDir==PrioDir3) |
                                (Dir==PrioDir1 & BestDir==PrioDir4) |
                                (Dir==PrioDir2 & BestDir==PrioDir3) |
                                (Dir==PrioDir2 & BestDir==PrioDir4) |
                                (Dir==PrioDir3 & BestDir==PrioDir4)).

get_opposite_direction(left,right).
get_opposite_direction(right,left).
get_opposite_direction(up,down).
get_opposite_direction(down,up).

one_direction_possible :- .count(can_move(X,Y),1).

position_score(DirX,DirY,0) :- not(solving_position(DirX,DirY,_)).
position_score(DirX,DirY,Score) :- solving_position(X,Y,Score).

//primitiva
first([H|T], H).
second([H|T], HH) :- first(T, HH).
is(X,X).
// pridani prvku na zacatek seznamu
prepend(H,[], [H]).
prepend(H,L, [H|L]).
// test, zda X lezi v intervalu (From,To)
between(X, From, To) :- From < To & X > From & X < To.
between(X, From, To) :- From > To & X < From & X > To.
// vzdalenost mezi dvema body v 1D
euler_dist(A,B,Dist) :- A > B & Dist = A - B.
euler_dist(A,B,Dist) :- A < B & Dist = B - A.
euler_dist(A,B,0) :- A=B.

// GOTO PLAN - nejdrive jede k depotu, pak krouzi kolem nej
//goto_plan([[X,Y]]) :- depot(X,Y). // nejdrive jed k depotu
goto_plan([[49,49],[X,Y]]) :- depot(X,Y).
//goto_plan([[9,26],[11,15]]).
empty_goto_plan :- goto_plan([]).
get_first_position(X,Y) :- goto_plan([H|T]) & first(H, X) & second(H, Y).

// Zjistovani, jestli mezi dvema body na jedne souradnici lezi prekazka
obstacle_on_x_path(X1,X2,Y) :- blocked_tile(Xobs, Y) & between(Xobs, X1, X2).
obstacle_on_y_path(Y1,Y2,X) :-blocked_tile(X, Yobs) & between(Yobs, Y1, Y2).

// zjistovani vzdalenosti mezi dvema body na stejne souradnici
distance_x_line(X1, X2, Y, Dist) :- obstacle_on_x_path(X1,X2,Y) & 
                                    euler_dist(X1,X2,D) & 
								    Dist = D + 3.
distance_x_line(X1, X2, Y, Dist) :- euler_dist(X1,X2,Dist).
distance_y_line(Y1, Y2, X, Dist) :- obstacle_on_y_path(Y1,Y2,X) & 
                                    euler_dist(Y1,Y2,D) & 
									Dist = D + 3.
distance_y_line(Y1, Y2, X, Dist) :- euler_dist(Y1,Y2,Dist).

// stav agenta
// searching | harvesting | going_to_depot
state(searching).

// zjistovani vzdalenosti mezi dvema libovolnymi body
distance(MyX,MyY,X,Y,Dist) :- distance_x_line(MyX, X, Y, Dist_Y) &
							  distance_x_line(MyX, X, MyY, Dist_MyY) &
							  distance_y_line(MyY, Y, X, Dist_X) &
							  distance_y_line(MyY, Y, MyX, Dist_MyX) &
							  A = Dist_MyX + Dist_Y &
							  B = Dist_MyY + Dist_X &
							  .min([A,B],Dist).

// predikat pro zjisteni, jestli bod (X,Y) lezi na hraci plose
//on_board(X,Y) :- grid_size(GX,GY) & between(X,-1,GX+1) & between(Y, -1, GY+1).
on_board(X,Y) :- grid_size(GX,GY) & X < GX & Y < GY.

// vypocet funkce agregovane vzdalenosti od mista, kde agent je a od depotu
aggregated_distance(PosDist, DepDist, Dist) :- DepDist > 32 & Dist = (PosDist + 1.6*DepDist).
aggregated_distance(PosDist, DepDist, Dist) :- DepDist < 33 & Dist = (PosDist + 2.1*DepDist).

// zjisteni vsech bodu, ktere jsou aktualne od agenta nejblize
min_distance(Unvisited,UnvisitedMinDistList) :-
    min_distance_worker(Unvisited,1001,[],UnvisitedMinDistList).

min_distance_worker([],_,K,K).

min_distance_worker([H|T],MinDist,Keeper,UnvisitedMinDistList) :-
    pos(PosX,PosY) &
	first(H,Xtarget) & second(H,Ytarget) &
    distance(PosX,PosY,Xtarget,Ytarget,PosDist) &
	PosDist < MinDist &
	min_distance_worker(T,PosDist,[H],UnvisitedMinDistList).

min_distance_worker([_|T],MinDist,Keeper,UnvisitedMinDistList) :- 	
	min_distance_worker(T,MinDist,Keeper,UnvisitedMinDistList).
	
// vsechna mista na mape uz byla navstivena
all_visited :- .count(unvisited(X,Y),0).

// vygeneruje na zacatku vsechny nenavstivene body, ktere je nutne navstivit.
// pravdepodobne budem generovat 3x3 nebo 2x2 sit
+!generate_unvisited : grid_size(GX,GY)
    <- for ( .range(X,0,GX-1) ) {
	       if (((X mod 2) == 0) | (X == GX-1)) {
	           for ( .range(Y,0,GY-1) ) {
		           if (((Y mod 2) == 0) | (Y == GY-1)) { +unvisited(X,Y) }
		       }
		   }
       }.

+!register_obstacles
    <- .count(blocked_tile(A,B), ObsCount);
	   if (ObsCount > 25) {
	       // SPEEDUP!
	       .abolish(blocked_tile(_,_));
	   }
	   .findall([X1,Y1], stone(X1,Y1), StoneObstacles);
	   .findall([X2,Y2], water(X2,Y2), WaterObstacles);
	   !register_obstacles_worker(StoneObstacles);
	   !register_obstacles_worker(WaterObstacles).

+!register_obstacles_worker([]) <- true.
+!register_obstacles_worker([H|T])
    <- ?first(H,X); ?second(H,Y); // ziskani souradnic
	   +blocked_tile(X,Y); -unvisited(X,Y); // zapsani prekazky do db
	   //?get_middle_name(MiddleName);
	   //.send(MiddleName, achieve, add_blocked_tile(X, Y));
	   //?get_slow_name(SlowName);
	   //.send(SlowName, achieve, add_blocked_tile(X, Y));
	   !register_obstacles_worker(T).

+!register_visited : pos(X,Y)
    <- -unvisited(X,Y); -unvisited(X-1,Y); -unvisited(X+1,Y);
	    -unvisited(X,Y-1); -unvisited(X,Y+1).
		


// odstrani prvni prvek z goto planu
+!pop_first_position
    <- ?goto_plan([H|T]);
	   -goto_plan(_);
	   +goto_plan(T).

// pomocny rekurzivni srotovac pro naplanovani vsech bodu z listu UnvisitedMinDistList
+!plan_nearest_unvisited_worker([]) <- true.
+!plan_nearest_unvisited_worker([H|T])
    <- ?first(H,X); ?second(H,Y);
	   !plan_best_path(X,Y);
	   !plan_nearest_unvisited_worker(T).
	   
// prida do planu nejblizsi nenavstivene body na mape
+!plan_nearest_unvisited
    <- .findall([X,Y], unvisited(X,Y), Unvisited);
	   ?min_distance(Unvisited,UnvisitedMinDistList);
	   !plan_nearest_unvisited_worker(UnvisitedMinDistList).
	   
// prida bod na zacatek planu
+!prepend_to_goto_plan(X,Y)
    <- ?goto_plan(G);
	   // pokud uz neni tento bod naplanovan na zacatku, tak ho naplanuje
	   if (not get_first_position(X,Y)) {
	       ?prepend([X,Y],G,GG);
	       -goto_plan(_);
	       +goto_plan(GG)
	   }.

// naplanuje do goto planu nejlepsi moznou cestu do bodu A[X,Y]
+!plan_best_path(X,Y) : pos(MyX, MyY)
    <- ?distance_x_line(MyX, X, Y, Dist_Y);
	   ?distance_x_line(MyX, X, MyY, Dist_MyY);
	   ?distance_y_line(MyY, Y, X, Dist_X);
	   ?distance_y_line(MyY, Y, MyX, Dist_MyX);
	   // naplanuju cestu do cile
	   !prepend_to_goto_plan(X,Y);
	   // pokud je cesta po y a pak po x mensi
	   if ((Dist_MyX + Dist_Y) < (Dist_MyY + Dist_X)) {
		   // jdeme z (MyX,MyY) do (MyX,Y) pokud tam je nejaka vzdalenost
		   if (not is(MyY,Y) & not is(MyX,X)) {
	           !prepend_to_goto_plan(MyX,Y);
		   }
	   }
	   else { // jinak je cesta po x a pak po y mensi
		   // jdeme z (MyX,MyY) do (X,MyY)
		   if ((not is(MyX,X)) & (not is(MyY,Y))) {
		       // pokud jsou cesty stejne dlouhe
		       if (is((Dist_MyX + Dist_Y),(Dist_MyY + Dist_X))) {
			       if (unvisited(X,MyY)) {
				       !prepend_to_goto_plan(X,MyY);
				   }
				   else {
				       !prepend_to_goto_plan(MyX,Y);
				   }
			   }
			   else {
	               !prepend_to_goto_plan(X,MyY);
			   }
		   }
	   }.

+!update_solving_position : pos(MyX, MyY) & solving_position(MyX,MyY,Visits)
<-  -solving_position(MyX,MyY,_);
    +solving_position(MyX,MyY,Visits+1).

+!update_solving_position
<-  ?pos(MyX, MyY);
    +solving_position(MyX,MyY,1).

+!choose_best_direction(Direction)
 <- for (can_move(Dir)) {
        ?get_direction_coords(Dir,DirX,DirY);
        if (not(best_direction(_,_))) {
            ?position_score(DirX,DirY,Score);
            +best_direction(Dir, Score);
        } else {
            ?best_direction(BestDir,BestScore);
            ?position_score(DirX,DirY,MyScore);
            ?solving_obstacle(PrioDir1, PrioDir2)
            if (MyScore < BestScore | (MyScore == BestScore & dir_is_superior(Dir,BestDir))) {
                -best_direction(_,_);
                +best_direction(Dir, MyScore);
            }
        }
    }
    ?best_direction(Direction,_);
    -best_direction(_,_).

+!goto(X,Y) : pos(X,Y) <- true.

+!goto(X,Y) : one_direction_possible
<-
    ?can_move(Direction);
    ?pos(MyX, MyY);
    -unvisited(MyX, MyY);
    +blocked_tile(MyX, MyY);
    do(Direction).

// obchazim prekazku
+!goto(X,Y) : solving_obstacle(TargetDir, SolvingDir) 
    <- // pridani bodu do databaze solving_position
	   !update_solving_position; 
       !choose_best_direction(D);
	   do(D);
	   if (is(TargetDir,D)) {
	       -solving_obstacle(_,_);
	   }.

+!goto(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
              & get_horizontal_direction(MyX,X,Xdir) & can_move(Xdir)
<-
    do(Xdir).

+!goto(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
              & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
              & get_vertical_direction(MyY,Y,Ydir) & can_move(Ydir)
<-
    +solving_obstacle(Xdir, Ydir);
    do(Ydir).

+!goto(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
              & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
              & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
              & get_opposite_direction(Ydir,OpYDir) & can_move(OpYDir)
<-
    +solving_obstacle(Xdir, OpYDir);
	do(OpYDir).

+!goto(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
              & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
              & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
              & get_opposite_direction(Ydir,OpYDir) & not(can_move(OpYDir))
              & get_opposite_direction(Xdir,OpXDir) & can_move(OpXDir)
<-
    +solving_obstacle(Xdir, OpXDir);
	do(OpXDir).

+!goto(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
              & get_vertical_direction(MyY,Y,Ydir) & can_move(Ydir)
<-
    do(Ydir).

+!goto(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
              & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
              & get_horizontal_direction(MyX,X,Xdir) & can_move(Xdir)
<-
    +solving_obstacle(Ydir, Xdir);
    do(Xdir).

+!goto(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
              & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
              & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
              & get_opposite_direction(Xdir,OpXDir) & can_move(OpXDir)
<-
    +solving_obstacle(Ydir, OpXDir);
	do(OpXDir).

+!goto(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
              & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
              & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
              & get_opposite_direction(Xdir,OpXDir) & not(can_move(OpXDir))
              & get_opposite_direction(Ydir,OpYDir) & can_move(OpYDir)
<-
    +solving_obstacle(Ydir, OpYDir);
	do(OpYDir).

+!goto_next_position
    <- // pokud nemas prazdny plan, tak jedem
	   if(not empty_goto_plan) {
	       //.print("NOT EMPTY GOTO PLAN");
	       // ziskani prvni pozice z planu, kam se ma jet
	       ?get_first_position(X,Y);
		   // Pokud je tam prekazka (tzn pozice byla naplanovana pred tim, nez
		   // jsem uvidel, ze tam je prekazka), tak tam nejedu a pokracuju dal.
		   if (blocked_tile(X,Y) | stone(X,Y) | water(X,Y)) {
		       //.print("KNOWN OBSTACLE | OBSTACLCE");
		       // odstraneni prvniho prvku v goto planu
		       !pop_first_position;
			   // rekurzivne se zavola sam na sebe (jede na dalsi prvek v goto planu)
			   !goto_next_position
		   }
		   else {
		       !goto(X,Y)
		   }
	   }
	   else {
	       !plan_nearest_unvisited;
		   !goto_next_position
	   }.

+!set_next_state
    <- // pokud hleda (prohledava mapu) a uz nema nic co prohledavat, jde tezit
	   if (all_visited & empty_goto_plan) {
	       //.print("***********************************");
	       //.print("****** TED JDU TEZIT");
		   //.print("***********************************");
           // kdyz uz vsechno navstivil a nema nic v planu, jde sbirat suroviny
		   -state(_);
		   +state(harvesting);
		   // Informing friend agents that I see the whole board.
		   //?get_middle_name(MiddleName);
		   //.send(MiddleName, tell, searchingFinished);
		   //?get_slow_name(SlowName);
		   //.send(SlowName,tell,searchingFinished);
		   // This will nnot be used, middle agent sents me where I should go.
		   //!plan_harvesting
	   }
	   // pokud jde do deptotu se surovinou
	   else { if (state(going_to_depot)) {
	       // pokud je uz v depotu a odevzdal surovinu, jde dal sbirat
	       ?pos(X,Y);
		   if (depot(X,Y)) {
		       -state(_);
			   +state(harvesting);
			   // pokud uz nema zadnou tezbu v planu, tak ji naplanuje
			   if (empty_goto_plan) {
			       !plan_harvesting
			   }
		   }
	   }
	   else {
	       -state(_);
		   +state(searching)
	   }}.

+!do_step : moves_left(M) & pos(MyX,MyY) & M > 0
      <- // zaznamenam, si, kde jsem a co vidim
         !register_visited;
		 ?get_first_position(X,Y);
		 // pokud jsem u cile, smazu ho z goto planu
		 if (get_first_position(MyX,MyY)) {
		     //.print("MAZU SOLVING POSITION a SOLVING OBSTACLE");
		     !pop_first_position;
			 // pokud jsem obchazel prekazky, vymazu databazi
			 -solving_obstacle(_,_);
			 .abolish(solving_position(_,_,_));
		 }
		 // nastavi se novy stav agenta
	     !set_next_state;
         // prida vsechny prekazky o kterych vi toto kolo
         !register_obstacles;
	     // pokud vyhledava suroviny
         if(state(searching)) {
             // prida vsechno zlato a drevo, o kterych vi toto kolo
             //!register_gold;
             //!register_wood;
             // jde na dalsi naplanovanou pozici
             !goto_next_position
         } 
		 else { if (state(harvesting)) {
		     // tezi zlato / drevo - stav, kdy jde k surovine a taky kdyz ji zveda
			 //!go_harvesting
			 do(skip);
		 }
		 else { if (state(going_to_depot)) {
			 !goto_depot
		 }
		 else {
		     // jinak nevim co mam delat.
			 do(skip)
		 }}}.