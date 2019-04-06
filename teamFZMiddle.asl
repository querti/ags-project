+step(0)
<-
    ?pos(MYX,MYY);
	?grid_size(X,Y);
	for ( .range(I, 0, X-1)) {
		for ( .range(J, 0, X-1)) {
			+undiscovered(I, J);
		}
	};
	+maxX(X);
	+maxY(Y);
	!observe(MYX,MYY);
	do(skip);
	do(skip).

+step(1)
<-
	?pos(MYX,MYY);
	!find_closest(MYX,MYY,MinX,MinY);
	.print("MIN coords:",MinX,", ",MinY);
	!astar(MYX,MYY,55,55);
	if (noPath) {
		.print("There is no path");
	};
	?astar_path(MYX,MYY,NextX,NextY);
	.print("Idem do ",NextX,", ",NextY);
	do(skip);
	do(skip).
	
	
+!observe(X,Y)
<-
	?grid_size(A,B);
	for (.range(I,X-3,X+3)) {
		for (.range(J,Y-3,Y+3)) {
			!discover(I,J);
		}
	}.

+!discover(X,Y):
	maxX(MX) &
	maxY(MY) &
	X > 0 &
	X < MX &
	Y > 0 &
	Y < MY &
	undiscovered(X,Y)
<-
	if (wood(X, Y)){
		+isWood(X,Y)
	};
	if (gold(X,Y)){
		+isGold(X,Y);
	};
	if (pergamen(X,Y)){
		+isPergamen(X,Y);
	};
	if (spectacles(X,Y)){
		+isSpectacles(X,Y);
	};
	if (gloves(X,Y)){
		+isHloves(X,Y);
	};
	if (shoes(X,Y)){
		+isShoes(X,Y);
	};
	if (stone(X,Y)){
		+isStone(X,Y);
		+blocked(X,Y);
	};
	if (water(X,Y)){
		+isWater(X,Y);
		+blocked(X,Y);
	};
	-undiscovered(X,Y).

+!discover(X,Y)
<-
	.print("Nothing to do").

+!find_closest(MYX,MYY,MinX,MinY)
<-
	+dis(0,0,5000);
	?grid_size(X,Y);
	for ( .range(I, 0, X-1)) {
		for ( .range(J, 0, X-1)) {
			if (undiscovered(I,J)){
				!distance(MYX,MYY,I,J,Dis);
				?dis(M,N,OldDis);
				if (OldDis > Dis & undiscovered(I,J)) {
					-dis(M,N,OldDis);
					+dis(I,J,Dis);
				};
			};
		};
	};
	?dis(MinX,MinY,MinDis);
	-dis(MinX,MinY,MinDis).

	
+!distance(X,Y,I,J,Dis)
<-
	if (X >= I) {
		Xdis = X-I;
	} else {
		Xdis = I-X;
	};
	if (Y >= J) {
		Ydis = Y-J;
	} else {
		Ydis = J-Y;
	};
	Dis=Xdis + Ydis.

//=============================================================================
//THIS IS A STAR ALGORITHM
//=============================================================================
//master function that checks next tiles, evaluates best next tile to test and eventuelly finds path
+!astar(StartX,StartY,EndX,EndY)
<-
	?grid_size(MaxX,MaxY);
	+best_next_astar(StartX,StartY);
	+continueIter;
	while (continueIter) {
		!test_neighbour_tiles(EndX,EndY);
		//check if end was reached
		if (astar_score(EndX,EndY,_,_,_,_)) {
			?astar_score(EndX,EndY,EndScore1,EndScore2,_,_);
			-continueIter;
			!find_astar_path(StartX,StartY,EndX,EndY);
			-astar_score(_,_,_,_,_,_);
			-tested_astar(_,_);
			-best_next_astar(_,_);
		//if not, evaluate next point
		} else {
			-best_next_astar(_,_);
			!find_next_astar;
			if (not(found_next_astar)) {
				-continueIter
				+noPath
			} else {
				-found_next_astar
			};
		};
	}.
+!astar
<-
	.print("Sorry m8").

//Tests UP, DOWN, LEFT, RIGHT of currently tested tile
+!test_neighbour_tiles(EndX,EndY)
<-
	?best_next_astar(CurrentX,CurrentY);
	+tested_astar(CurrentX,CurrentY);
	!test_left(CurrentX,CurrentY,EndX,EndY);
	!test_up(CurrentX,CurrentY,EndX,EndY);
	!test_right(CurrentX,CurrentY,EndX,EndY);
	!test_down(CurrentX,CurrentY,EndX,EndY).

+!test_left(CurrentX,CurrentY,EndX,EndY)
<-
	if ((CurrentX \== 0) & (not(undiscovered(CurrentX-1,CurrentY)) | (CurrentX-1 == EndX & CurrentY == EndY)) & not(blocked(CurrentX-1,CurrentY))) {
		!distance(CurrentX,CurrentY,CurrentX-1,CurrentY,CurrDis);
		!distance(EndX,EndY,CurrentX-1,CurrentY,EndDis);
		if (astar_score(CurrentX-1,CurrentY,_,_,_,_)) {
			?astar_score(CurrentX-1,CurrentY,CurrStartScore,CurrEndScore,_,_);
			if ((CurrStartScore + CurrEndScore) > (CurrDis + EndDis)) {
				-astar_score(CurrentX-1,CurrentY,CurrStartScore,CurrEndScore,_,_);
				+astar_score(CurrentX-1,CurrentY,CurrDis, EndDis, CurrentX,CurrentY);
			};
		} else{
			+astar_score(CurrentX-1,CurrentY,CurrDis, EndDis, CurrentX,CurrentY);
		};
	}.

+!test_up(CurrentX,CurrentY,EndX,EndY)
<-
	if ((CurrentY \== 0) & (not(undiscovered(CurrentX,CurrentY-1)) | (CurrentX == EndX & CurrentY-1 == EndY)) & not(blocked(CurrentX,CurrentY-1))) {
		!distance(CurrentX,CurrentY,CurrentX,CurrentY-1,CurrDis);
		!distance(EndX,EndY,CurrentX,CurrentY-1,EndDis);
		if (astar_score(CurrentX,CurrentY-1,_,_,_,_)) {
			?astar_score(CurrentX,CurrentY-1,CurrStartScore,CurrEndScore,_,_);
			if ((CurrStartScore + CurrEndScore) > (CurrDis + EndDis)) {
				-astar_score(CurrentX,CurrentY-1,CurrStartScore,CurrEndScore,_,_);
				+astar_score(CurrentX,CurrentY-1,CurrDis, EndDis, CurrentX,CurrentY);
			};
		} else{
			+astar_score(CurrentX,CurrentY-1,CurrDis, EndDis, CurrentX,CurrentY);
		};
	}.

+!test_right(CurrentX,CurrentY,EndX,EndY)
<-
	if ((CurrentX \== MaxX-1) & (not(undiscovered(CurrentX+1,CurrentY)) | (CurrentX+1 == EndX & CurrentY == EndY)) & not(blocked(CurrentX+1,CurrentY))) {
		!distance(CurrentX,CurrentY,CurrentX+1,CurrentY,CurrDis);
		!distance(EndX,EndY,CurrentX+1,CurrentY,EndDis);
		if (astar_score(CurrentX+1,CurrentY,_,_,_,_)) {
			?astar_score(CurrentX+1,CurrentY,CurrStartScore,CurrEndScore,_,_);
			if ((CurrStartScore + CurrEndScore) > (CurrDis + EndDis)) {
				-astar_score(CurrentX+1,CurrentY,CurrStartScore,CurrEndScore,_,_);
				+astar_score(CurrentX+1,CurrentY,CurrDis, EndDis, CurrentX,CurrentY);
			};
		} else{
			+astar_score(CurrentX+1,CurrentY,CurrDis, EndDis, CurrentX,CurrentY);
		};
	}.

+!test_down(CurrentX,CurrentY,EndX,EndY)
<-
	if ((CurrentY \== MaxY-1) & (not(undiscovered(CurrentX,CurrentY+1)) | (CurrentX == EndX & CurrentY+1 == EndY)) & not(blocked(CurrentX,CurrentY+1))) {
		!distance(CurrentX,CurrentY,CurrentX,CurrentY+1,CurrDis);
		!distance(EndX,EndY,CurrentX,CurrentY+1,EndDis);
		if (astar_score(CurrentX,CurrentY+1,_,_,_,_)) {
			?astar_score(CurrentX,CurrentY+1,CurrStartScore,CurrEndScore,_,_);
			if ((CurrStartScore + CurrEndScore) > (CurrDis + EndDis)) {
				-astar_score(CurrentX,CurrentY+1,CurrStartScore,CurrEndScore,_,_);
				+astar_score(CurrentX,CurrentY+1,CurrDis, EndDis, CurrentX,CurrentY);
			};
		} else{
			+astar_score(CurrentX,CurrentY+1,CurrDis, EndDis, CurrentX,CurrentY);
		};
	}.

// Once a viable path is possible, creates a set of knowledge points that represent it
+!find_astar_path(StartX,StartY,EndX,EndY)
<-
	+continuePath;
	+this_astar_point(EndX,EndY);
	while (continuePath) {
		!find_next_point;
		?this_astar_point(CurrentX,CurrentY);
		if (CurrentX == StartX & CurrentY == StartY) {
			-continuePath;
			-this_astar_point(CurrentX,CurrentY);
		};
	}.

//Find next point of the path
+!find_next_point
<-
	?this_astar_point(CurrentX,CurrentY);
	?astar_score(CurrentX,CurrentY,_,_,PathX,PathY);
	+astar_path(PathX,PathY,CurrentX,CurrentY);
	-this_astar_point(CurrentX,CurrentY);
	+this_astar_point(PathX,PathY).
	
//Find next tile to be tested
+!find_next_astar
<-
	?astar_score(SomeX,SomeY,_,_,_,_);
	+next_astar(SomeX,SomeY);
	?grid_size(MaxX,MaxY);
	for ( .range(I, 0, MaxX-1)) {
		for ( .range(J, 0, MaxX-1)) {
			?next_astar(NextX,NextY);
			?astar_score(NextX,NextY,CurrBestStart,CurrBestEnd,_,_);
			if (astar_score(I,J,_,_,_,_)){
				?astar_score(I,J,ThisStart,ThisEnd,_,_);
				if ((CurrBestStart+CurrBestEnd) > (ThisStart+ThisEnd) & not(tested_astar(I,J))) {
					-next_astar(NextX,NextY);
					+next_astar(I,J);
					+found_next_astar;
				} elif (((CurrBestStart+CurrBestEnd) == (ThisStart+ThisEnd)) & (CurrBestEnd > ThisEnd) & not(tested_astar(I,J))){
					-next_astar(NextX,NextY);
					+next_astar(I,J);
					+found_next_astar;
				};
			};
		};
	};
	?next_astar(BestX,BestY);
	+best_next_astar(BestX,BestY);
	-next_astar(BestX,BestY).
	
	
	
	
