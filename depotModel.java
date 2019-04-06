/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import java.util.LinkedList;

/**
 *
 * @author zborilf
 */
  public class depotModel{
        int money;
        int[] items; // NO perg, stone, water, wood, gold
        LinkedList<int[]> spells=new LinkedList<int[]>();
        
        public boolean drop(int item){
            if(item==-1)
                    return(false);    
            items[item]++;
            return(true);
        }
        
        public LinkedList<int[]> getSpells(){
            return(spells);
        }
        
        public boolean checkMaterial(int[] needed){
            for(int i=0;i<=4;i++)
                if(items[i]<needed[i])
                    return(false);
            return(true);
        }
        
        public synchronized int useMaterial(int[] material){
            if(!checkMaterial(material))
                return(0);
            int materialUsed=0;
            for(int i=0;i<=4;i++){
                items[i]-=material[i];
                materialUsed+=material[i];
            }
            return(materialUsed);
        }
        
        public synchronized void addSpell(int[] spell){
            spells.add(spell);
        }
       
        
        public int getItemCount(int item){
            switch(item){
                case WorldModel.DWObjects.PERGAMEN:return(items[0]);
                case WorldModel.DWObjects.STONE:return(items[1]);
                case WorldModel.DWObjects.WATER:return(items[2]);
                case WorldModel.DWObjects.WOOD:return(items[3]);
                case WorldModel.DWObjects.GOLD:return(items[4]);
            }
            return(0);
        }
        
        public boolean matchSpell(int[] sp, int[] spell){
            for(int i=0;i<4;i++)  // mag. cislo, pocet druhy ingerdienci
                if(sp[i]!=spell[i])
                    return(false);
            return(true);
        }
        
        public boolean matchSpells(int[] spell){
            for(int[] sp:spells){    
                if(matchSpell(sp,spell))
                        return(true);
            }            
            return(false);               
        }
        
        public int[] getItemsCounts(){
            return(items);
        }
        
        public String getItemsString(){
            return(
                    new String(items[0]+"/"+items[1]+"/"+items[2]+"/"+items[3]+"/"+items[4])
                    );
        }
        
        public void raiseMoney(int amount){
            money +=amount;
        }
        
        public int getMoney(){
            return(money);
        }
        
        public depotModel(){
            money=0;
            items=new int[]{13,50,40,50,40};
        }
        
    }
 
