/**
 *  AGS project - environment model
 *  Based on gold-miners example
 *  @ Jan Horacek <ihoracek@fit.vutbr.cz> , 
 *  @ Frantisek Zboril jr <zborilf@fit.vut.cz>
 */

package mining;

import jason.environment.grid.GridWorldModel;
import jason.environment.grid.Location;
import java.util.Random;

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import mining.MiningPlanet.Move;

/**
  *  Model of the world
  *  @warning Singleton Class
  *  @warning Input/Output methods have to be synchronized (if it is not synchronized from Jason)
  */

public class WorldModel extends GridWorldModel
{
  
    
  public static final int _FORREST_WOODS=80;
  public static final int _FORREST_RADIUS=15;
  
    
private static int _moneyLimit=100;
private static int _dominanceLimit=15;
private Location depot;
private Set<Location> clearLocations = new HashSet<Location>(); // namisto zaremovanych, zvednute objekty se vymazi az po ckonceni kola   
private Logger logger   = Logger.getLogger("jasonTeamSimLocal.mas2j." + WorldModel.class.getName());
private String id = "WorldModel";
public agentModel[] MyAgents=new agentModel[8];
public depotModel DepotA, DepotB;
    

public static class DWObjects{
  public static final int   GOLD  = 16;
  public static final int   DEPOT = 32;
  public static final int   WOOD  = 64;
  public static final int   GLOVES  = 128;
  public static final int   SHOES  = 256;
  public static final int   SPECTACLES  = 512;
  public static final int   WATER = 1024;
  public static final int   STONE = 2048;
  public static final int   PERGAMEN = 4096;
  
  
  public static int[] getObjectsList(){
      return(new int[]{GOLD, DEPOT, WOOD, GLOVES, SHOES,
                        SPECTACLES,WATER,STONE,PERGAMEN});
  }
  
  public static String getObjectName(int i){
      switch(i){
        case GOLD:return new String("gold");
        case DEPOT:return new String("depot");
        case WOOD:return new String("wood");
        case GLOVES:return new String("gloves");
        case SHOES:return new String("shoes");
        case SPECTACLES:return new String("spectacles");
        case WATER:return new String("water");
        case STONE:return new String("stone");
        case PERGAMEN:return new String("pergamen");  
      }
      return(null);
  }
 
  public static int getDropByName(String s){
      if(s.equals("gold"))
               return(GOLD);
      if(s.equals("wood"))
               return(WOOD);
      if(s.equals("water"))
               return(WATER);
      if(s.equals("stoen"))
               return(STONE);
      if(s.equals("pergamen"))
               return(PERGAMEN);
      if(s.equals("all"))
               return(0);
      return(-1);
  }
  
  
  public static int getObjectIndex(int object){
      int idx=object / 32;
      idx=(int)Math.round(Math.log(idx)/Math.log(2));
      return(idx);
  }        
 }

 void printIntVector(int[] vector){
     System.out.print('[');
     for(int i:vector)
         System.out.print(i);
     System.out.println(']');
 }
         
 
  static final int _Agent_Count=8;


  
    public int objectToDepotIndex(int object){

            switch(object){
                case(DWObjects.PERGAMEN):return(0);
                case(DWObjects.STONE):return(1);
                case(DWObjects.WATER):return(2);
                case(DWObjects.WOOD):return(3);
                case(DWObjects.GOLD):return(4);
            }
            return(-1);
    }
    
  
      public depotModel getDepot(int team){
      depotModel depot=DepotA;
      if(team==2)
          depot=DepotB;
      return(depot);
      
  }

    
  public String getDepotItems(int team){
      return(getDepot(team).getItemsString());
  }  
   
  /* ============================================================================== */
  
  public boolean gameOver(){
      if(Math.abs(DepotA.getMoney()-DepotB.getMoney())>_dominanceLimit)
          return(true);
      return((DepotA.getMoney()>=_moneyLimit)||
                (DepotB.getMoney()>=_moneyLimit));
  }
  
  
  

 /* 
    environment/position checks
  */
  public boolean passable(int x, int y){
          return(isFree(DWObjects.STONE,x,y)&isFree(OBSTACLE,x,y));
  }
  
  public boolean deadly(int x, int y){
      int xa=Math.max(0,x-1);
      int ya=Math.max(0,y-1);
      int xb=Math.min(this.getWidth()-1,x+1);
      int yb=Math.min(this.getHeight()-1,y+1);
      for(int i=xa;i<=xb;i++)
          for(int j=ya;j<=yb;j++)
            if(!(hasObject(DWObjects.WATER,i,j)))
                return(false);
      return(true);
  }

  
  /**
    *  Results of actions
    *  @note ERROR could mean cheating
    */
  public enum ActionResult
  {
    OK(0), MISTAKE(1), ROUND_FINISHED(2), SIMULATION_ENDS(3), ERROR(-1);
    private int val;
    
    ActionResult(int i)
    {
      val = i;
    }
    
    boolean isNotError()
    {
      return (val>=0);
    }
  };
  

  
  // singleton pattern
  protected static WorldModel model = null;
  
  synchronized public static WorldModel create(int w, int h, int nbAgs)
  {
    if (model == null)
    {
      model = new WorldModel(w, h, nbAgs);
    }
    return model;
  }
  
  public static WorldModel get()
  {
    return model;
  }
  
  public static void destroy()
  {
    model = null;
  }

  private WorldModel(int w, int h, int nbAgs)
  {
    super(w, h, nbAgs);
        
    for(int i=0;i<MyAgents.length;i++)
        MyAgents[i]=new agentModel(i);
    DepotA=new depotModel();
    DepotB=new depotModel();
    for(int i=0;i<6;i++){
      int[] spell=newSpell(2);
      DepotA.addSpell(spell);
      DepotB.addSpell(spell);
    }

    initMoves(); 
     
  }

  public String getId()
  {
    return id;
  }
  public void setId(String id)
  {
    this.id = id;
  }
  public String toString()
  {
    return id;
  }
  
  /**
    *  Where is some depot
    */
  public Location getDepot()
  {
    return depot;
  }

 
  /**
    *  Set depot position
    */
  private void setDepot(int x, int y)
  {
    depot = new Location(x, y);
    data[x][y] = DWObjects.DEPOT;
  }                                     

  /**
    *  Set agent position
    *  @note Agents can now have the same position (problem with removing agent repaired)
    *  @note xpokor04 2010-03-11: Fixed bugs connected with the attempt to persuade gold-miners' classes to do another job :-)
    */
  @Override
  public void setAgPos(int ag, Location l)
  {
    Location oldLoc = getAgPos(ag);
    if (oldLoc != null) {
      boolean reallyDeleteAgent = true;
      for (int i = 0; i < agPos.length; i++)
      {
        if (ag != i)
        {
          Location otherLoc = getAgPos(i);
          if ((otherLoc.x == oldLoc.x) && (otherLoc.y == oldLoc.y))
          {
            reallyDeleteAgent = false;

            /* redraw original agent that stays at the place;  drawEmpty necessary to get rid of
               little rectangles belonging to throughgoing agents */
            Graphics g = view.getCanvas().getGraphics();
            view.drawEmpty(g, otherLoc.x, otherLoc.y);
            view.drawAgent(g, otherLoc.x, otherLoc.y, Color.black /*won't be used*/, i);

            break;
          }
        }
      }
      if (reallyDeleteAgent)
      {
        remove(AGENT, oldLoc.x, oldLoc.y);
      }
    }
    agPos[ag] = l;
    add(AGENT, l.x, l.y);
  }
  
  /**
    *  Is position free
    *  @note Added GOLD and DEPOT type
    */
  @Override
  public boolean isFree(int x, int y)
  {
    return inGrid(x, y) && (data[x][y] & OBSTACLE) == 0 && 
            (data[x][y] & AGENT) == 0 && 
            (data[x][y] & DWObjects.GOLD) == 0 && 
            (data[x][y] & DWObjects.DEPOT) == 0 &&
            (data[x][y] & DWObjects.WATER) == 0 &&
            (data[x][y] & DWObjects.STONE) == 0;
  }

  public boolean isPickable(int x, int y)
  {
    return inGrid(x, y) && 
            (data[x][y] & DWObjects.WOOD) != 0 ||
            (data[x][y] & DWObjects.GOLD) != 0 ||
            (data[x][y] & DWObjects.PERGAMEN) != 0 ||
            (data[x][y] & DWObjects.SPECTACLES) != 0 ||
            (data[x][y] & DWObjects.SHOES) != 0 ||
            (data[x][y] & DWObjects.GLOVES) != 0;
  }

 
  private void clearLocations(){
      // stone -> spectacles -> gloves -> shoes -> wood -> pergamen -> gold
  
      for(Location l:clearLocations){
          if(hasObject(WorldModel.DWObjects.STONE,l))
              model.remove(WorldModel.DWObjects.STONE, l);
          else if(hasObject(WorldModel.DWObjects.SPECTACLES,l))
              model.remove(WorldModel.DWObjects.SPECTACLES, l);
          else if(hasObject(WorldModel.DWObjects.GLOVES,l))
              model.remove(WorldModel.DWObjects.GLOVES, l);
          else if(hasObject(WorldModel.DWObjects.SHOES,l))
              model.remove(WorldModel.DWObjects.SHOES, l);
          else if(hasObject(WorldModel.DWObjects.WOOD,l))
              model.remove(WorldModel.DWObjects.WOOD, l);
          else if(hasObject(WorldModel.DWObjects.PERGAMEN,l))
              model.remove(WorldModel.DWObjects.PERGAMEN, l);
          else if(hasObject(WorldModel.DWObjects.GOLD,l))
              model.remove(WorldModel.DWObjects.GOLD, l);
      }
  }
  
  
  /**
    *  List of agents that are near by specified agent
    */
  public synchronized Set confusedAgents(int agId)
  {
    Set result = new HashSet<Integer>();
    Location pos = getAgPos(agId);
    for (int other = 0; other<6; other++)
    {
      if (agId != other)
      {
        Location l = getAgPos(other);
        if (Math.abs(l.x - pos.x) <= 1 && Math.abs(l.y - pos.y) <= 1)
          result.add(other);
      }
    }
    return result;
  }
  
  
  private synchronized void initMoves()
  {
    for(int i=0; i<_Agent_Count; i++)

        MyAgents[i].resetSteps(); 
        

  }
  
  private int stepsLeftModel(int a,int b){
      // agenti 1 .. 6
      // indexy 0 .. 5
      int result=0;
      for(int i=a-1;i<b;i++)
          result+=MyAgents[i].getStepsLeft();
      return(result);
  }
  
  private int getAmount(int material){
      switch(material){
          case(2):return(2);
          case(3):return(4);
          case(4):return(6);
          case(5):return(9);
      }
      return(0);
  }
  


  /**
    *  Maps agent name to id
    */
  public int getAgIdBasedOnName(String agName) throws Error
  {
    if (agName.equals("aSlow")) return 0;
    else
    if (agName.equals("aMiddle")) return 1;
    else
    if (agName.equals("aFast")) return 2;
    else
    if (agName.equals("bSlow")) return 3;
    else
    if (agName.equals("bMiddle")) return 4;
    else
    if (agName.equals("bFast")) return 5;
    else
    if (agName.equals("aDruid")) return 6;
    else
    if (agName.equals("bDruid")) return 7;
    else
    throw new Error("Wrong agent name [" + agName + "]");
  }
  
  /**
    *  Maps agent id to name
    */
  public String getAgNameBasedOnId(int agId) throws Error
  {
    if (agId == 0) return "aSlow";
    else
    if (agId == 1) return "aMiddle";
    else
    if (agId == 2) return "aFast";
    else
    if (agId == 3) return "bSlow";
    else
    if (agId == 4) return "bMiddle";
    else
    if (agId == 5) return "bFast";
    else
    if (agId == 6) return "aDruid";
    else
    if (agId == 7) return "bDruid";
    else
    throw new Error("Wrong agent ID [" + agId + "]");
  }
 
  
  /*****************************************************************************
   *                            ACTIONS
   ****************************************************************************/
 
  
  
  /**
    *  Check if agent can do this action
    *  @note Cheating prevention
    */
  
  private synchronized ActionResult checkActions(int agId, int moves, boolean finishedOk, String action)
  {
      
    if(!MyAgents[agId].decreaseSteps(moves)){
        logger.warning("Error " + model.getAgNameBasedOnId(agId) + " trying to run more steps than alowed!!!"+" action "+action);
        return ActionResult.ERROR;
    }

    if(stepsLeftModel(1,3)+stepsLeftModel(7,7)==0)
        logger.info("Team A finished round");
    if(stepsLeftModel(4,6)+stepsLeftModel(8,8)==0)
        logger.info("Team B finished round");

    if(stepsLeftModel(1,8)<=0)
    {
        logger.info("!!!!!!!!!!    Round finished    !!!!!!!!!!!!");
        logger.info("============================================");
        clearLocations();
        initMoves();
        return ActionResult.ROUND_FINISHED;
    }
      
    if (finishedOk)
    {
      return ActionResult.OK;
    }
    else
    {
      return ActionResult.MISTAKE;
    }
  }
  
  
  
  /**
    *********
    *  move *
    *********
    */
  
  synchronized ActionResult move(Move dir, int ag) throws Exception
  {
    Location l = getAgPos(ag);
    switch (dir) {
    case UP:
      if (passable(l.x, l.y - 1))
      {
        setAgPos(ag, l.x, l.y - 1);
        logger.info("Agent " + getAgNameBasedOnId(ag) + " moved at position [" + getAgPos(ag) + "]");
      }
      else
      {          
        return checkActions(ag, 1, false, "move");
      }
      break;
    case DOWN:
      if (passable(l.x, l.y + 1))
      {
        setAgPos(ag, l.x, l.y + 1);
        logger.info("Agent " + getAgNameBasedOnId(ag) + " moved at position [" + getAgPos(ag) + "]");
      }
      else
      {          
        return checkActions(ag, 1, false, "move");
      }
      break;
    case RIGHT:
      if (passable(l.x + 1, l.y))
      {
        setAgPos(ag, l.x + 1, l.y);
        logger.info("Agent " + getAgNameBasedOnId(ag) + " moved at position [" + getAgPos(ag) + "]");
      }
      else
      {          
        return checkActions(ag, 1, false, "move");
      }
      break;
    case LEFT:
      if (passable(l.x - 1, l.y))
      {
        setAgPos(ag, l.x - 1, l.y);
        logger.info("Agent " + getAgNameBasedOnId(ag) + " moved at position [" + getAgPos(ag) + "]");
      }
      else
      {          
        return checkActions(ag, 1, false, "move");
      }
      break;
    }
    return checkActions(ag, 1, true, "move");
  }
  
  /**
    ****************
    *  Action skip *
    ****************
    */
  synchronized ActionResult skip(int ag)
  {        
    logger.info("Agent " + getAgNameBasedOnId(ag) + " skipped its move");
    return checkActions(ag, 1, true, "skip");
  }

  /*
  *  DRUIDI AKCE
  */
    
  synchronized ActionResult createItem(int ag, int[] spell){
      ActionResult result=ActionResult.ERROR;
      if((ag!=6)&&(ag!=7)){
          logger.warning("Agent " + getAgNameBasedOnId(ag) + " is not a druid!");
          return checkActions(ag, 1, false, "createItem");
    }
      depotModel depot=DepotA;
      if(ag==7)
          depot=DepotB;
 
      if(depot.matchSpells(spell)){
          int[] material=new int[]{0,spell[0],spell[1],spell[2],spell[3]};
          int used=depot.useMaterial(material);

          int amount=getAmount(used); // kolik penez za vyrobek z x kusu materialu
          depot.raiseMoney(amount);
          if(gameOver()) {
              System.out.println("============ KONEC ============");
               return(ActionResult.SIMULATION_ENDS);
          }
          return(checkActions(ag,1,true,"createItem"));
      }
      else
        return(checkActions(ag,1,false,"createItem"));     
  }
  
  
  int[] newSpell(int number){
      int[] spell=new int[]{0,0,0,0};
      int j;
      for(int i=0;i<number;i++){
          j=(int)Math.round(Math.random()*3);
          spell[j]++;
      }
      return(spell);
  }

  
  synchronized ActionResult readSpell(int ag, int pergamens){
      depotModel depot;          
      if((ag!=6)&&(ag!=7)){
          logger.warning("Agent " + getAgNameBasedOnId(ag) + " is not a druid!");
          return (checkActions(ag, 1, false, "read")); // Druid ma 1 move, takze by stacila jednicka, dtto 'create'
    }
      if(ag==6)
          depot=DepotA;
                  else
          depot=DepotB; 
      if((depot.useMaterial(new int[]{pergamens,0,0,0,0})==0))
          return(checkActions(ag,1,false, "read"));
      int[] spell=newSpell(pergamens);
      depot.addSpell(spell);
          
      return(checkActions(ag,1,true, "read"));
  }

  
   /**
    **************************************
    *  Action pick 
    *  1, wood  2, pergamen 3, gold 
    **************************************
    */
  
  
  synchronized ActionResult gainSuperability(int ag)
  {
    	Location l = getAgPos(ag);
        MyAgents[ag].setSuperability();
    	clearLocations.add(l);
        return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "superb");
  }
  
  synchronized ActionResult pickWater(int ag)
  {
    Location l = getAgPos(ag);
    MyAgents[ag].addToBag(WorldModel.DWObjects.WATER);
    logger.info("Agent " + getAgNameBasedOnId(ag) + " picked water");
    return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "pick_water");   
  } 

  synchronized ActionResult pickStone(int ag,int x, int y)
  {
    Location l = getAgPos(ag);
    clearLocations.add(new Location(x,y));
    MyAgents[ag].addToBag(WorldModel.DWObjects.STONE);
    logger.info("Agent " + getAgNameBasedOnId(ag) + " picked a stone");
    return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "pick_stone");   
  } 

  
  synchronized ActionResult pickWood(int ag)
  {
    Location l = getAgPos(ag);
    clearLocations.add(l);
    MyAgents[ag].addToBag(WorldModel.DWObjects.WOOD);
    logger.info("Agent " + getAgNameBasedOnId(ag) + " picked a wood");
    return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "pick_wood");   
  } 

  synchronized ActionResult pickPergamen(int ag)
  {
    Location l = getAgPos(ag);
    clearLocations.add(l);
    MyAgents[ag].addToBag(WorldModel.DWObjects.PERGAMEN);
    logger.info("Agent " + getAgNameBasedOnId(ag) + " picked a pergamen");
    return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "pick_pergamen");  
  }
  
  synchronized ActionResult pickGold(int ag)
  {
    Location l = getAgPos(ag);
    clearLocations.add(l);
    MyAgents[ag].addToBag(WorldModel.DWObjects.GOLD);
    logger.info("Agent " + getAgNameBasedOnId(ag) + " picked a gold");
    return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "pick_gold");  
  }
   
  
  synchronized ActionResult pick(int ag, int dx, int dy)
  {
      if(MyAgents[ag].getCapacityLeft()<1){
           logger.warning("Agent " + getAgNameBasedOnId(ag) + " reached its capacity and cannot pick it!");
           return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick");
      }
          
    // stone -> spectacles -> gloves -> shoes -> wood -> pergamen -> gold
      
        Location l = getAgPos(ag);
        l.x+=dx; l.y+=dy;
      
        // Magicke predmety nepotrebuji kapacitu, jen spravny typ agenta
      
	if (hasObject(WorldModel.DWObjects.SPECTACLES, l.x, l.y))
	{
		if((ag==0)||(ag==3))
		    return gainSuperability(ag);
		else
		{
			logger.warning("Error " +getAgNameBasedOnId(ag)+" tries to take spectacles!");
			return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick");
		}
	}

	else if(hasObject(WorldModel.DWObjects.GLOVES, l.x, l.y))
	{
		if((ag==1)||(ag==4))
		    return gainSuperability(ag);
		else
		{
			logger.warning("Error " +getAgNameBasedOnId(ag)+" is trying to take gloves!");
			return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick");
		}
	}

	else if (hasObject(WorldModel.DWObjects.SHOES, l.x, l.y))
	{
		if((ag==2)||(ag==5))
		    return gainSuperability(ag);
		
		else
		{
			logger.warning("Error " +getAgNameBasedOnId(ag)+" is trying to take shoes!");
			return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick");
		}
	}
    
// na ostatni potrebujeme kapacitu minimalne 1 
// kazde zvednuti by melo
//          naplnit batoh prislusneho agenta
//          snizit volnou kapacitu
//          oznacit misto k procisteni (o objekt s nejvyssi prioritou)


      if (hasObject(WorldModel.DWObjects.WATER, l.x, l.y))
          if(!MyAgents[ag].bagEmpty()){
                logger.warning("Error " +getAgNameBasedOnId(ag)+" is trying to take water - bag must be empty!");
		return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick");

          }
          else
            return pickWater(ag);
      if (hasObject(WorldModel.DWObjects.STONE, l.x, l.y))
            return pickStone(ag,l.x,l.y);
      if (hasObject(WorldModel.DWObjects.WOOD, l.x, l.y))
            return pickWood(ag);
        else if (hasObject(WorldModel.DWObjects.PERGAMEN, l.x, l.y))
            return pickPergamen(ag);
        else if (hasObject(WorldModel.DWObjects.GOLD, l.x, l.y))     
            return pickGold(ag);
               
        logger.warning("Error " + getAgNameBasedOnId(ag) + " there is nothing to pick up!!!");
        return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick");
          
  }

  /**
    ****************
    *  Action dig *
    ****************
    */
  
  synchronized ActionResult pick(int ag){
        return(pick(ag,0,0));
  };
  
  synchronized ActionResult dig(int ag, String direction){
      // kopani ma ofset dle semru a muze se kopat pouze kamen
      int dx=0; int dy=0;
      if(direction.equals("n"))
            dy-=1;
      else if(direction.equals("s"))
            dy=1;
      else if(direction.equals("e"))
            dx-=1;
      else if(direction.equals("w"))
            dx=1;
      else{
          logger.warning("Agent " + getAgNameBasedOnId(ag) + " is trying to dig - wrong direction!");
          return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "pick"); 
      }
   
      Location l = getAgPos(ag);
      l.x+=dx; l.y+=dy;
      
      
      if(hasObject(WorldModel.DWObjects.STONE, l.x, l.y))
            return(pick(ag,dx,dy));
      
      logger.warning("Agent " + getAgNameBasedOnId(ag) + " is digging but no stone here!");
      return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "pick");
  };
   
 
  /**
    ****************
    *  Action drop *
    ****************
    */   
  
  
  synchronized ActionResult drop(int ag, String objectName)
  {

  
    Location l = getAgPos(ag);
    if (!l.equals(getDepot()))
    {
      logger.warning("Agent " + getAgNameBasedOnId(ag) + " is trying to drop something outside a depot!");
      return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "drop");
    }
    
    int object=WorldModel.DWObjects.getDropByName(objectName);
    
    if(object<0){
      logger.warning("Agent " + getAgNameBasedOnId(ag) + " is trying to drop something that cannot be dropped!");
      return checkActions(ag, MyAgents[ag].getStepsLeft(), false, "drop");
    }
  
    
    int dropped=0;
    depotModel depot;
    if(ag<=3)
        depot=DepotA;
                else
        depot=DepotB;
    
    for(int i=0;i<MyAgents[ag].getCapacity()&&(dropped*object)==0;i++)
        // object=0 - ulozit vsechno
        // dropped*object -> pokracovat ve snaze pokladat
        if(((object>0)&&(MyAgents[ag].getObjectInBag(i)==object))||
                    ((object==0)&&(MyAgents[ag].getObjectInBag(i)>0))){
            dropped++;
            depot.drop(objectToDepotIndex(MyAgents[ag].getObjectInBag(i)));
            MyAgents[ag].drop(i);
        }
        
    if(dropped==0)
      logger.warning("Agent " + getAgNameBasedOnId(ag) + " has nothing to drop"); 
      return checkActions(ag, MyAgents[ag].getStepsLeft(), true, "drop");
  }  
  
  
  
  
  
  /*****************************************************************************
  
            Inicializace mapy a modelu
  
  *****************************************************************************/
  
  
  
  
    
   private static void loadModel(String filename){
        File fl;
        char ch;
        System.out.println();
        fl = new File(filename);
        if(fl.exists()){
            System.out.println("Nahrávám mapu!");
             try{
                FileReader file = new FileReader(fl);
                    file.skip(3);
               for(int j=0;j<55;j++){ 
                for(int i=0;i<55;i++){
                    ch=(char)file.read();
                   switch(ch){
                        case 'V':model.add(WorldModel.DWObjects.WATER, i, j);
                                break;
                        case 'S':model.add(WorldModel.DWObjects.STONE, i, j);
                                break;
                    }
                    file.skip(1);
                }
                file.skip(1);
               }
             }catch(Exception e){};
             System.out.println();
        }   
        System.out.println("Nahrano!");
   }
   
   private static void makeForrest(WorldModel model){
       int centx,centy,posx,posy;
       
       centx=(int)(Math.random()*55); // spravne velikost gridu
       centy=(int)(Math.random()*55);
       
       for(int i=0;i<_FORREST_WOODS;i++){
           
       posx=(int)(Math.random()*_FORREST_RADIUS)-5+centx; // spravne velikost gridu
       posy=(int)(Math.random()*_FORREST_RADIUS)-5+centy;
       if(model.isFree(posx, posy))
           model.add(WorldModel.DWObjects.WOOD,posx,posy);
       }
   }
  
    private static void setItems(WorldModel model){
    	
    	Location l;
	Random random = new Random();
    	int gold_pos = 25 + random.nextInt(5);
    	int wood_pos = 5 + random.nextInt(5);

	int pergam_pos = 30;
	
        makeForrest(model);
        makeForrest(model);
 

    	for (int i = 0; i<pergam_pos; i++)
    	{
      	l = model.getFreePos();
      	if (l != null)
        	model.add(WorldModel.DWObjects.PERGAMEN, l.x, l.y);
    	}


    	for (int i = 0; i<gold_pos; i++)
    	{
      	l = model.getFreePos();
      	if (l != null)
        	model.add(WorldModel.DWObjects.GOLD, l.x, l.y);
    	}

    	for (int i = 0; i<wood_pos; i++)
    	{
      	l = model.getFreePos();
      	if (l != null)
        	model.add(WorldModel.DWObjects.WOOD, l.x, l.y);
    	}


	l= model.getFreePos();
	model.add(WorldModel.DWObjects.SPECTACLES, l.x, l.y);
        l= model.getFreePos();
	model.add(WorldModel.DWObjects.SPECTACLES, l.x, l.y);
	l= model.getFreePos();
	model.add(WorldModel.DWObjects.SHOES, l.x, l.y);
        l= model.getFreePos();
	model.add(WorldModel.DWObjects.SHOES, l.x, l.y);
	l= model.getFreePos();
	model.add(WorldModel.DWObjects.GLOVES, l.x, l.y);
        l= model.getFreePos();
	model.add(WorldModel.DWObjects.GLOVES, l.x, l.y);

//    	model.setInitialNbGolds(model.countObjects(WorldModel.DWObjects.GOLD));
//    	model.setInitialNbWoods(model.countObjects(WorldModel.DWObjects.WOOD));
    }


  /*
    *  World no. 1
    */
  static WorldModel world1() throws Exception
  {
    WorldModel model = WorldModel.create(55, 55, 6);
    model.setId("Scenario 1");
    loadModel("mapa1.csv");
    model.setDepot(16, 16);
    model.setAgPos(0, 10, 7);
    model.setAgPos(1, 11, 8);
    model.setAgPos(2, 10, 26);
    model.setAgPos(3, 11, 7);
    model.setAgPos(4, 12, 7);
    model.setAgPos(5, 12, 21);
    
    model.add(WorldModel.DWObjects.PERGAMEN, 15, 7);
    model.add(WorldModel.DWObjects.GOLD, 15, 7);
    model.add(WorldModel.DWObjects.WOOD, 15, 7);
    model.add(WorldModel.DWObjects.WOOD, 16, 7);
    model.add(WorldModel.DWObjects.WOOD, 17, 7);
    model.add(WorldModel.DWObjects.WOOD, 15, 8);
    model.add(WorldModel.DWObjects.WOOD, 16, 8);
    model.add(WorldModel.DWObjects.WOOD, 17, 8);
 
    setItems(model);
    
    return model;
  }

  /**
    *  World no. 2
    */
  static WorldModel world2() throws Exception
  {
    WorldModel model = WorldModel.create(55, 55, 6);
    model.setId("Scenario 2");
    loadModel("mapa2.csv");
    
    model.setDepot(16, 16);
    
    model.setAgPos(0, 1, 0);
    model.setAgPos(1, 20, 0);
    model.setAgPos(2, 6, 26);
    model.setAgPos(3, 1, 1);
    model.setAgPos(4, 20, 1);
    model.setAgPos(5, 6, 27);
    
    setItems(model);

    return model;
  }


  static WorldModel world3() throws Exception
  {
    WorldModel model = WorldModel.create(55, 55, 6);
    model.setId("Scenario 3");
    loadModel("mapa3.csv");
    model.setDepot(16, 16);
    model.setAgPos(0, 27, 6);
    model.setAgPos(1, 28, 7);
    model.setAgPos(2, 29, 8);
    model.setAgPos(3, 30, 9);
    model.setAgPos(4, 31, 10);
    model.setAgPos(5, 32, 11);
    setItems(model);
    return model;
  }
  
}
