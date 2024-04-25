import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;

public class Board extends Parent {
    private VBox rows = new VBox(); //holds all the cells

    private boolean enemy;  //if the board is the enemy board (top board)
    public BoardCell ShotAtCell; //cell that is selected on enemy board


    public boolean lockboard = false; //if true the board cannot be pressed
    boolean vertical = false; //if false the ships will be placed horizontally
    public int[] ships = {5,4,3,3,2,0}; //each ship that is placed (length of ships)

    /*  5 by 4 array of cords.
     Each row should hold the first and last cord of a ships position
     (e.g x1 y1 x2 y2 where x1 <= x2 and y1 <= y2).   */
    public int[][] shipPositions = new int[5][4];
    public int currentShip = 0; //index used for ships list

    String EnemyBoardColor = "#8df1b1"; //green color for board
    String PlayerBoardColor = "#8d9bf1"; //blue color for board
    public Board(boolean enemy) {
        this.enemy = enemy;
        if(enemy){  lockboard = true; }

        for (int y = 0; y < 10; y++) {
            HBox row = new HBox();
            for (int x = 0; x < 10; x++) {
                BoardCell c = new BoardCell(x, y, this);
                row.getChildren().add(c);
            }
            rows.getChildren().add(row);
        }
        getChildren().add(rows);

        // Ensure all cells are fully initialized before setting up hover effects
       if (!enemy) { Platform.runLater(this::setupPlayerHoverEffects); }
       else { Platform.runLater(this::setUpEnemyHoverEffects);}
    }

    //reset board for new game
    public void clearBoard(boolean enemyBoard) {
          vertical = false; //if false the ships will be placed horizontally
         currentShip = 0; //index used for ships list
        lockboard = enemyBoard;  //locks enemy board and enables player board
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                BoardCell cell = getCell(x,y);
                cell.shotAt = false;
                cell.ship = false;
               cell.cellsHorizontal = cell.getNextCells(cell,false); //these are prebaked at runtime
                cell.cellsVeritcal = cell.getNextCells(cell,true); // these are prebaked at runtime
                getCell(x,y).setFill(enemyBoard ? Color.web(EnemyBoardColor) : Color.web(PlayerBoardColor));
            }
            }
        }

    //goes through every cell and adds hovereffect for enemy board (since hovereffects use info from other cells this needs to be done after the board is created)
    private void setUpEnemyHoverEffects() {
        for (Node row : rows.getChildren()) {
            for (Node cell : ((HBox) row).getChildren()) {
                ((BoardCell) cell).setUpEnemyHoverEffects();
            }
        }
    }

    //goes through every cell and adds hovereffect for player board (since hovereffects use info from other cells this needs to be done after the board is created)
    private void setupPlayerHoverEffects() {
        for (Node row : rows.getChildren()) {
            for (Node cell : ((HBox) row).getChildren()) {
                ((BoardCell) cell).setupPlayerHoverEffect();
            }
        }
    }

    //return a cell from rows or null if not found
    public BoardCell getCell(int x, int y) {
        if (x >= 0 && x < 10 && y >= 0 && y < 10) {
            return (BoardCell) ((HBox) rows.getChildren().get(y)).getChildren().get(x);
        }
        return null;  // Return null if out of bounds
    }

    //used for player board when updating panels
    public void setCell(int shotXCord, int shotYCord, boolean hit) {
        BoardCell cell = getCell(shotXCord,shotYCord);
        cell.setFill(hit ? Color.RED : Color.WHITE);
    }


    public class BoardCell extends Rectangle {
        public int x, y; //cords of cell
        boolean ship = false; //ship on cell
        boolean shotAt = false; //space has been shot at
        ArrayList<BoardCell> cellsHorizontal; //these are prebaked at runtime (cells horizontally next)
        ArrayList<BoardCell> cellsVeritcal;  //cells vetically next

        public BoardCell(int x, int y, Board board) { //cell constructor
            super(20, 20);
            this.x = x; this.y = y;
            setFill(board.enemy ? Color.web(EnemyBoardColor) : Color.web(PlayerBoardColor));
            setStroke(Color.BLACK);
        }

        public void setupPlayerHoverEffect() { //logic for all mouse actions on a cell
             cellsHorizontal = getNextCells(this,false); //these are prebaked at runtime
             cellsVeritcal = getNextCells(this,true); // these are prebaked at runtime
            this.setOnMouseEntered(e -> {
                if (!lockboard) {
                    ArrayList<BoardCell> cells = cellsHorizontal;
                    if (vertical) {
                        cells = cellsVeritcal;
                    }
                    if (CanPlaceShip(cells)) {
                        cells.forEach(cell -> cell.setFill(Color.LIGHTGRAY));
                    }
                }
            });
            this.setOnMouseClicked(e ->{
                if (!lockboard) {
                    ArrayList<BoardCell> cells = cellsHorizontal;
                    if (vertical) {
                        cells = cellsVeritcal;
                    }

                    if (CanPlaceShip(cells)) {
                        for (BoardCell cell : cells) {
                            cell.setFill(Color.web("#939393"));
                            cell.ship = true;
                        }
                        shipPositions[currentShip][0] = cells.get(0).x;
                        shipPositions[currentShip][1] = cells.get(0).y;
                        shipPositions[currentShip][2] = cells.get(cells.size() - 1).x;
                        shipPositions[currentShip][3] = cells.get(cells.size() - 1).y;

                        currentShip++;
                        if (currentShip == 7) {
                            currentShip = 6;
                        }
                    }
                }
            });
            this.setOnMouseExited(e -> {
                if (!lockboard) {
                    ArrayList<BoardCell> cells = cellsHorizontal;
                    if (vertical) {
                        cells = cellsVeritcal;
                    }

                    if (CanPlaceShip(cells)) {
                        for (BoardCell cell : cells) {
                            if (!cell.ship) {
                                cell.setFill(Color.web(PlayerBoardColor));
                            }
                        }
                    }
                }
            });
        }

        public void setUpEnemyHoverEffects() {
            this.setOnMouseEntered(e -> {
                if (!lockboard && !shotAt) {
                    setFill(Color.DARKGRAY);
                }
            });
            this.setOnMouseClicked(e ->{
                if (!lockboard) {
                    shotAt = true;
                    ShotAtCell = this;
                }
            });
            this.setOnMouseExited(e -> {
                if (!lockboard && !shotAt) {
                    setFill(Color.web(EnemyBoardColor));
                }
            });
        }

        //checks to see if a ship can be placed in a given spot
        private boolean CanPlaceShip(ArrayList<BoardCell> cells) {

            if (cells.size() < ships[currentShip]) { return false; }  //if ship cant fit or enemy board return

            while (ships[currentShip] != cells.size()) { //remove later spaces so cells is the size of the current ship we are inserting
                cells.remove(cells.size()-1);
            }
            for (BoardCell x : cells) { //if a ship is found on any cell return
                if (x.ship) { return false; }
            }
            return true;
        }

        //returns a list of cells either vertically or horizontally
        private ArrayList<BoardCell> getNextCells(BoardCell startingCell, boolean vert) {
            ArrayList<BoardCell> cells = new ArrayList<>();
            int cursorX = 0; int cursorY = 0;

            for (int i = 0; i < 5; i++) {
                BoardCell nextCell = getCell(startingCell.x + cursorX, startingCell.y + cursorY);
                if (vert) {  cursorY--; } else { cursorX++; }
                if (nextCell != null) {
                    cells.add(nextCell);
                } else {
                    break;
                }
            }
            return cells;
        }
    }
}
