
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

position_score(DirX,DirY,0) :- not(tile_visits(DirX,DirY,_)).
position_score(DirX,DirY,Score) :- tile_visits(X,Y,Score).

movement_goals([]).

+!find_blocked_tiles
<-	.findall([X1,Y1], stone(X1,Y1), StoneObstacles);
	for (stone(X,Y)) {
		+blocked_tile(X,Y);
	};
	for (water(X,Y)) {
		+blocked_tile(X,Y);
	}.

+!add_movement_subgoal(DestX,DestY) : (movement_goals([[CDestX,CDestY]|T]) & CDestX \== DestX & CDestY \==DestY) | (movement_goals([]))
<-	?movement_goals(_|Tail);
	-movement_goals(_);
	+movement_goals([[DestX,DestY]|Tail]).

+!plan_best_route(X,Y) : pos(MyX,MyY) & Y == MyY & X == MyX
<-   true.

+!plan_best_route(X,Y) : pos(MyX,MyY) & X == MyX
<-   !add_movement_subgoal(X,Y).

+!plan_best_route(X,Y) : pos(MyX,MyY) & Y == MyY
<-   !add_movement_subgoal(X,Y).

+!plan_best_route(X,Y) : pos(MyX,MyY)
<-   !add_movement_subgoal(MyX,Y).

+!update_tile_visits : pos(MyX, MyY) & tile_visits(MyX,MyY,Visits)
<-  -tile_visits(MyX,MyY,_);
    +tile_visits(MyX,MyY,Visits+1).

+!update_tile_visits
<-  ?pos(MyX, MyY);
    +tile_visits(MyX,MyY,1).

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

+!atomic_move(X,Y) : pos(X,Y) <- true.

+!atomic_move(X,Y) : one_direction_possible
<-
    ?can_move(Direction);
    ?pos(MyX, MyY);
    +blocked_tile(MyX, MyY);
    do(Direction).

+!atomic_move(X,Y) : solving_obstacle(PrioDir1, PrioDir1) 
    <-
	   !update_tile_visits; 
       !choose_best_direction(Direction);
	   do(Direction);
	   if (PrioDir1==Direction) {
	       -solving_obstacle(_,_);
	   }.

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
                     & get_horizontal_direction(MyX,X,Xdir) & can_move(Xdir)
<-
    do(Xdir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
                     & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
                     & get_vertical_direction(MyY,Y,Ydir) & can_move(Ydir)
<-
    +solving_obstacle(Xdir, Ydir);
    do(Ydir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
                     & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
                     & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
                     & get_opposite_direction(Ydir,OpYDir) & can_move(OpYDir)
<-
    +solving_obstacle(Xdir, OpYDir);
	do(OpYDir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_horizontal_movement(MyX,X)
                     & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
                     & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
                     & get_opposite_direction(Ydir,OpYDir) & not(can_move(OpYDir))
                     & get_opposite_direction(Xdir,OpXDir) & can_move(OpXDir)
<-
    +solving_obstacle(Xdir, OpXDir);
	do(OpXDir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
                     & get_vertical_direction(MyY,Y,Ydir) & can_move(Ydir)
<-
    do(Ydir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
                     & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
                     & get_horizontal_direction(MyX,X,Xdir) & can_move(Xdir)
<-
    +solving_obstacle(Ydir, Xdir);
    do(Xdir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
                     & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
                     & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
                     & get_opposite_direction(Xdir,OpXDir) & can_move(OpXDir)
<-
    +solving_obstacle(Ydir, OpXDir);
	do(OpXDir).

+!atomic_move(X,Y) : pos(MyX, MyY) & continue_vertical_movement(MyY,Y)
                     & get_vertical_direction(MyY,Y,Ydir) & not(can_move(Ydir)) 
                     & get_horizontal_direction(MyX,X,Xdir) & not(can_move(Xdir)) 
                     & get_opposite_direction(Xdir,OpXDir) & not(can_move(OpXDir))
                     & get_opposite_direction(Ydir,OpYDir) & can_move(OpYDir)
<-
    +solving_obstacle(Ydir, OpYDir);
	do(OpYDir).

+!add_position_goal(X,Y)
<-	-movement_goals(_);
	+movement_goals([[X,Y]]).

+!move
<-  !find_blocked_tiles;
	!execute_movement.

+!execute_movement : movement_goals([])
<- +reached_goal.

+!execute_movement : not(movement_goals([])) & movement_goals([[DestX,DestY]|T]) & pos(DestX,DestY)
<-	-movement_goals(_);
	+movement_goals(T);
	-solving_obstacle(_,_);
	.abolish(tile_visits(_,_,_));
	!execute_movement.

+!execute_movement : not(movement_goals([])) & movement_goals([[DestX,DestY]|T])
                     & not(blocked_tile(DestX,DestY)) & not(stone(DestX,DestY)) & not(water(DestX,DestY))
<- !atomic_move(DestX,DestY).

+!execute_movement : not(movement_goals([])) & movement_goals([[DestX,DestY]|T])
                     & (blocked_tile(DestX,DestY) | stone(DestX,DestY) | water(DestX,DestY))
<-	-movement_goals(_);
	+movement_goals(T);
	!execute_movement.

