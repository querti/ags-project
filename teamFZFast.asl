{ include("astar.asl") }  

viable_scout(X,Y) :- discovered(X,Y) & not(blocked(X,Y)) & not(denied_scout(X,Y)) &
                    (undiscovered(X-1,Y) | undiscovered(X,Y-1) | undiscovered(X+1,Y) | undiscovered(X,Y+1)).

+step(0)
<-  ?pos(X,Y);
    !map_undiscovered;
	+reach_depot;
	?depot(DepotX,DepotY);
	!define_goal(DepotX,DepotY);
    !do_step;
    !do_step;
	!do_step.

+step(I)
<-  !do_step;
    !do_step;
	!do_step.

+!do_step
<-  !discover(1);
	?pos(MyX,MyY);
	if (reach_depot) {
		?depot(DepotX,DepotY);
		if (MyX==DepotX & MyY==DepotY) {
			-reach_depot;
			+scout_map;
		} else {
			!move;
		}
	}
	if (scout_map) {
		?big_goal(GX,GY);
		if (MyX==GX & MyY==GY) {
			-reached;
			!scout_next;
			?next_scout(ScX,ScY);
			.print("next goal ",ScX,", ",ScY);
			!define_goal(ScX,ScY);
			-next_scout(_,_);
			!move;
		} else {
			!move;
		}
	}.

+!scout_next
<-
	+next_scout(50,50).
