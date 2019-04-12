
abs(X,X) :- X >= 0.
abs(X,-X) :- X < 0.

on_board(X,Y) :- grid_size(MaxX,MaxY) & 
                 X > 0 & X < MaxX &
                 Y > 0 & Y < MaxY.

untested_score(X,Y,StartDis,EndDis,SrcX,SrcY) :- tile_score(X,Y,StartDis,EndDis,SrcX,SrcY) & not(tested(X,Y)).

direction(X,Y,MoveX,MoveY,Dir) :- X-1==MoveX & Dir=left.
direction(X,Y,MoveX,MoveY,Dir) :- Y-1==MoveY & Dir=up.
direction(X,Y,MoveX,MoveY,Dir) :- X+1==MoveX & Dir=right.
direction(X,Y,MoveX,MoveY,Dir) :- Y+1==MoveY & Dir=down.

viable_tile(X,Y) :- discovered(X,Y) & not(blocked(X,Y)) & not(attempted(X,Y)) &
                    (undiscovered(X-1,Y) | undiscovered(X,Y-1) | undiscovered(X+1,Y) | undiscovered(X,Y+1)).

+!map_undiscovered : pos(MyX,MyY) & grid_size(MaxX,MaxY)
<-  for (.range(X,0,MaxX)) {
        for (.range(Y,0,MaxY)) {
            +undiscovered(X,Y);
        }

    }.

+!discover(Range) : pos(MyX,MyY)
<-  for (stone(X,Y)) {
        .print("There is stone: ",X,", ",Y);
		+blocked(X,Y);
	};
	for (water(X,Y)) {
		+blocked(X,Y);
	}
    for (.range(DX,MyX-Range,MyX+Range)) {
        for (.range(DY,MyY-Range,MyY+Range)) {
            //.print("objavujem",DX,", ",DY);
            -undiscovered(DX,DY);
            +discovered(DX,DY);
        }
    }.


+!distance(X1,Y1,X2,Y2,Dis)
<-  DisX = X1 - X2;
    DisY = Y1 - Y2;
    ?abs(DisX,AbsX);
    ?abs(DisY,AbsY);
    Dis = AbsX + AbsY.
//================================================================================================
//ASTAR
//================================================================================================
+!calculate_score(X,Y,SrcX,SrcY) : start(X1,Y1) & end(X2,Y2) & on_board(X,Y) & not(undiscovered(X,Y)) & not(blocked(X,Y))
<-  !distance(X,Y,X2,Y2,EndDis);
    ?tile_score(SrcX,SrcY,PrevStartDis,_,_,_);
    //.print("prev score: ",SrcX,", ", SrcY, "  ",PrevStartDis);
    StartDis = PrevStartDis + 1;
    if (tile_score(X,Y,CurrStartDis,CurrEndDis,_,_)) {
        if (StartDis + EndDis < CurrStartDis + CurrEndDis) {
                -tile_score(X,Y,_,_);
                +tile_score(X,Y,StartDis,EndDis,SrcX,SrcY);
                //.print("score: ",X,", ",Y,",  ",StartDis,", ",EndDis );
           }
    } else {
        +tile_score(X,Y,StartDis,EndDis,SrcX,SrcY);
        if (X==X2 & Y==Y2) {
            +found_end;
            //.print("score: ",X,", ",Y,",  ",StartDis,", ",EndDis );
        }
    }.

+!calculate_score(X,Y,SrcX,SrcY)
<- //.print("aaa").
	true.

+!calculate_scores(X,Y)
<-
    !calculate_score(X-1,Y,X,Y);
    !calculate_score(X,Y-1,X,Y);
    !calculate_score(X+1,Y,X,Y);
    !calculate_score(X,Y+1,X,Y).

+!get_smallest_score(SmX,SmY)
<-  for (tile_score(X,Y,StartDis,EndDis,_,_)) {
        
        if (not(tested(X,Y))) {
            //.print("testujem",X,", ",Y);
            if (not(smallest_score(CurrX,CurrY,CurrStartDis,CurrEndDis))) {
                +smallest_score(X,Y,StartDis,EndDis);
            } else {
                ?smallest_score(CurrX,CurrY,CurrStartDis,CurrEndDis);
                if ((StartDis + EndDis < CurrStartDis + CurrEndDis) | 
                    ((StartDis + EndDis == CurrStartDis + CurrEndDis) & EndDis < CurrEndDis)) {
                    -smallest_score(_,_,_,_);
                   +smallest_score(X,Y,StartDis,EndDis);
                }
            }
        }

    }
    if (not(smallest_score(_,_,_,_))) {
        +no_path;
    } else {
        ?smallest_score(SmX,SmY,_,_);
        -smallest_score(_,_,_,_);
        
    }.

+!astar
<-  !get_smallest_score(X,Y);
    //.print("smallest: ",X,", ",Y);
    if (not(no_path)) {
        +tested(X,Y);
        !calculate_scores(X,Y);
        //.print("scores calculated");
    }.

+!astar_loop(EndX,EndY) : pos(SrcX,SrcY)
<-  +start(SrcX,SrcY);
    +end(EndX,EndY);
    !distance(SrcX,SrcY,EndX,EndY,EndDis);
    +tile_score(SrcX,SrcY,0,EndDis,SrcX,SrcY);
    while (not(found_end) & not(no_path)) {
        !astar;
    }
    if (not(no_path)) {
        !get_path(EndX,EndY);
    }
    .abolish(start(_,_));
    .abolish(end(_,_));
    .abolish(tile_score(_,_,_,_,_,_));
    .abolish(tested(_,_));
    .abolish(found_end).

+!get_path(X,Y) : start(StartX,StartY) & X == StartX & Y == StartY
<- true.

+!get_path(X,Y)
<-  ?tile_score(X,Y,_,_,SrcX,SrcY);
    +astar_path(SrcX,SrcY,X,Y);
    //.print("Added path", SrcX,", ",SrcY,",  ",X,", ",Y);
    !get_path(SrcX,SrcY).

//================================================================================================

+!define_goal(X,Y)
<-  -big_goal(_,_);
    +big_goal(X,Y).

+!find_next_subgoal
<-  ?big_goal(EndX,EndY);
    for (discovered(X,Y)) {
        if (viable_tile(X,Y)) {
            //.print("viable tile: ",X,", ",Y);
            !distance(X,Y,EndX,EndY,Dis);
            if (not(best_candidate(_,_,_))) {
                +best_candidate(X,Y,Dis);
            } else {
                ?best_candidate(CurrX,CurrY,CurrDis);
                if (Dis < CurrDis) {
                    -best_candidate(_,_,_);
                    +best_candidate(X,Y,Dis);
                }
            }
        }
    }
    ?best_candidate(BestX,BestY,_);
    !astar_loop(BestX,BestY);
    if (no_path) {
        -no_path;
        -best_candidate(_,_,_);
        +attempted(BestX,BestY);
        !find_next_subgoal;
    }
    -best_candidate(_,_,_).

+!move
<-  ?pos(MyX,MyY);
    ?big_goal(GoalX,GoalY);
    if (MyX==GoalX & MyY==GoalY) {
        +reached;
        .abolish(astar_path(_,_,_,_));
    } elif (not(astar_path(MyX,MyY,_,_))) {
        .abolish(astar_path(_,_,_,_));
        !astar_loop(GoalX,GoalX);
        if (no_path) {
            -no_path;
            !find_next_subgoal;
        }
        ?astar_path(MyX,MyY,EndX,EndY);
        ?direction(MyX,MyY,EndX,EndY,Direction);
        do(Direction);
    } else {
        ?pos(MyX,MyY);
        ?astar_path(MyX,MyY,EndX,EndY);
        ?direction(MyX,MyY,EndX,EndY,Direction);
        do(Direction);
    }.