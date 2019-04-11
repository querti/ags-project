
{ include("travelling.asl") }   

+step(0) <- !add_position_goal(49,49);
			!move; !move; !move.
+step(X) <- !move; !move; !move.