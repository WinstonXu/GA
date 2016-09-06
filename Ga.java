import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.*;
import java.util.ArrayList;
import java.lang.InterruptedException;
import java.util.Random;

// Each MyPolygon has a color and a Polygon object
class MyPolygon {

  Polygon polygon;
  Color color;

  public MyPolygon(Polygon _p, Color _c) {
    polygon = _p;
    color = _c;
  }

  public Color getColor() {
    return color;
  }

  public Polygon getPolygon() {
    return polygon;
  }

}


// Each GASolution has a list of MyPolygon objects
class GASolution {

  ArrayList<MyPolygon> shapes;

  // width and height are for the full resulting image
  int width, height;

  public GASolution(int _width, int _height) {
    shapes = new ArrayList<MyPolygon>();
    width = _width;
    height = _height;
  }

  public void addPolygon(MyPolygon p) {
    shapes.add(p);
  }

  public ArrayList<MyPolygon> getShapes() {
    return shapes;
  }

  public int size() {
    return shapes.size();
  }

  public void setShapes(ArrayList<MyPolygon> _shapes){
    this.shapes = _shapes;
  }

  // Create a BufferedImage of this solution
  // Use this to compare an evolved solution with
  // a BufferedImage of the target image
  //
  // This is almost surely NOT the fastest way to do this...
  public BufferedImage getImage() {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    for (MyPolygon p : shapes) {
      Graphics g2 = image.getGraphics();
      g2.setColor(p.getColor());
      Polygon poly = p.getPolygon();
      if (poly.npoints > 0) {
        g2.fillPolygon(poly);
      }
    }
    return image;
  }

  public String toString() {
    return "" + shapes;
  }
}


// A Canvas to draw the highest ranked solution each epoch
class GACanvas extends JComponent{

  int width, height;
  GASolution solution;

  public GACanvas(int WINDOW_WIDTH, int WINDOW_HEIGHT) {
    width = WINDOW_WIDTH;
    height = WINDOW_HEIGHT;
  }

  public int getWidth() { return width; }
  public int getHeight() { return height; }

  public void setImage(GASolution sol) {
    solution = sol;
  }

  public void paintComponent(Graphics g) {
    BufferedImage image = solution.getImage();
    g.drawImage(image, 0, 0, null);
  }
}


class GA extends JComponent{

  GACanvas canvas;
  int width, height;
  BufferedImage realPicture;
  ArrayList<GASolution> population;
  ArrayList<Double> pop_fitness;

  // Adjust these parameters as necessary for your simulation
  double MUTATION_RATE = 0.004;
  double CROSSOVER_RATE = 0.6;
  int MAX_POLYGON_POINTS = 5;
  int MAX_POLYGONS = 10;
  int NUM_EPOCHS = 60000;

  public GA(GACanvas _canvas, BufferedImage _realPicture) {
    canvas = _canvas;
    realPicture = _realPicture;
    width = realPicture.getWidth();
    height = realPicture.getHeight();
    population = new ArrayList<GASolution>();
    pop_fitness = new ArrayList<Double>();

    // You'll need to define the following functions
    createPopulation(50);	// Make 50 new, random chromosomes
  }

  // YOUR CODE GOES HERE!
  //Creates inital population of 50 solutions with random colors
  //And random coordinates for polygons
  public void createPopulation(int num_chrom){
    Random rand = new Random();
    for(int i = 0; i < num_chrom; i++){
      GASolution newChrom = new GASolution(width, height);
      for(int j = 0; j < MAX_POLYGONS; j++){

        int[] x_coords = new int[MAX_POLYGON_POINTS];
        int[] y_coords = new int[MAX_POLYGON_POINTS];
        for(int k = 0; k < MAX_POLYGON_POINTS; k++){

          int rand_x = rand.nextInt(width);
          int rand_y = rand.nextInt(height);
          x_coords[k] = rand_x;
          y_coords[k] = rand_y;
        }
        Polygon p = new Polygon(x_coords, y_coords, MAX_POLYGON_POINTS);
        int r_val = rand.nextInt(255);
        int g_val = rand.nextInt(255);
        int b_val = rand.nextInt(255);
        Color c = new Color(r_val, g_val, b_val);
        MyPolygon mp = new MyPolygon(p,c);
        newChrom.addPolygon(mp);
      }
      population.add(newChrom);
    }
    getPopFitness();
  }
  //Finds euclidean distance of target color and solution color
  public double fitness(GASolution sol){
    BufferedImage curr = sol.getImage();
    double counter = 0;
    Random rand = new Random();
    int samplesize = 100;
    for(int i = 0; i < samplesize; i ++){
      int x = rand.nextInt(width);
      int y = rand.nextInt(height);
      Color c_color = new Color(curr.getRGB(x,y));
      Color r_color = new Color(realPicture.getRGB(x,y));
      double r_diff = Math.pow(c_color.getRed()-r_color.getRed(),2);
      double g_diff = Math.pow(c_color.getGreen()-r_color.getGreen(),2);
      double b_diff = Math.pow(c_color.getBlue()-r_color.getBlue(),2);
      counter += Math.sqrt(r_diff+g_diff+b_diff);
    }
    return 1/(counter/samplesize);
  }

  public void getPopFitness(){

    pop_fitness = new ArrayList<Double>();
    for(GASolution sol: population){
      pop_fitness.add(fitness(sol));
    }
  }
  //Based off class example
  public GASolution pickFitParent(){
    double totalFitness = 0;
    Random rand = new Random();
    for(int i = 0; i < pop_fitness.size(); i++){
      totalFitness += pop_fitness.get(i);
    }
    double r = Math.random()*totalFitness;
    int index = -1;
    while(r > 0){
      index++;
      r -= pop_fitness.get(index);
    }
    return population.get(index);
  }

  public Polygon copyPoly(MyPolygon mp){
    Polygon p = mp.getPolygon();
    int[] xcopy = new int[MAX_POLYGON_POINTS];
    int[] ycopy = new int[MAX_POLYGON_POINTS];
    for(int i= 0; i < MAX_POLYGON_POINTS; i++){
      xcopy[i] = p.xpoints[i];
      ycopy[i] = p.ypoints[i];
    }
    Polygon copy = new Polygon(xcopy, ycopy, MAX_POLYGON_POINTS);
    return copy;
  }
  //Averages color values of parents
  //Randomly chooses some number of polygons to move to child from each parent
  public ArrayList<MyPolygon> crossover(GASolution p1, GASolution p2){
    //average of colors?
    ArrayList<MyPolygon> chrom_1 = p1.getShapes();
    ArrayList<MyPolygon> chrom_2 = p2.getShapes();
    ArrayList<MyPolygon> new_chrom = new ArrayList<MyPolygon>();
    ArrayList<Color> new_colors = new ArrayList<Color>();

    for(int i = 0; i < chrom_1.size(); i++){
      Color parent1 = chrom_1.get(i).getColor();
      Color parent2 = chrom_2.get(i).getColor();
      int new_red = (parent1.getRed()+parent2.getRed())%256;
      int new_green = (parent1.getGreen()+parent2.getGreen())%256;
      int new_blue = (parent1.getBlue()+parent2.getBlue())%256;
      Color child_color = new Color(new_red, new_green, new_blue);
      new_colors.add(child_color);
    }
    int cutOff = (int) (Math.random()*chrom_1.size());
    for(int j = 0; j < cutOff; j++){
        Polygon p = copyPoly(chrom_1.get(j));
        new_chrom.add(new MyPolygon(p, new_colors.get(j)));
      //Need to deep copy polygons?
    }
    for(int k = cutOff; k < chrom_1.size(); k++){
      Polygon p = copyPoly(chrom_2.get(k));
      new_chrom.add(new MyPolygon(p, new_colors.get(k)));
    }

    return new_chrom;
  }
//Randomly reassigns color and coordinates
  public ArrayList<MyPolygon> mutate(ArrayList<MyPolygon> child){
    for(int i = 0; i < child.size(); i++){
      double random = Math.random();
      if(random < MUTATION_RATE){
        MyPolygon kid = child.get(i);
        Color c = kid.color;
        int red = (int) (Math.random()*256);
        int green = (int) (Math.random()*256);
        int blue = (int) (Math.random()*256);
        kid.color = new Color(red,green,blue);
        int[] x_coord = kid.polygon.xpoints;
        int[] y_coord = kid.polygon.ypoints;
        for(int j = 0; j < x_coord.length; j++){
          x_coord[j] = (int) (Math.random()*(width+1));
          y_coord[j] = (int) (Math.random()*(height+1));
        }
        kid.polygon.xpoints = x_coord;
        kid.polygon.ypoints = y_coord;
      }
    }
    return child;
  }

  public void createNewPopulation(){
    ArrayList<GASolution> newPop = new ArrayList<GASolution>();
    for(int i = 0; i < population.size(); i++){
      GASolution parent1 = pickFitParent();
      GASolution parent2 = pickFitParent();
      ArrayList<MyPolygon> child = crossover(parent1, parent2);
      child = mutate(child);
      GASolution newSolution = new GASolution(width, height);
      //loop to add mypolygons
      newSolution.setShapes(child);
      double c_o_prob = Math.random();
      if(c_o_prob < CROSSOVER_RATE){
        newPop.add(newSolution);
      }
      else if(c_o_prob < CROSSOVER_RATE+(1.0-CROSSOVER_RATE)/2.0){
        newPop.add(parent1);
      }
      else{
        newPop.add(parent2);
      }
    }
    population = newPop;
  }

  public void runSimulation() {

    for(int i = 0; i < NUM_EPOCHS; i++){
      createNewPopulation();
      getPopFitness();
      int index = 0;
      double max = pop_fitness.get(index);
      for(int j = 1; j < pop_fitness.size(); j++){
        if(pop_fitness.get(j) > max){
          index = j;
          max = pop_fitness.get(j);
        }
      }
      if(i % 50 == 0){
        GASolution best = population.get(index);
        canvas.setImage(best);
        canvas.repaint();
        System.out.println(pop_fitness.get(index) + " "+i);
      }
    }
  }

  public static void main(String[] args) throws IOException {

    String realPictureFilename = "test.jpg";

    BufferedImage realPicture = ImageIO.read(new File(realPictureFilename));

    JFrame frame = new JFrame();
    frame.setSize(realPicture.getWidth(), realPicture.getHeight());
    frame.setTitle("GA Simulation of Art");

    GACanvas theCanvas = new GACanvas(realPicture.getWidth(), realPicture.getHeight());
    frame.add(theCanvas);
    frame.setVisible(true);

    GA pt = new GA(theCanvas, realPicture);
    pt.runSimulation();
  }
}
