/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

/**
 *
 * @author zborilf
 */
  public class agentModel{
      // v pripade superschopnosti secte
      private final int[][] steps=new int[][]{{1,2,3,1},{0,0,3,0}};
      private final int[][] capacities=new int[][]{{1,4,2,0},{0,4,0,0}};
      private final int[][] visibilities=new int[][]{{3,1,1,0},{3,0,0,0}};
      private final int[] superabilities=new int[]{
            WorldModel.DWObjects.SHOES,
            WorldModel.DWObjects.GLOVES,
            WorldModel.DWObjects.SPECTACLES};
      private int superability=0;
      private int[] bag;
      private int steps_left;
      private int agent_type; // 0,1,2 
      private int agent_team; // 0,1
      
    public int getObjectInBag(int i){
        return(bag[i]);
    }
    
    public synchronized void drop(int i){
        bag[i]=0;
    }
      
    public int getVisibility(){
        return(visibilities[0][agent_type]+
                        superability*visibilities[1][agent_type]); 
    }       
      
    public void setSuperability(){
        superability=1;
    }
    
    
    public int getStepsTotal(){
        return(steps[0][agent_type]+
                        superability*steps[1][agent_type]);
    }   
  
    public int getStepsLeft(){
        return(steps_left);
    }   
    
    public void resetSteps(){
        steps_left=getStepsTotal(); 
    }
    
    public synchronized boolean decreaseSteps(int i){
        if(i>steps_left){
            steps_left=0;
            return(false);
        }
        steps_left-=i;
        return(true);
    } 
    
    public int getCapacity(){
        return(capacities[0][agent_type]+
                        superability*capacities[1][agent_type]); 
    }

    public boolean bagEmpty(){
        int capacity=getCapacity();
        for(int i=0;i<capacity;i++)
            if(bag[i]!=0)
                return(false);
        return(true);
    }
    
    public int getCapacityLeft(){
        int capacity=getCapacity();
        int capacityLeft=0;
        for(int i=0;i<capacity;i++)
            if(bag[i]==0)
                capacityLeft++;
        return(capacityLeft);
    }

    
    public int[] getItems(){
      return(bag.clone());
    }
    
    public synchronized boolean addToBag(int item){
        for(int i=0;i<bag.length;i++)
            if(bag[i]==0){
                bag[i]=item;
                return(true);
            }
        return(false);
    }
  
    public agentModel(int type){
        // p,s,wt,wd,g
        if(type>5){
            agent_type=3;
            agent_team=type-6;
            System.out.println("Vytvarim druida "+agent_type+" tymu "+agent_team);
        }
        else
        {
            bag=new int[]{0,0,0,0,0,0,0,0};
            agent_type=type % 3;
            agent_team=type / 3;
            System.out.println("Vytvarim agenta "+type+" typu "+agent_type+" tymu "+agent_team);
        //resetSteps();
        }
    }
  }