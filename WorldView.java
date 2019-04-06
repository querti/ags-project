/**
 *  AGS project - environment visualisation
 *  Based on gold-miners example
 *  @author Jan Horacek <ihoracek@fit.vutbr.cz>
 */

package mining;

import jason.environment.grid.GridWorldView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class WorldView extends GridWorldView
{

  static final int _NOScenarios=6;
  MiningPlanet env = null;
  Color[] idColor = new Color[6];
  
  /**
    *  Constructor
    */
  public WorldView(WorldModel model)
  {
    super(model, "Mining World", 600);
    idColor[0] = new Color(0,0,255);
    idColor[1] = new Color(120,30,255);
    idColor[2] = new Color(0,120,255);
    idColor[3] = new Color(255,0,0);
    idColor[4] = new Color(205,80,150);
    idColor[5] = new Color(255,150,0);
    setVisible(true);
    repaint();
  }

  public void setEnv(MiningPlanet env)
  {
    this.env = env;
    scenarios.setSelectedIndex(env.getSimId()-1);
  }
  
  JLabel    jlMouseLoc;
  JComboBox scenarios;
  JSlider   jSpeed;
  JLabel    jGoldsC;
  JLabel    jWoodsC;

  /**
    *  Initialize components
    */
  @Override
  public void initComponents(int width)
  {
    super.initComponents(width);
    scenarios = new JComboBox();
    for (int i=1; i<=_NOScenarios; i++)
    {
      scenarios.addItem(i);
    }
    JPanel args = new JPanel();   
    args.setLayout(new BoxLayout(args, BoxLayout.Y_AXIS));

    JPanel sp = new JPanel(new FlowLayout(FlowLayout.LEFT));
    sp.setBorder(BorderFactory.createEtchedBorder());
    sp.add(new JLabel("Scenario:"));
    sp.add(scenarios);
    
    jSpeed = new JSlider();
    jSpeed.setMinimum(0);
    jSpeed.setMaximum(400);
    jSpeed.setValue(50);
    jSpeed.setPaintTicks(true);
    jSpeed.setPaintLabels(true);
    jSpeed.setMajorTickSpacing(100);
    jSpeed.setMinorTickSpacing(20);
    jSpeed.setInverted(true);
    Hashtable<Integer,Component> labelTable = new Hashtable<Integer,Component>();
    labelTable.put( 0, new JLabel("max") );
    labelTable.put( 200, new JLabel("speed") );
    labelTable.put( 400, new JLabel("min") );
    jSpeed.setLabelTable( labelTable );
    JPanel p = new JPanel(new FlowLayout());
    p.setBorder(BorderFactory.createEtchedBorder());
    p.add(jSpeed);
    
    args.add(sp);
    args.add(p);

    JPanel msg = new JPanel();
    msg.setLayout(new BoxLayout(msg, BoxLayout.Y_AXIS));
    msg.setBorder(BorderFactory.createEtchedBorder());
    
    p = new JPanel(new FlowLayout(FlowLayout.CENTER));
    p.add(new JLabel("Click on the cells to add new pieces of gold."));
    msg.add(p);
    p = new JPanel(new FlowLayout(FlowLayout.CENTER));
    p.add(new JLabel("(mouse at:"));
    jlMouseLoc = new JLabel("0,0)");
    p.add(jlMouseLoc);
    msg.add(p);
    p = new JPanel(new FlowLayout(FlowLayout.CENTER));
    p.add(new JLabel("Depot A (P,S,Wt,Wd,G):"));
    jGoldsC = new JLabel("0,0,0,0,0");
    p.add(jGoldsC);
    msg.add(p);
    p = new JPanel(new FlowLayout(FlowLayout.CENTER));
    p.add(new JLabel("Depot A (P,S,Wt,Wd,G)::"));
    jWoodsC = new JLabel("0,0,0,0,0");
    p.add(jWoodsC);
    msg.add(p);

    JPanel s = new JPanel(new BorderLayout());
    s.add(BorderLayout.WEST, args);
    s.add(BorderLayout.CENTER, msg);
    getContentPane().add(BorderLayout.SOUTH, s);        

    // Events handling
    jSpeed.addChangeListener(new ChangeListener(){
                                    public void stateChanged(ChangeEvent e)
                                    {
                                      if (env != null)
                                      {
                                          env.setSleep((int)jSpeed.getValue());
                                      }
                                    }
                                  });

    scenarios.addItemListener(new ItemListener(){
                                    public void itemStateChanged(ItemEvent ievt) {
                                      int w = ((Integer)scenarios.getSelectedItem()).intValue();
                                      if (env != null && env.getSimId() != w)
                                      {
                                        env.endSimulation();
                                        env.initWorld(w);
                                      }
                                    }            
                                  });
    
    getCanvas().addMouseMotionListener(new MouseMotionListener(){
                                              public void mouseDragged(MouseEvent e) {}
                                              public void mouseMoved(MouseEvent e)
                                              {
                                                int col = e.getX() / cellSizeW;
                                                int lin = e.getY() / cellSizeH;
                                                if (col >= 0 && lin >= 0 && col < getModel().getWidth() && lin < getModel().getHeight())
                                                {
                                                  jlMouseLoc.setText(col+","+lin+")");
                                                }
                                              }            
                                            });
  }
  
  public void udpateCollectedItems() {
      WorldModel wm = (WorldModel)model;
      String sm1=" {earned:"+wm.DepotA.getMoney()+"}";
      String sm2=" {earned:"+wm.DepotB.getMoney()+"}";
      String s1=wm.getDepotItems(1);
      String s2=wm.getDepotItems(2);
      jGoldsC.setText(s1+sm1);
      jWoodsC.setText(s2+sm2);
  }
  
  
  /**
    *  Draw own data types
    */
  @Override
  public void draw(Graphics g, int x, int y, int object)
  {
    switch (object)
    {
      case WorldModel.DWObjects.DEPOT:   drawDepot(g, x, y);  break;
      case WorldModel.DWObjects.GOLD:    drawGold(g, x, y);  break;
      case WorldModel.DWObjects.WOOD:    drawWood(g, x, y);  break;
      case WorldModel.DWObjects.SHOES:   drawShoes(g, x, y); break;
      case WorldModel.DWObjects.GLOVES:  drawGloves(g, x, y); break;
      case WorldModel.DWObjects.SPECTACLES: drawSpectacles(g, x, y); break;
      case WorldModel.DWObjects.WATER: drawWater(g, x, y); break;
      case WorldModel.DWObjects.STONE: drawStone(g, x, y); break;
      case WorldModel.DWObjects.PERGAMEN: drawPergamen(g, x, y); break;
    }
  }

  /**
    *  Draw an agent
    */
  @Override
  public void drawAgent(Graphics g, int x, int y, Color c, int id)
  {
    if(id>=6) return; // druidy nekreslime
    
    Color agColor = idColor[id];
    super.drawAgent(g, x, y, agColor, -1);
    g.setColor(Color.black);
    if (id%3 == 0)
    {
      g.drawRect(x * cellSizeW, y * cellSizeH, 3, 3);
    }
    else if (id%3 == 1)
    {
      g.drawRect((x+1) * cellSizeW - 3, y * cellSizeH, 3, 3);
    }
    else if (id%3 == 2)
    {
      g.drawRect(x * cellSizeW, (y+1) * cellSizeH - 3, 3, 3);
    }
    g.setColor(Color.white);
    int count = id;
    drawString(g, x, y, defaultFont, String.valueOf(count));
  }

  /**
    *  Draw depot
    */
  public void drawDepot(Graphics g, int x, int y)
  {
    g.setColor(Color.gray);
    g.fillRect(x * cellSizeW, y * cellSizeH, cellSizeW, cellSizeH);
    g.setColor(Color.pink);
    g.drawRect(x * cellSizeW + 2, y * cellSizeH + 2, cellSizeW - 4, cellSizeH - 4);
    g.drawLine(x * cellSizeW + 2, y * cellSizeH + 2, (x + 1) * cellSizeW - 2, (y + 1) * cellSizeH - 2);
    g.drawLine(x * cellSizeW + 2, (y + 1) * cellSizeH - 2, (x + 1) * cellSizeW - 2, y * cellSizeH + 2);
  }

  /**
    *  Draw gold
    */
  

  private void drawRectangle(Graphics g, int x, int y){
    g.drawRect(x * cellSizeW, y * cellSizeH, cellSizeW, cellSizeH);
    g.fillRect(x * cellSizeW, y * cellSizeH, cellSizeW, cellSizeH);
  }


  private void drawOval(Graphics g, int x, int y){
    g.drawRect(x * cellSizeW + 2, y * cellSizeH + 2, cellSizeW - 4, cellSizeH - 4);
    int[] vx = new int[4];
    int[] vy = new int[4];
    vx[0] = x * cellSizeW + (cellSizeW / 2);
    vy[0] = y * cellSizeH;
    vx[1] = (x + 1) * cellSizeW;
    vy[1] = y * cellSizeH + (cellSizeH / 2);
    vx[2] = x * cellSizeW + (cellSizeW / 2);
    vy[2] = (y + 1) * cellSizeH;
    vx[3] = x * cellSizeW;
    vy[3] = y * cellSizeH + (cellSizeH / 2);
    g.fillPolygon(vx, vy, 4);
  }

  public void drawGold(Graphics g, int x, int y)
  {
    g.setColor(Color.yellow);
    drawOval(g,x,y);
  }

   /**
    *  Draw wood
    */
  public void drawWood(Graphics g, int x, int y)
  {
    g.setColor(Color.green);
    drawOval(g,x,y);
  }

  public void drawWater(Graphics g, int x, int y)
  {
    g.setColor(Color.blue);
    drawRectangle(g,x,y);
  }

  public void drawStone(Graphics g, int x, int y)
  {
    g.setColor(Color.gray);
    drawRectangle(g,x,y);
  }

  public void drawPergamen(Graphics g, int x, int y)
  {
    g.setColor(Color.white);
    drawOval(g,x,y);
  }


//FZjr

  public void drawShoes(Graphics g, int x, int y)
  {
    g.setColor(Color.darkGray);
    g.fillOval( x*cellSizeW+2, (y+1)*cellSizeH-5, cellSizeW-4, 4);
    g.fillOval( x*cellSizeW+3, y*cellSizeH+1, 4, cellSizeH-2);

  //  g.fillOval( x*cellSizeW+5, y*cellSizeH+2, 4, 7);
  }
  

  public void drawGloves(Graphics g, int x, int y)
  {
    g.setColor(Color.black);
    g.fillRect( x*cellSizeW+3, y*cellSizeH+5, 7, 7);
   // g.fillRect( x*cellSizeW+1, y*cellSizeH-0, 6, 6);
    g.drawLine(x*cellSizeW+3, y*cellSizeH+4, x*cellSizeW+3, y*cellSizeH+2);
    g.drawLine(x*cellSizeW+5, y*cellSizeH+4, x*cellSizeW+5, y*cellSizeH+1);
    g.drawLine(x*cellSizeW+7, y*cellSizeH+4, x*cellSizeW+7, y*cellSizeH+1);
    g.drawLine(x*cellSizeW+9, y*cellSizeH+4, x*cellSizeW+9, y*cellSizeH+2);
    g.drawLine(x*cellSizeW+10, y*cellSizeH+7, x*cellSizeW+11, y*cellSizeH+3);
    g.drawLine(x*cellSizeW+10, y*cellSizeH+8, x*cellSizeW+11, y*cellSizeH+3);

  }

  public void drawSpectacles(Graphics g, int x, int y)
  {
    g.setColor(Color.black);
    g.drawOval( x*cellSizeW+3, y*cellSizeH+4, 4, 4);
    g.drawLine( x*cellSizeW+7, y*cellSizeH+5,x*cellSizeW+9, y*cellSizeH+5);
    g.drawOval( x*cellSizeW+9, y*cellSizeH+4, 4, 4);
  }
  
//FZjr_end

  public static void main(String[] args) throws Exception
  {
    MiningPlanet env = new MiningPlanet();
    env.init(new String[] {"5","50","yes"});
  }
}
