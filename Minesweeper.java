import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// to represent a Cell in the world MineSweeper
class Cell {
  // true if this cell contains a mine
  boolean hasMine;
  // ArrayList of the cells neighboring this one
  ArrayList<Cell> neighbors;
  // has the cell been pressed on / reveals the mine
  boolean revealed;
  // has the cell been flagged
  boolean flag;


  // main constructor
  Cell(boolean hasMine, ArrayList<Cell> neighbors, boolean revealed, boolean flag) {
    // cells initially have no mines
    this.hasMine = false;
    // cells start off with no neighbors
    this.neighbors = new ArrayList<Cell>();
    // all cells are initially covered
    // need to press to reveal
    this.revealed = false;
    // all cells are initially not flagged
    // need to right click
    this.flag = false;
  }

  // mutates a cell and makes it contain a mine
  void placeMine() {
    this.hasMine = true;
  }
  
  // reveals all the mines if the game is lost
  void gameReveal() {
    if (!this.revealed) {
      this.revealed = hasMine;
    }
  }

  // does the cell already have a flag?
  boolean hasFlag() {
    return this.flag;
  }

  // is this cell revealed?
  boolean isRevealed() {
    return this.revealed;
  }

  // mutates a cell and makes it flagged
  void placeFlag() {
    this.flag = true;
  }

  // removes flag from a cell
  void removeFlag() {
    this.flag = false;
  }

  // mutates a cell and makes it a revealed one
  void reveal() {
    this.revealed = true;
  }

  // to draw all the different cell variants
  public WorldImage draw() {
    WorldImage mineImage = new CircleImage(10, OutlineMode.SOLID, Color.red);
    WorldImage cellImage = new OverlayImage(
        new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.black),
        new RectangleImage(30, 30, OutlineMode.SOLID, Color.LIGHT_GRAY));
    WorldImage cellPressedImage = new RectangleImage(30, 30, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage cellWithMineImage = new OverlayImage(mineImage,
        new OverlayImage(new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.black),
            new RectangleImage(30, 30, OutlineMode.SOLID, Color.BLUE)));
    WorldImage flagImage = new RotateImage(new EquilateralTriangleImage(19, "solid", Color.RED),
        90);
    WorldImage pole = new RectangleImage(3, 20, OutlineMode.SOLID, Color.ORANGE);
    WorldImage flagWithPole = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, pole, 0, 0,
        flagImage);
    WorldImage cellWithFlagImage = new OverlayImage(flagWithPole, cellImage);
    WorldImage surroundingMinesImage = new TextImage(Integer.toString(this.countMines()), 10,
        FontStyle.BOLD, this.countColor());
    WorldImage countMinesCellImage = new OverlayImage(surroundingMinesImage, cellImage);
 
    if (this.hasMine && this.revealed) {
      return cellWithMineImage;
    }
    else if (this.flag) {
      return cellWithFlagImage;
    }
    else if (this.revealed && this.countMines() == 0) {
      return cellPressedImage;
    }
    else if (this.revealed && this.countMines() > 0) {
      return countMinesCellImage;
    }
    else {
      return cellImage;
    }
  }

  // adds a neighbor c to this cell
  void addNeighbor(Cell c) {
    this.neighbors.add(c);
  }

  // counts the number of mines around this cell
  int countMines() {
    int m = 0;
    for (int i = 0; i < this.neighbors.size(); i++) {
      if (this.neighbors.get(i).hasMine) {
        m += 1;
      }
    }
    return m;
  }

  // floods the cells around adjacent mines if their 
  // count is 0 as well
  void floodFill() {
    this.reveal();
    if (this.countMines() == 0) {
      for (Cell n : neighbors) {
        if (n.countMines() > 0 && !this.hasMine) {
          n.reveal();
        }
        if (!n.isRevealed() && n.countMines() == 0) {
          n.reveal();
          n.floodFill();
        }
      }
    }
  }

  // to have different colors depending on the number of
  // surrounding mines
  Color countColor() {
    if (this.countMines() == 1) {
      return Color.BLUE;
    }
    else if (this.countMines() == 2) {
      return Color.GREEN;
    }
    else if (this.countMines() == 3) {
      return Color.MAGENTA;
    }
    else if (this.countMines() == 4) {
      return Color.YELLOW;
    }
    else if (this.countMines() == 5) {
      return Color.ORANGE;
    }
    else if (this.countMines() == 6) {
      return Color.PINK;
    }
    else if (this.countMines() == 7) {
      return Color.RED;
    }
    else {
      return Color.WHITE;
    }
  }
}

// to represent the MineSweeper World
class MineSweeper extends World {
  int rows;
  int columns;
  ArrayList<ArrayList<Cell>> board;
  Random rand;
  int mineCap;
  int seconds;
  int minutes;
  boolean gameOver;
  boolean win;
  int flagCount;

  // constructor for choosing own amount of mines
  MineSweeper(int rows, int columns, Random rand, int mineCap) {
    this.rows = rows;
    this.columns = columns;
    // board initially starts off empty
    this.board = new ArrayList<ArrayList<Cell>>();
    // random value for placing mines
    this.rand = rand;
    //
    this.mineCap = mineCap;
    // fills board with lists of rows and cells
    this.board = this.makeGrid();
    // links cells to each of their neighbors
    // and fills their neighbor list
    this.linkNeighbors();
    // randomizes the placement of mines
    this.randomizeMines();
    this.seconds = 0;
    this.minutes = 0;
    this.gameOver = false;
    this.win = false;
    this.flagCount = mineCap;
  }

  // draws the current scene
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(rows * 30, columns * 30);
    WorldImage clock = new TextImage("【" + this.minutesAsString() + " : " 
        + this.secondsAsString() + "】", 30, FontStyle.BOLD, Color.red);
    WorldImage flagImage = new RotateImage(new EquilateralTriangleImage(19, "solid", Color.RED),
        90);
    WorldImage pole = new RectangleImage(3, 20, OutlineMode.SOLID, Color.ORANGE);
    WorldImage flagWithPole = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, pole, 0, 0,
        flagImage);
    WorldImage flagCount = new BesideImage(flagWithPole, 
        new TextImage(Integer.toString(this.flagCount), 25, Color.DARK_GRAY));
    WorldImage winText = new TextImage(("You win"), rows * 2, FontStyle.BOLD, Color.BLACK);
    WorldImage clickedMineText = new TextImage(("You clicked a mine :("), rows * 2,
        FontStyle.BOLD, Color.BLACK);
    WorldImage endTimeText = new TextImage("Time: " 
        + this.minutesAsString() + " : " + this.secondsAsString(), 
        rows, FontStyle.BOLD, Color.BLACK);
    WorldImage restartText = new TextImage("Press r to restart" , 
        rows, FontStyle.BOLD, Color.BLACK);
    WorldImage whiteRect = new RectangleImage(rows * 23, columns * 5, 
        OutlineMode.SOLID, Color.WHITE);
    WorldImage blueRect = new RectangleImage(rows * 25, columns * 6, OutlineMode.SOLID, Color.cyan);
    WorldImage whiteOnBlueRect = new OverlayImage(whiteRect, blueRect);
    for (int r = 0; r < rows; r++) {
      // in-play game scene
      for (int c = 0; c < columns; c++) {
        scene.placeImageXY(this.board.get(r).get(c).draw(), r * 30 + 15, c * 30 + 15);
        scene.placeImageXY(clock, rows * 30 - 95, columns * 30 + 25);
        scene.placeImageXY(flagCount, rows + 20, columns * 30 + 25);
      }
    }
    // scene made when game is over
    if (this.gameOver) {
      scene.placeImageXY(new OverlayImage(new AboveImage(
          clickedMineText, endTimeText, restartText), whiteOnBlueRect), 
          rows * 15, columns * 15); 
    }
    // scene if game is won
    if (this.win) {
      scene.placeImageXY(new OverlayImage(new AboveImage(
          winText, endTimeText), whiteOnBlueRect), rows * 15, columns * 15);
    }
    return scene;
  }

  // randomize amount of mines
  public void randomizeMines() {
    int m = 0;
    int randRow = rand.nextInt(rows);
    int randColumn = rand.nextInt(columns);
    while (m < mineCap) {
      if (!board.get(randRow).get(randColumn).hasMine) {
        this.board.get(randRow).get(randColumn).placeMine();
        m++;
      }
      randRow = rand.nextInt(rows);
      randColumn = rand.nextInt(columns);
    }
  }

  // generate board
  ArrayList<ArrayList<Cell>> makeGrid() {
    // adds empty rows list to board
    for (int r = 0; r < rows; r++) {
      ArrayList<Cell> newRow = new ArrayList<Cell>();
      // add cells in columns for each row
      for (int c = 0; c < columns; c++) {
        newRow.add(new Cell(false, new ArrayList<Cell>(), false, false));
      }
      board.add(newRow);
    }
    return board;
  }

  // counts the number of mines neighboring a particular cell
  // and adds it to their list
  void linkNeighbors() {
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < columns; c++) {

        Cell cell = board.get(r).get(c);

        if (r > 0) {
          cell.addNeighbor(board.get(r - 1).get(c));
        }
        if (r < rows - 1) {
          cell.addNeighbor(board.get(r + 1).get(c));
        }
        if (c > 0) {
          cell.addNeighbor(board.get(r).get(c - 1));
        }
        if (c < columns - 1) {
          cell.addNeighbor(board.get(r).get(c + 1));
        }
        if (r > 0 && c > 0) {
          cell.addNeighbor(board.get(r - 1).get(c - 1));
        }
        if (r < rows - 1 && c > 0) {
          cell.addNeighbor(board.get(r + 1).get(c - 1));
        }
        if (r > 0 && c < columns - 1) {
          cell.addNeighbor(board.get(r - 1).get(c + 1));
        }
        if (r < rows - 1 && c < columns - 1) {
          cell.addNeighbor(board.get(r + 1).get(c + 1));
        }
      }
    }
  }
  
  // checks to see if the game has been won
  // if so, sets win boolean to true
  void gameWin() {
    int target = rows * columns - mineCap;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < columns; c++) {
        Cell cell = board.get(r).get(c);
        if (!cell.hasMine && cell.isRevealed()) {
          target -= 1;
        }
      }
    }
    if (target == 0) {
      this.win = true;
    }
  }
 

  // reveals all the cells if the game is over
  void gameOver() {
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < columns; c++) {
        Cell cell = board.get(r).get(c);
        cell.gameReveal();
      }
    }
  }

  // timer for how long the game has been running
  public void onTick() {
    if (!this.gameOver && !this.win) {
      if (this.seconds > 0 && this.seconds % 59 == 0) {
        this.seconds = 0;
        this.minutes += 1;
      }
      else {
        this.seconds += 1;
      }
    }
  }

  // to display one digit seconds with a 0 before it
  public String secondsAsString() {
    if (this.seconds < 10) {
      return "0" + Integer.toString(seconds);
    }
    else {
      return Integer.toString(seconds);
    }
  }

  // to display one digit seconds with a 0 before it
  public String minutesAsString() {
    if (this.minutes < 10) {
      return "0" + Integer.toString(minutes);
    }
    else {
      return Integer.toString(minutes);
    }
  }
  
  // to restart the world
  void restartGame() {
    this.seconds = 0;
    this.minutes = 0;
    this.gameOver = false;
    this.win = false;
    this.flagCount = mineCap;
    this.board = new ArrayList<ArrayList<Cell>>();
    // fills board with lists of rows and cells
    this.board = this.makeGrid();
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < columns; c++) {
        Cell cell = board.get(r).get(c);
        cell.revealed = false;
        cell.flag = false;
        cell.neighbors = new ArrayList<Cell>();
      }
    }
    // links cells to each of their neighbors
    // and fills their neighbor list
    this.linkNeighbors();
    // randomizes the placement of mines
    this.randomizeMines();
  }

  // handler for mouse clicks clicks
  public void onMouseClicked(Posn pos, String buttonName) {
    int cellWidth = 30;
    int cellHeight = 30;
    int r = Math.floorDiv(pos.x, cellWidth);
    int c = Math.floorDiv(pos.y, cellHeight);
    Posn cellCoor = new Posn(r * cellWidth, c * cellHeight);
    int cellRight = cellCoor.x + cellWidth;
    int cellBottom = cellCoor.y + cellHeight;
    
    // conditions where mouse clicks shouldn't be registered
    // gameOver, win, or out of bounds
    if (this.gameOver || this.win || r > this.rows - 1 || c > this.columns - 1) {
      return;
    }
   
    // cell that is clicked
    Cell cellClicked = board.get(r).get(c);
    
    // placing a flag
    if (pos.x >= cellCoor.x && pos.x < cellRight 
        && pos.y >= cellCoor.y && pos.y < cellBottom
        && buttonName.equals("RightButton") 
        && !cellClicked.hasFlag() && !cellClicked.isRevealed()) {
      board.get(r).get(c).placeFlag();
      this.flagCount -= 1;
    }
    
    // removing a flag
    else if (pos.x >= cellCoor.x && pos.x < cellRight 
        && pos.y >= cellCoor.y && pos.y < cellBottom
        && buttonName.equals("RightButton") && cellClicked.hasFlag()) {
      board.get(r).get(c).removeFlag();
      this.flagCount += 1;
    }
    
    // when a cell is pressed
    else if (pos.x >= cellCoor.x && pos.x < cellRight 
        && pos.y >= cellCoor.y && pos.y < cellBottom
        && buttonName.equals("LeftButton") && !cellClicked.hasFlag()) {
      board.get(r).get(c).floodFill();
      this.gameWin();
    }

    // when a mine is pressed
    if (pos.x >= cellCoor.x && pos.x < cellRight 
        && pos.y >= cellCoor.y && pos.y < cellBottom
        && buttonName.equals("LeftButton") && cellClicked.hasMine
        && cellClicked.isRevealed() && !cellClicked.hasFlag()) {
      this.gameOver = true;
      this.gameOver();
    }
  }
  
  // for restarting the game
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.restartGame();
    }
  }
}

// example and tests for the world
class ExamplesMinesweeper {

  // to run the game
  void testBigBang(Tester t) {

    // to choose game difficulty when run
    // type in console
    System.out.println("Minesweeper:");
    System.out.println("Type Beginner, Expert, Intermediate, or Custom for different game modes!");
    Scanner difficulty = new Scanner(System.in);
    String input = difficulty.next();
    int rows = 0;
    int columns = 0;
    int mines = 0;

    if (input.equals("Beginner")) {
      rows = 9;
      columns = 9;
      mines = 10;
    }

    if (input.equals("Expert")) {
      rows = 16;
      columns = 16;
      mines = 40;
    }

    if (input.equals("Intermediate")) {
      rows = 30;
      columns = 16;
      mines = 99;
    }

    if (input.equals("Custom")) {
      System.out.println("How many columns would you like? ");
      rows = difficulty.nextInt();
      System.out.println("How many rows would you like? ");
      columns = difficulty.nextInt();
      System.out.println("How many mines would you like? ");
      mines = difficulty.nextInt();
    }

    MineSweeper world = new MineSweeper(rows, columns, new Random(), mines);
    int worldWidth = world.rows * 30;
    int worldHeight = world.columns * 30 + 50;
    double tickRate = 1;
    world.bigBang(worldWidth, worldHeight, tickRate);
  }
  
  // regular game board
  MineSweeper m1;
  Cell c0;
  Cell c1;
  Cell c2;
  Cell c3;
  Cell c4;
  Cell c5;
  Cell c6;
  Cell c7;
  Cell c8;
  ArrayList<Cell> r0;
  ArrayList<Cell> r1;
  ArrayList<Cell> r2;

  // tiny intermediate game board
  MineSweeper m2;
  Cell c9;
  Cell c10;
  Cell c11;
  Cell c12;
  ArrayList<Cell> r3;
  ArrayList<Cell> r4;
  ArrayList<Cell> r5;

  void init() {
    m1 = new MineSweeper(3, 3, new Random(20), 2);
    /* 
     *  +----+----+----+
     *  | c0 | c3 | c6 |
     *  +----+----+----+
     *  | c1 | c4 | c7 |
     *  +----+----+----+
     *  | c2 | c5 | c8 |
     *  +----+----+----+
     * 
     *  +----+----+----+
     *  |  ● |||||||||||
     *  +----+----+----+
     *  |||||||||||  ● | 
     *  +----+----+----+
     *  ||||||||||||||||
     *  +----+----+----+
     */
    c0 = m1.board.get(0).get(0);
    c1 = m1.board.get(0).get(1);
    c2 = m1.board.get(0).get(2);
    c3 = m1.board.get(1).get(0);
    c4 = m1.board.get(1).get(1);
    c5 = m1.board.get(1).get(2);
    c6 = m1.board.get(2).get(0);
    c7 = m1.board.get(2).get(1);
    c8 = m1.board.get(2).get(2);
    r0 = new ArrayList<Cell>(Arrays.asList(c0, c1, c2));
    r1 = new ArrayList<Cell>(Arrays.asList(c3, c4, c5));
    r2 = new ArrayList<Cell>(Arrays.asList(c6, c7, c8));
    
    m2 = new MineSweeper(2, 2, new Random(15), 1);
    /*
     * +----+----+
     * | c9 | c11|
     * +----+----+ 
     * | c10| c12|
     * +----+----+
     * 
     * +----+----+
     * |||||||||||
     * +----+----+ 
     * ||||||  ● |
     * +----+----+
     * 
     */
    c9 = m2.board.get(0).get(0);
    c10 = m2.board.get(0).get(1);
    c11 = m2.board.get(1).get(0);
    c12 = m2.board.get(1).get(1);
    r3 = new ArrayList<Cell>(Arrays.asList(c9, c10)); 
    r4 = new ArrayList<Cell>(Arrays.asList(c11, c12)); 
    
  }
 
 
  // CELL CLASS TESTS:   
  // tests for the method placeMine 
  void testPlaceMine(Tester t) {
    init();
    // originally doesn't have a mine
    t.checkExpect(c3.hasMine, false);
    c3.placeMine();
    t.checkExpect(c3.hasMine, true);
    
    // has a mine already
    t.checkExpect(c0.hasMine, true);
    c0.placeMine();
    t.checkExpect(c0.hasMine, true);
  }
  
  // tests for the method gameReveal
  void testGameReveal(Tester t) {
    init();
    // cells that have mines should be revealed
    t.checkExpect(c0.hasMine, true);
    c0.gameReveal();
    t.checkExpect(c0.revealed, true);
    
    // cells that don't have mines and 
    // haven't been revealed should stay the same
    t.checkExpect(c1.revealed, false);
    c1.gameReveal();
    t.checkExpect(c1.revealed, false);
    
    // cells that don't have mines 
    // and have already been revealed should 
    // stay the same 
    t.checkExpect(c12.revealed, false);
    c12.reveal();
    c12.gameReveal();
    t.checkExpect(c12.revealed, true);
    
  }
  
  void testAllFlagMethods(Tester t) {
    init();
    // tests for has, place, and remove, flag
    
    // placing and removing a flag 
    t.checkExpect(c3.hasFlag(), false);
    c3.placeFlag(); // now has a flag
    t.checkExpect(c3.hasFlag(), true);
    c3.removeFlag(); // no longer has a flag
    t.checkExpect(c3.hasFlag(), false);
    
    // just placing 
    t.checkExpect(c4.flag, false);
    c4.placeFlag();
    t.checkExpect(c4.flag, true);
    
    // removing 
    c4.removeFlag();
    t.checkExpect(c4.flag, false);
  }
  
  void testIsRevealed(Tester t) {
    init();
    // a not revealed cell
    t.checkExpect(c4.isRevealed(), false);
    
    // a revealed cell
    c7.reveal();
    t.checkExpect(c7.isRevealed(), true);
  }
  
  void testReveal(Tester t) {
    init();
    // revealing a cell with mine 
    t.checkExpect(c0.revealed, false);
    c0.reveal();
    t.checkExpect(c0.revealed, true);
    
    // revealing a regular cell
    t.checkExpect(c5.revealed, false);
    c5.reveal();
    t.checkExpect(c5.revealed, true);
  }
  
  // tests for the method draw
  void testDraw(Tester t) {
    init();
    WorldImage mineImage = new CircleImage(10, OutlineMode.SOLID, Color.red);
    WorldImage cellImage = new OverlayImage(
        new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.black),
        new RectangleImage(30, 30, OutlineMode.SOLID, Color.LIGHT_GRAY));
    WorldImage cellPressedImage = new RectangleImage(30, 30, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage cellWithMineImage = new OverlayImage(mineImage,
        new OverlayImage(new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.black),
            new RectangleImage(30, 30, OutlineMode.SOLID, Color.BLUE)));
    WorldImage flagImage = new RotateImage(new EquilateralTriangleImage(19, "solid", Color.RED),
        90);
    WorldImage pole = new RectangleImage(3, 20, OutlineMode.SOLID, Color.ORANGE);
    WorldImage flagWithPole = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, pole, 0, 0,
        flagImage);
    WorldImage cellWithFlagImage = new OverlayImage(flagWithPole, cellImage);
    WorldImage surroundingMinesImage = new TextImage("1", 10,
        FontStyle.BOLD, Color.blue);
    WorldImage countMinesCellImage = new OverlayImage(surroundingMinesImage, cellImage);
    // regular cell
    c0.reveal();
    t.checkExpect(c0.draw(), cellWithMineImage);
    // cell with mine
    t.checkExpect(c1.draw(), cellImage);
    // cell that's been pressed
    c2.reveal();
    t.checkExpect(c2.draw(), cellPressedImage);
    // cell with count
    c10.reveal();
    t.checkExpect(c10.draw(), countMinesCellImage);
    // flagged cell
    c11.placeFlag();
    t.checkExpect(c11.draw(), cellWithFlagImage);
  }
  
  // tests for the method addNeighbor
  void testAddNeighbor(Tester t) {
    init();
    t.checkExpect(c3.neighbors.containsAll(
        new ArrayList<Cell>(Arrays.asList(c0, c1, c4, c6, c7))), true);
    c3.addNeighbor(c2);
    t.checkExpect(c3.neighbors.containsAll(
        new ArrayList<Cell>(Arrays.asList(c0, c1, c2, c4, c6, c7))), true);
  }
  
  // tests for the method countMines
  void testCountMines(Tester t) {
    init();
    t.checkExpect(c1.countMines(), 1);
    t.checkExpect(c8.countMines(), 1);
    t.checkExpect(c0.countMines(), 0);
  }
  
  void testFloodFill(Tester t) {
    init();
    // flooding a cell with neighboring mines
    // (should only reveal that one cell)
    t.checkExpect(c11.revealed, false);
    // floodFill on c11
    // (this onMouseClicked call calls floodFill on c11)
    m2.onMouseClicked(new Posn(30, 0), "LeftButton");
    // check mutations
    t.checkExpect(c11.revealed, true);
    // checking to see that its neighbors haven't been affected
    t.checkExpect(c9.revealed, false);
    t.checkExpect(c10.revealed, false);
    
    // flooding a cell with no neighboring mines
    MineSweeper m4 = new MineSweeper(4, 3, new Random(15), 2);
    /*
     *  +----+----+----+----+
     *  |    | c21| c23| c25|
     *  +----+----+----+----+
     *  |    | c22| c24| c26|
     *  +----+----+----+----+
     *  |    |    |    |    |
     *  +----+----+----+----+
     * 
     *  after flooding c25 it should look like this:
     *  +----+----+----+----+
     *  |    |  1 |||||||||||
     *  +----+----+----+----+
     *  |    |  2 |  1 |  1 |
     *  +----+----+----+----+
     *  |    |    |    |    |
     *  +----+----+----+----+
     */
    
    Cell c21 = m4.board.get(1).get(0);
    Cell c22 = m4.board.get(1).get(1);
    Cell c23 = m4.board.get(2).get(0);
    Cell c24 = m4.board.get(1).get(1);
    Cell c25 = m4.board.get(3).get(0);
    Cell c26 = m4.board.get(3).get(1);
    
    // nothing is revealed
    t.checkExpect(c21.isRevealed(), false);
    t.checkExpect(c22.isRevealed(), false);
    t.checkExpect(c23.isRevealed(), false);
    t.checkExpect(c24.isRevealed(), false);
    t.checkExpect(c25.isRevealed(), false);
    t.checkExpect(c26.isRevealed(), false);
    // floodFill on c26
    // (this onMouseClicked call calls floodFill on c26)
    m4.onMouseClicked(new Posn(90, 0), "LeftButton");
    // its neighbors are revealed as well since
    // it had no neighboring mines
    t.checkExpect(c21.isRevealed(), true);
    t.checkExpect(c22.isRevealed(), true);
    t.checkExpect(c23.isRevealed(), true);
    t.checkExpect(c24.isRevealed(), true);
    t.checkExpect(c25.isRevealed(), true);
    t.checkExpect(c26.isRevealed(), true);
    
  }
  
  void testCountColor(Tester t) {
    init();
    // making  a bigger board to test all cases
    MineSweeper m3 = new MineSweeper(20, 16, new Random(21), 180);
    
    // 1
    Cell c13 = m3.board.get(19).get(4);
    t.checkExpect(c13.countColor(), Color.BLUE);
    
    // 2
    Cell c14 = m3.board.get(16).get(15);
    t.checkExpect(c14.countColor(), Color.GREEN);

    // 3
    Cell c15 = m3.board.get(0).get(0);
    t.checkExpect(c15.countColor(), Color.MAGENTA);

    // 4
    Cell c16 = m3.board.get(1).get(15);
    t.checkExpect(c16.countColor(), Color.YELLOW);

    // 5
    Cell c17 = m3.board.get(7).get(11);
    t.checkExpect(c17.countColor(), Color.ORANGE);

    // 6
    Cell c18 = m3.board.get(2).get(10);
    t.checkExpect(c18.countColor(), Color.PINK);

    // 7
    Cell c19 = m3.board.get(3).get(10);
    t.checkExpect(c19.countColor(), Color.RED);

    // 8 
    Cell c20 = m3.board.get(13).get(4);
    t.checkExpect(c20.countColor(), Color.WHITE);

  }
  
  // MINESWEEPER CLASS TESTS:
  // tests for the method makeScene
  void testMakeScene(Tester t) {
    init();
    WorldImage mineImage = new CircleImage(10, OutlineMode.SOLID, Color.red);
    WorldImage cellImage = new OverlayImage(
        new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.black),
        new RectangleImage(30, 30, OutlineMode.SOLID, Color.LIGHT_GRAY));
    WorldImage cellPressedImage = new RectangleImage(30, 30, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage cellWithMineImage = new OverlayImage(mineImage,
        new OverlayImage(new RectangleImage(30, 30, OutlineMode.OUTLINE, Color.black),
            new RectangleImage(30, 30, OutlineMode.SOLID, Color.BLUE)));
    WorldImage flagImage = new RotateImage(new EquilateralTriangleImage(19, "solid", Color.RED),
        90);
    WorldImage clock = new TextImage("【00 : 00】", 30, FontStyle.BOLD, Color.red);
    WorldImage pole = new RectangleImage(3, 20, OutlineMode.SOLID, Color.ORANGE);
    WorldImage flagWithPole = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, pole, 0, 0,
        flagImage);
    WorldImage flagCount = new BesideImage(flagWithPole, new TextImage("2", 25, Color.DARK_GRAY));
    WorldImage flagCount2x2 = new BesideImage(flagWithPole, 
        new TextImage("1", 25, Color.DARK_GRAY));
    WorldImage cellWithFlagImage = new OverlayImage(flagWithPole, cellImage);
    WorldImage surroundingMinesImage = new TextImage("1", 10,
        FontStyle.BOLD, Color.blue);
    WorldImage countMinesCellImage = new OverlayImage(surroundingMinesImage, cellImage);
    WorldImage clickedMineText = new TextImage(("You clicked a mine :("), 4,
        FontStyle.BOLD, Color.BLACK);
    WorldImage winText = new TextImage(("You win"), 4, FontStyle.BOLD, Color.BLACK);
    WorldImage endTimeText = new TextImage("Time: " + "00" 
        + " : " + "00", 2, FontStyle.BOLD, Color.BLACK);
    WorldImage restartText = new TextImage("Press r to restart" , 2, FontStyle.BOLD, Color.BLACK);
    WorldImage whiteRect = new RectangleImage(46, 10, OutlineMode.SOLID, Color.WHITE);
    WorldImage blueRect = new RectangleImage(50, 12, OutlineMode.SOLID, Color.cyan);
    WorldImage whiteOnBlueRect = new OverlayImage(whiteRect, blueRect);
   
    // 3 x 3 initial grid
    WorldScene w1 = new WorldScene(90, 90);
    w1.placeImageXY(cellImage, 15, 15);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 15, 45);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 15, 75);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 45, 15);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 45, 45);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 45, 75);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 75, 15);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 75, 45);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    w1.placeImageXY(cellImage, 75, 75);
    w1.placeImageXY(clock, -5, 115);
    w1.placeImageXY(flagCount, 23, 115);
    t.checkExpect(m1.makeScene(), w1);
    
    // 3 x 3 intermediate grid
    init();
    WorldScene w2 = new WorldScene(90, 90);
    // has a number, mine, flag, and pressed cell on it 
    c0.reveal();
    c1.placeFlag();
    c8.reveal();
    c2.reveal();
    w2.placeImageXY(cellWithMineImage, 15, 15);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellWithFlagImage, 15, 45);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellPressedImage, 15, 75);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellImage, 45, 15);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellImage, 45, 45);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellImage, 45, 75);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellImage, 75, 15);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(cellImage, 75, 45);
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    w2.placeImageXY(countMinesCellImage, 75, 75); 
    w2.placeImageXY(clock, -5, 115);
    w2.placeImageXY(flagCount, 23, 115);
    t.checkExpect(m1.makeScene(), w2);
    
    // 2 x 2 game over
    init();
    m2.gameOver = true;
    WorldScene w3 = new WorldScene(60, 60);
    w3.placeImageXY(cellImage, 15, 15);
    w3.placeImageXY(clock, -35, 85);
    w3.placeImageXY(flagCount2x2, 22, 85);
    w3.placeImageXY(cellImage, 15, 45);
    w3.placeImageXY(clock, -35, 85);
    w3.placeImageXY(flagCount, 22, 85);
    w3.placeImageXY(cellImage, 45, 15);
    w3.placeImageXY(clock, -35, 85);
    w3.placeImageXY(flagCount2x2, 22, 85);
    w3.placeImageXY(cellImage, 45, 45);
    w3.placeImageXY(clock, -35, 85);
    w3.placeImageXY(flagCount2x2, 22, 85);
    w3.placeImageXY(new OverlayImage(new AboveImage(
        clickedMineText, endTimeText, restartText), whiteOnBlueRect), 30, 30);
    t.checkExpect(m2.makeScene(), w3);
    
    // 2 x 2 win 
    init();
    m2.win = true;
    WorldScene w4 = new WorldScene(60, 60);
    w4.placeImageXY(cellImage, 15, 15);
    w4.placeImageXY(clock, -35, 85);
    w4.placeImageXY(flagCount2x2, 22, 85);
    w4.placeImageXY(cellImage, 15, 45);
    w4.placeImageXY(clock, -35, 85);
    w4.placeImageXY(flagCount, 22, 85);
    w4.placeImageXY(cellImage, 45, 15);
    w4.placeImageXY(clock, -35, 85);
    w4.placeImageXY(flagCount2x2, 22, 85);
    w4.placeImageXY(cellImage, 45, 45);
    w4.placeImageXY(clock, -35, 85);
    w4.placeImageXY(flagCount2x2, 22, 85);
    w4.placeImageXY(new OverlayImage(new AboveImage(
        winText, endTimeText), whiteOnBlueRect), 30, 30);
    t.checkExpect(m2.makeScene(), w4);
  }
 
  // test for the method secondsAsString and minutesAsString
  void testAsString(Tester t) {
    init();
    // 1 digit seconds 
    m1.seconds = 3;
    t.checkExpect(m1.secondsAsString(), "03");
    
    // 2 digit seconds
    m2.seconds = 13;
    t.checkExpect(m2.secondsAsString(), "13");
    
    // 1 digit minutes
    m1.minutes = 1;
    t.checkExpect(m1.minutesAsString(), "01");
    
    // 2 digit minutes
    m2.minutes = 13;
    t.checkExpect(m2.minutesAsString(), "13");
    
  }
  
  // tests for the method onKeyEvent and restartGame
  void testRestartGame(Tester t) {
    init();
    ArrayList<Cell> m1Row = m1.board.get(1);
    t.checkExpect(m1.board.get(1), m1Row);
    // calls m1.restartGame
    m1.onKeyEvent("r");
    // new row has been created, shows has restarted
    t.checkExpect((m1.board.get(1) == m1Row), false);
    
  }
  
  // tests for the method gameWin
  void testGameWin(Tester t) {
    init();
    // winning m2 board
    t.checkExpect(m2.win, false);
    // clicking all cells without mines
    m2.onMouseClicked(new Posn(0, 0), "LeftButton");
    m2.onMouseClicked(new Posn(0, 30), "LeftButton");
    m2.onMouseClicked(new Posn(30, 0), "LeftButton");
    // win :)
    t.checkExpect(m2.win, true);
    
    // winning m1 board
    t.checkExpect(m1.win, false);
    // clicking all cells without mines
    m1.onMouseClicked(new Posn(0, 30), "LeftButton");
    m1.onMouseClicked(new Posn(0, 60), "LeftButton");
    m1.onMouseClicked(new Posn(30, 0), "LeftButton");
    m1.onMouseClicked(new Posn(30, 30), "LeftButton");
    m1.onMouseClicked(new Posn(30, 60), "LeftButton");
    m1.onMouseClicked(new Posn(60, 0), "LeftButton");
    m1.onMouseClicked(new Posn(60, 60), "LeftButton");
    // win :)
    t.checkExpect(m1.win, true);
  }
  
  // tests for the method gameOver
  void testGameOver(Tester t) {
    init();
    // losing m1 board
    t.checkExpect(m1.gameOver, false);
    // clicking cell with mine
    m1.onMouseClicked(new Posn(0, 0), "LeftButton");
    // lose :(
    t.checkExpect(m1.gameOver, true);
    
    // losing m2 board
    t.checkExpect(m2.gameOver, false);
    // clicking cell with mine
    m2.onMouseClicked(new Posn(30, 30), "LeftButton");
    // lose :(
    t.checkExpect(m2.gameOver, true);
  }
  
  void testOnTick(Tester t) {
    init();
    
    m1.seconds = 57;
    t.checkExpect(m1.seconds, 57);
    m1.onTick();
    t.checkExpect(m1.seconds, 58);
    m1.onTick();
    t.checkExpect(m1.seconds, 59);
    m1.onTick();
    t.checkExpect(m1.seconds, 0);
    t.checkExpect(m1.minutes, 1);
    
  }
  
  // tests for the method randomizeMines 
  // (does it randomly place the correct amount of mines?)
  void testRandomizeMines(Tester t) {
    init();
    // m1 has 2 mines
    t.checkExpect(c0.hasMine, true); // MINE
    t.checkExpect(c1.hasMine, false);
    t.checkExpect(c2.hasMine, false);
    t.checkExpect(c3.hasMine, false);
    t.checkExpect(c4.hasMine, false);
    t.checkExpect(c5.hasMine, false);
    t.checkExpect(c6.hasMine, false);
    t.checkExpect(c7.hasMine, true); // MINE
    t.checkExpect(c8.hasMine, false);
    
    // m2 has 1 mine 
    t.checkExpect(c9.hasMine, false);
    t.checkExpect(c10.hasMine, false);
    t.checkExpect(c11.hasMine, false);
    t.checkExpect(c12.hasMine, true); // MINE
    
    // yes!
  }
  
  // tests for the method makeGrid
  void testMakeGrid(Tester t) {
    init();
    // board is initialized with makeGrid
    // testing to see if board has grid
    t.checkExpect(m1.board, new ArrayList<ArrayList<Cell>>(Arrays.asList(r0, r1, r2)));
    t.checkExpect(m2.board, new ArrayList<ArrayList<Cell>>(Arrays.asList(r3, r4)));
  }

  // tests for the method LinkNeighbors
  void testLinkNeighbors(Tester t) {
    init();
    // top left
    t.checkExpect(c0.neighbors.containsAll(new ArrayList<Cell>(Arrays.asList(c3, c1, c4))), true);
    // top right
    t.checkExpect(c2.neighbors.containsAll(new ArrayList<Cell>(Arrays.asList(c1, c4, c5))), true);
    // bottom left
    t.checkExpect(c6.neighbors.containsAll(new ArrayList<Cell>(Arrays.asList(c3, c4, c7))), true);
    // bottom right
    t.checkExpect(c8.neighbors.containsAll(new ArrayList<Cell>(Arrays.asList(c7, c4, c5))), true);
    // middle
    t.checkExpect(c4.neighbors
        .containsAll(new ArrayList<Cell>(Arrays.asList(c0, c1, c2, c3, c5, c6, c7, c8))), true);
  }
  
  // tests for onMouseClicked and all its conditions
  void testOnMouseClicked(Tester t) {
    init();
    
    // placing a flag 
    t.checkExpect(c0.flag, false);
    m1.onMouseClicked(new Posn(0, 0), "RightButton");
    t.checkExpect(c0.flag, true);
    // ensure flagCount is changing
    t.checkExpect(m1.flagCount, 1);
    
    // CONDITION CHECK: can't place a flag on a cell that's already been revealed
    c6.reveal();
    t.checkExpect(c6.flag, false);
    m1.onMouseClicked(new Posn(0, 30), "RightButton");
    t.checkExpect(c6.flag, false);
    
    // removing a placed flag
    m1.onMouseClicked(new Posn(0, 0), "RightButton");
    t.checkExpect(c0.flag, false);
    
    // CONDITION CHECK: can't left click a cell that has a flag
    m1.onMouseClicked(new Posn(60, 60), "RightButton");
    t.checkExpect(c8.flag, true);
    t.checkExpect(c8.revealed, false);
    m1.onMouseClicked(new Posn(60, 60), "LeftButton");
    t.checkExpect(c8.flag, true);
    t.checkExpect(c8.revealed, false);
    
    // left clicking on a cell with no
    // neighboring mines (should flood)
    t.checkExpect(c1.revealed, false);
    t.checkExpect(c2.revealed, false);
    t.checkExpect(c4.revealed, false);
    t.checkExpect(c5.revealed, false);
    m1.onMouseClicked(new Posn(0, 60), "LeftButton");
    t.checkExpect(c1.revealed, true);
    t.checkExpect(c2.revealed, true);
    t.checkExpect(c4.revealed, true);
    t.checkExpect(c5.revealed, true);
    
    // left clicking on a mine should end the game
    // and reveal all mines
    t.checkExpect(m1.gameOver, false);
    m1.onMouseClicked(new Posn(0, 0), "LeftButton");
    t.checkExpect(c0.revealed, true);
    t.checkExpect(c7.revealed, true);
    t.checkExpect(m1.gameOver, true);
  }
}