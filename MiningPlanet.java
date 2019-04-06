/**
  *  AGS project - environment connection with Jason
  *  Based on gold-miners example
  *  @ Jan Horáček <ihoracek@fit.vutbr.cz>
  *  @ Frantisek Zboril jr <zborilf@fit.vut.cz>
  */

package mining;

// Environment code for project jasonTeamSimLocal.mas2j

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.grid.Location;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.LinkedList;
import mining.WorldModel.ActionResult;

public class MiningPlanet extends jason.environment.Environment
{
  private Logger logger = Logger.getLogger("jasonTeamSimLocal.mas2j." + MiningPlanet.class.getName());
  
  WorldModel  model;
  WorldView   view;
  
  int     simId    = 3; // type of environment
  int     nbWorlds = 3;
  int     step     = 0;
  int[]   alive    = new int[]{1,1,1,1,1,1};

  int     sleep    = 0;
  boolean running  = true;
  boolean hasGUI   = true;
  
  public static final int SIM_TIME = 60;  // in seconds

  Term                    up       = Literal.parseLiteral("do(up)");
  Term                    down     = Literal.parseLiteral("do(down)");
  Term                    right    = Literal.parseLiteral("do(right)");
  Term                    left     = Literal.parseLiteral("do(left)");
  Term                    skip     = Literal.parseLiteral("do(skip)");
  Term                    pick     = Literal.parseLiteral("do(pick)");
  Term                    drop     = Literal.parseLiteral("do(drop)");

  
  public enum Move
  {
    UP, DOWN, RIGHT, LEFT
  };

  @Override
  public void init(String[] args)
  {
    logger.setLevel(Level.INFO);
    hasGUI = args[2].equals("yes"); 
    sleep  = Integer.parseInt(args[1]);
    initWorld(Integer.parseInt(args[0]));
  }
  
  public int getSimId() {
    return simId;
  }
  
  /**
    *  Actualize step count of agent
    */
  private void signalizeStep()
  {
    logger.info("Starting round: " + step);
    removePercept(Literal.parseLiteral("step(" + (step-1) + ")"));
    addPercept(Literal.parseLiteral("step(" + step + ")"));
    step++;
  }
  
  public void setSleep(int s)
  {
    sleep = s;
  }

  @Override
  public void stop()
  {
    running = false;
    super.stop();
  }

  /*
  *     Druidi akce
  */

  int getInt(String str){
       switch (str){
              case "0": return(0);
              case "1": return(1);
              case "2": return(2);
              case "3": return(3);
              case "4": return(4);
              case "5": return(5);
       }
       return(-1);
  }
  
  public ActionResult executeDruidAction(String ag, int agID, Structure action){
      if (action.getFunctor().toString().equals("do") && action.getTerm(0).toString().equals("read")){
          String pergamensS=action.getTerm(1).toString();
          int pergamens=getInt(pergamensS);
          if((pergamens>=3)&&(pergamens<=5))
              return(model.readSpell(agID, pergamens));
      }
      
      if (action.getFunctor().toString().equals("do") && action.getTerm(0).toString().equals("create")){
          int[] spell=new int[4];
          spell[0]=getInt(action.getTerm(1).toString());
          spell[1]=getInt(action.getTerm(2).toString());
          spell[2]=getInt(action.getTerm(3).toString());
          spell[3]=getInt(action.getTerm(4).toString());
          return(model.createItem(agID, spell));
      }
      return(ActionResult.MISTAKE); // ??
  }
  
  /**
    *  Action called from Jason
    */
 
  
  @Override
  public boolean executeAction(String ag, Structure action)
  {
    ActionResult result = ActionResult.ERROR;
    try
    {
      if (sleep > 0)
      {
        Thread.sleep(sleep);
      }
      
      // get the agent id based on its name
      int agId = model.getAgIdBasedOnName(ag);      

      if (action.equals(skip)) // pro vsechny agenty
      {
        result = model.skip(agId);
      } 
      
      else if((agId==6)||(agId==7)){ // pro driudy
          result=executeDruidAction(ag, agId, action); 
          updateDruidsPercepts();
          view.udpateCollectedItems();
      }
  
      else{ // pro delniky
      
          Set confusedBefore = model.confusedAgents(agId);

          Location l = model.getAgPos(agId);

          if (model.deadly(l.x, l.y)) {
              System.out.println("Agent " + agId + " umira na " + l.x + "/" + l.y);
              alive[agId] = 0;
          }

          if (alive[agId] == 0) {
              System.out.println("agent " + agId + " je mrtvy");
              result = model.skip(agId);
          } else if (action.equals(up)) {
              result = model.move(Move.UP, agId);
          } else if (action.equals(down)) {
              result = model.move(Move.DOWN, agId);
          } else if (action.equals(right)) {
              result = model.move(Move.RIGHT, agId);
          } else if (action.equals(left)) {
              result = model.move(Move.LEFT, agId);
          } else if (action.equals(skip)) {
              result = model.skip(agId);
          } else if (action.equals(pick)) {
              result = model.pick(agId);
              view.udpateCollectedItems();
          } else if (action.getFunctor().toString().equals("do") && action.getTerm(0).toString().equals("dig")) {
              result = model.dig(agId, action.getTerm(1).toString());
              view.udpateCollectedItems();

          } else if (action.getFunctor().toString().equals("drop")) {
              // drop([stone,water,wood,pergamen,gold,all])
              result = model.drop(agId, action.getTerm(0).toString());
              view.udpateCollectedItems();
              updateDruidsPercepts();
          } else {
              logger.info("executing: " + action + ", but not implemented!");
          }
      }

      
      if (result.isNotError()) //nepodvadel nekdo?
      {
        if (result == ActionResult.MISTAKE)
        {
          logger.warning("Action " + action + " of agent " + ag + " is not possible!!!");
        }
        if(agId<6)
            updateAgPercept(ag, agId);
        else
            updateDruidsPercepts();

        if (result == ActionResult.ROUND_FINISHED)
        {
          for (int i = 0; i < 6; i++)
          {
            removePerceptsByUnif(model.getAgNameBasedOnId(i),Literal.parseLiteral("moves_left(_)"));
            addPercept(model.getAgNameBasedOnId(i), Literal.parseLiteral("moves_left(" + model.MyAgents[i].getStepsLeft() + ")"));
          }
	  informAgsEnvironmentChanged();
	  Thread.sleep(2);
          signalizeStep();
        }
        else if (result == ActionResult.SIMULATION_ENDS)
        {
          logger.info("Game over");
          stop();
        }
        informAgsEnvironmentChanged();
        return true;
      }    

    }
    
    catch (InterruptedException e){}
    
    catch (Exception e)
    {
      logger.log(Level.SEVERE, "error executing " + action + " for " + ag, e);
    }
    return false;
  }
  
  
  /**
    *  Called on initialisation only
    */
  public void initWorld(int w) {
    simId = w;
    try
    {
      switch (w)
      {
        case 1: model = WorldModel.world1(); break;
        case 2: model = WorldModel.world2(); break;
        case 3: model = WorldModel.world3(); break;

        default:
          logger.info("Invalid index!");
          return;
      }
      
      clearPercepts();
      addPercept(Literal.parseLiteral("grid_size(" + model.getWidth() + "," + model.getHeight() + ")"));
      addPercept(Literal.parseLiteral("depot("+ model.getDepot().x + "," + model.getDepot().y + ")"));
      if (hasGUI)
      {
        view = new WorldView(model);
        view.setEnv(this);
        view.udpateCollectedItems();
      }
      updateAgsPercept();
      signalizeStep();
      informAgsEnvironmentChanged();
    }
    catch (Exception e)
    {
      logger.warning("Error creating world "+e);
    }
  }
  
  /**
    *  Called when simulation ends
    */
  public void endSimulation()
  {
    addPercept(Literal.parseLiteral("end_of_simulation(" + simId + ",0)"));
    informAgsEnvironmentChanged();
    if (view != null) view.setVisible(false);
    WorldModel.destroy();
  }

 
  
  /**
    *  Apdates percepts for all agents
    */
  
  
  private void updateDruidPercepts(String druidName, int[] items, int mus, int mthem, LinkedList<int[]> spells){
      removePerceptsByUnif(druidName,Literal.parseLiteral("depot(_,_)"));
      removePerceptsByUnif(druidName,Literal.parseLiteral("money(_,_)"));     
      addPercept(druidName,Literal.parseLiteral("depot(pergamen,"+items[0]+")"));
      addPercept(druidName,Literal.parseLiteral("depot(stone,"+items[1]+")"));
      addPercept(druidName,Literal.parseLiteral("depot(water,"+items[2]+")"));
      addPercept(druidName,Literal.parseLiteral("depot(wood,"+items[3]+")"));
      addPercept(druidName,Literal.parseLiteral("depot(gold,"+items[4]+")"));  
      addPercept(druidName,Literal.parseLiteral("money(us,"+mus+")"));  
      addPercept(druidName,Literal.parseLiteral("money(them,"+mthem+")"));  
      for(int[] spell:spells){
          String spellS="spell(";
          spellS=String.format("spell(%d,%d,%d,%d)",spell[0],spell[1],spell[2],spell[3]);
          addPercept(druidName,Literal.parseLiteral(spellS));
      }
  }
  
  private void updateDruidsPercepts(){
      int ma=model.DepotA.getMoney();
      int[] ia=model.DepotA.getItemsCounts();
      String nameA="aDruid";
      int mb=model.DepotB.getMoney();
      int[] ib=model.DepotB.getItemsCounts();
      String nameB="bDruid";
      LinkedList<int[]> spellsA=model.DepotA.getSpells();
      LinkedList<int[]> spellsB=model.DepotB.getSpells();
      updateDruidPercepts(nameA,ia,ma,mb,spellsA);
      updateDruidPercepts(nameB,ib,mb,ma,spellsB);
  }
  
  private void updateAgsPercept()
  {
    for (int i = 0; i < 6; i++) 
      updateAgPercept(model.getAgNameBasedOnId(i), i);
    updateDruidsPercepts();  
  }

  
  /**
    *  Update percepts for specified agent
    *  @note agName and agId must match!
    */
  
  private void updateAgPercept(String agName, int ag)
  {
    int[] bag;
    String item;
    clearPercepts(agName);    
    
    // its location
    Location l = model.getAgPos(ag);
    addPercept(agName, Literal.parseLiteral("pos(" + l.x + "," + l.y + ")"));
        
    //show some variables to agent
    
    item="";
    StringBuffer items=new StringBuffer("");
    bag=model.MyAgents[ag].getItems();
    int j;
    int capacity=model.MyAgents[ag].getCapacity();
    if(capacity==0)
        items=items.append("null");
    for(int i=0;i<capacity;i++){
        if(i!=0)
            items.append(",");
        else
            items.append("[");
        j=bag[i];
        item=WorldModel.DWObjects.getObjectName(j);
              items.append(item);
    }
    items.append("]");
    addPercept(agName, Literal.parseLiteral("bag("+items+")"));
    if (model.MyAgents[ag].getCapacityLeft()==0)
         addPercept(agName, Literal.parseLiteral("bag_full"));
    
    addPercept(agName, Literal.parseLiteral("carrying_capacity(" + model.MyAgents[ag].getCapacity()+")"));
    addPercept(agName, Literal.parseLiteral("moves_left(" + model.MyAgents[ag].getStepsLeft()+")"));
    addPercept(agName, Literal.parseLiteral("moves_per_round(" + model.MyAgents[ag].getStepsTotal()+")"));
    
    //friends of agent
    int i;
    if (ag < 3)
    {
      for (i = 0; i < 3; i++)
      {
        if (i != ag)
          addPercept(agName, Literal.parseLiteral("friend(" + model.getAgNameBasedOnId(i) + ")"));
      }
    }
    else
    {
      for (i = 3; i < 6; i++)
      {
        if (i != ag)
          addPercept(agName, Literal.parseLiteral("friend(" + model.getAgNameBasedOnId(i) + ")"));
      }
    }
    
    // what's around

	
    int dosah=1;
    if ((model.getAgIdBasedOnName(agName)==0) || (model.getAgIdBasedOnName(agName)==3))
     dosah=model.MyAgents[ag].getVisibility();
    
    for (int x = l.x - dosah; x <= l.x + dosah; x++)
    {
      for (int y = l.y - dosah; y <= l.y + dosah; y++)
      {
        updateAgPercept(agName, x, y);
      }
    }
	// FZjr ^^^^^
  }
  
  /**
    *  Update agent percept (what is on position x,y)
    */
  private void updateAgPercept(String agName, int x, int y)
  {
    if (model == null || !model.inGrid(x,y)) return;
  
    int agId = model.getAgIdBasedOnName(agName);

    
    int[] objectsList=WorldModel.DWObjects.getObjectsList();
    
    for(int i=0;i<objectsList.length;i++){
        if(model.hasObject(objectsList[i],x,y)){
            String objectName=WorldModel.DWObjects.getObjectName(objectsList[i]);
            addPercept(agName, Literal.parseLiteral(objectName+"(" + x + "," + y + ")"));
        }
    }

      if (model.hasObject(WorldModel.AGENT, x, y)) //is there an agent on that position
      {
        for (int other = 0; other<6; other++)
        {
          Location l = model.getAgPos(other);
          if (!(l.x == x && l.y == y))
            continue;
          if (agId != other)
          {
            if ((agId < 3 && other < 3) || (agId >= 3 && other >= 3))
            {
              addPercept(agName, Literal.parseLiteral("ally(" + x + "," + y + ")"));
            }
            else
            {
              addPercept(agName, Literal.parseLiteral("enemy(" + x + "," + y + ")"));
            }
          }
        }
      }
    }
}
