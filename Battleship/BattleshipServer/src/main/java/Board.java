import java.lang.reflect.Array;
import java.util.ArrayList;

public class Board {
    /*
    0 - Empty Cell
    1 - Ship Cell
    2 - Hit
    3 - Miss
     */
    final private int[][] board = new int[10][10];
    int shipsRemaining = 5;
    int shipCellsShot = 0;
    boolean shipFound = false;
    int lastHitX;
    int lastHitY;
    /*
    5 by 5 array of ship coordinates and number of remaining cells.
    Each row should hold the first and last coordinate of a ships position, the ships size, and the number of remaining cells that ship has.
    (e.g. ship start x, ship start y, ship end x, ship end y, ship size, # ship cells remaining)
     */
    final private int[][] ships = new int[5][6];

    public boolean initBoard(int[][] shipPositions){
        shipsRemaining = 5;

        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                board[row][col] = 0;
            }
        }

        for(int ship = 0; ship < 5; ship++) {
            int shipXStart = ships[ship][0] = shipPositions[ship][0];
            int shipYStart = ships[ship][1] = shipPositions[ship][3];
            int shipXEnd = ships[ship][2] = shipPositions[ship][2];
            int shipYEnd = ships[ship][3] = shipPositions[ship][1];

            //Ship is horizontal
            if (shipYStart == shipYEnd) {
                ships[ship][4] = ships[ship][5] = shipXEnd - shipXStart + 1;
                for (int col = shipXStart; col < shipXEnd + 1; col++) {
                    if(board[shipYStart][col] == 1) return false;
                    board[shipYStart][col] = 1;
                }
            //Ship is vertical
            } else {
                ships[ship][4] = ships[ship][5] = shipYEnd - shipYStart + 1;
                for (int row = shipYStart; row < shipYEnd + 1; row++) {
                    if (board[row][shipXStart] == 1) return false;
                    board[row][shipXStart] = 1;
                }
            }

        }
        return true;
    }

    public int updateBoard(int shotXCord, int shotYCord){
        if(board[shotYCord][shotXCord] == 0){ //ship missed
            board[shotYCord][shotXCord] = 3;
            return 0;
        }
        else if(board[shotYCord][shotXCord] == 2) return -1; //Cell previously hit
        else if(board[shotYCord][shotXCord] == 3) return -3; //Cell previously missed

        board[shotYCord][shotXCord] = 2; //ship hit
        for(int ship = 0; ship < 5; ship++){
            int shipXStart = ships[ship][0];
            int shipYStart = ships[ship][1];
            int shipXEnd = ships[ship][2];
            int shipYEnd = ships[ship][3];
            int shipSize = ships[ship][4];

            boolean horizontalShipShot = (shipYStart == shipYEnd && shotYCord == shipYStart && Math.abs(shotXCord - shipXStart) < shipSize);
            boolean verticalShipShot = (shipXStart == shipXEnd && shotXCord == shipXStart && Math.abs(shotYCord - shipYStart) < shipSize);

            if (horizontalShipShot || verticalShipShot) {
                shipCellsShot += 1;
                System.out.println("Ship#: " + ship + " (" + shipXStart + "," + shipYStart + ") Size: "+ shipSize);
                System.out.println("Ship cells left before hit: " + ships[ship][5]);
                ships[ship][5] -= 1; //decrement # of remaining cells
                System.out.println("Ship cells left after hit: " + ships[ship][5]);
                if(ships[ship][5] <= 0) { //Ship sunk
                    System.out.println("SHIP SUNK");
                    shipsRemaining -= 1;
                    return 2;
                }
                return 1;
            }
        }
        System.out.println("ERROR!");
        return -1; //should never return here
    }
    /*
    Checks that a ship can be placed at a coordinate such that the placement is valid
     */
    private boolean isValidPlacement(int firstCordX, int firstCordY, boolean dir, int shipSize){
        if(dir) { //horizontal
            if(firstCordX + shipSize - 1 >= 9) return false;
            for(int col = firstCordX; col < firstCordX + shipSize; col++){
                if(board[firstCordY][col] == 1) return false; //ship already at cell
            }
        }
        else{ //vertical
            if(firstCordY + shipSize - 1 >= 9) return false;
            for(int row = firstCordY; row < firstCordY + shipSize; row++){
                if(board[row][firstCordX] == 1) return false; //ship already at cell
            }
        }

        return true;
    }


    // ------- AI ------- (Only accesses known data (e.g. whether a cell was hit, missed, or not yet shot)
    public void initBoardWithRandShips(){
        shipCellsShot = 0;
        shipsRemaining = 5;

        System.out.println("Initializing AI Ships");
        System.out.println("Placing ship# " + 0);
        placeRandomShip(3, 0);
        for(int shipSize = 2; shipSize < 6; shipSize++){
            System.out.println("Placing ship#:" + (shipSize - 1));
            placeRandomShip(shipSize, shipSize - 1);
        }

        for(int i = 0; i < 10; i++){
            for(int j = 0; j < 10; j++){
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }

        System.out.println("AI Ships initialized");
    }
    public void makeBestShot(int[] shot){
        if(!shipFound && (unknownShipFound() && Math.random() > 0.1)){
            shipNotFoundShot(shot);
        } else if(shipFound){
            shipFoundShot(shot, lastHitX, lastHitY);
        } else{
            unknownShipFoundShot(shot);
        }
    }

    private void shipNotFoundShot(int[] shot){
        ArrayList<int[]> bestHits = getBestHitsDiscovery();
        int hit = (int)(Math.random() * bestHits.size());

        int shotResult = updateBoard(bestHits.get(hit)[0], bestHits.get(hit)[1]);
        shot[0] = bestHits.get(hit)[0];
        shot[1] = bestHits.get(hit)[1];
        shot[2] = shotResult;

        if(shotResult == -1){
            shot[2] = 1;
            return;
        }
        else if(shotResult == -2){
            shot[2] = 0;
            return;
        }

        if(shotResult == 1){
            shipFound = true;
            lastHitX = shot[0];
            lastHitY = shot[1];
        }
        else if(shotResult == 0){
            shipFound = false;
        }
    }
    private void unknownShipFoundShot(int[] shot) {
        int[][] sinkScores = new int[10][10];
        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                sinkScores[row][col] = 0;
            }
        }

        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                unknownGetSinkScores(sinkScores, col, row);
            }
        }

        int highestSinkScore = sinkScores[0][0];
        int highestSinkScoreX = 0;
        int highestSinkScoreY = 0;

        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                if(sinkScores[row][col] > highestSinkScore){
                    highestSinkScoreX = col;
                    highestSinkScoreY = row;
                    highestSinkScore = sinkScores[row][col];
                }
            }
        }

        if(highestSinkScore == 0 || board[highestSinkScoreY][highestSinkScoreX] > 1){
            shipNotFoundShot(shot);
            return;
        }

        int shotResult = updateBoard(highestSinkScoreX, highestSinkScoreY);
        shot[0] = highestSinkScoreX;
        shot[1] = highestSinkScoreY;
        shot[2] = shotResult;

        if(shotResult == 1){
            shipFound = true;
            lastHitX = shot[0] ;
            lastHitY = shot[1];
        }
        if(shotResult == 0) {
            shipFound = false;
        }
    }
    private void shipFoundShot(int[] shot, int hitX, int hitY) {
        ArrayList<int[]> ships = getPossibleShipsToSink(hitX, hitY);

        int[][] shipCellOverLap = new int[10][10];
        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++) {
                shipCellOverLap[row][col] = 0;
            }
        }

        for(int ship = 0; ship < ships.size(); ship++) {
            int shipXStart = ships.get(ship)[0];
            int shipYStart = ships.get(ship)[1];
            int shipXEnd = ships.get(ship)[2];
            int shipYEnd = ships.get(ship)[3];

            //Ship is horizontal
            if (shipYStart == shipYEnd) {
                for (int col = shipXStart; col < shipXEnd + 1; col++) {
                    if(board[shipYStart][col] < 2 && isAdjToHit(col, shipYStart)) shipCellOverLap[shipYStart][col] += 1;
                }
                //Ship is vertical
            } else {
                for (int row = shipYStart; row < shipYEnd + 1; row++) {
                    if(board[row][shipXStart] < 2 && isAdjToHit(shipXStart, row)) shipCellOverLap[row][shipXStart] += 1;
                }
            }
        }

        int mostOverlap = shipCellOverLap[0][0];
        int mostOverlapX = 0;
        int mostOverlapY = 0;

        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                if(shipCellOverLap[row][col] > mostOverlap){
                    mostOverlapX = col;
                    mostOverlapY = row;
                    mostOverlap = shipCellOverLap[row][col];
                }
            }
        }

        if(mostOverlap == 0 || board[mostOverlapY][mostOverlapX] > 1){
            shipNotFoundShot(shot);
            return;
        }

        int shotResult = updateBoard(mostOverlapX, mostOverlapY);

        shot[0] = mostOverlapX;
        shot[1] = mostOverlapY;
        shot[2] = shotResult;

        if(shotResult == 2) {
            shipFound = false;
            System.out.println("Ship sunk");
        }
    }


    private boolean isNotAdjPlacement(int firstCordX, int firstCordY, boolean dir, int shipSize){
        if(dir) { //horizontal
            for(int cell = 0; cell < shipSize; cell++){
                for(int col = firstCordX; col < firstCordX + shipSize; col++){
                    if(firstCordY > 0 && board[firstCordY - 1][col] == 1) return false; //adjacent ship above
                    if(firstCordY < 9 && board[firstCordY + 1][col] == 1) return false; //adjacent ship below
                }
            }

            if(firstCordX > 0 && board[firstCordY][firstCordX - 1] == 1) return false; //adjacent ship to the left
            if(firstCordX + shipSize < 10 && board[firstCordY][firstCordX + shipSize] == 1) return false; //adjacent ship to the right
        }
        else{ //vertical
            for(int row = firstCordY; row < firstCordY + shipSize; row++){
                for(int cell = 0; cell < shipSize; cell++){
                    if(firstCordX > 0 && board[row][firstCordX - 1] == 1) return false; //adjacent ship to the left
                    if(firstCordX < 9 && board[row][firstCordX + 1] == 1) return false; //adjacent ship to the right
                }
            }

            if(firstCordY > 0 && board[firstCordY - 1][firstCordX] == 1) return false; //adjacent ship above
            if(firstCordY + shipSize < 10 && board[firstCordY + shipSize][firstCordX] == 1) return false; //adjacent ship below
        }

        return true;
    }
    private void placeShip(int firstCordX, int firstCordY, boolean dir, int shipSize, int shipNum){
        ships[shipNum][0] = firstCordX;
        ships[shipNum][1] = firstCordY;
        ships[shipNum][4] = ships[shipNum][5] = shipSize;
        System.out.println("NUM CELLS: " + shipSize);

        if(dir) { //horizontal
            ships[shipNum][2] = firstCordX + shipSize - 1;
            ships[shipNum][3] = firstCordY;

            for(int cell = 0; cell < shipSize; cell++){
                for(int col = firstCordX; col < firstCordX + shipSize; col++){
                    board[firstCordY][col] = 1;
                }
            }
        }
        else{ //vertical
            ships[shipNum][2] = firstCordX;
            ships[shipNum][3] = firstCordY + shipSize - 1;

            for(int row = firstCordY; row < firstCordY + shipSize; row++){
                for(int cell = 0; cell < shipSize; cell++){
                    board[row][firstCordX] = 1;
                }
            }
        }
    }
    private void placeRandomShip(int shipSize, int shipNum){
        boolean dir = (int)(Math.random()*2) == 0; //true = horizontal, false = vertical

        while(true){
            int firstCordX;
            int firstCordY;
            if(dir){
                firstCordX = (int)(Math.random()*(10 - shipSize));
                firstCordY = (int)(Math.random()*10);
            }
            else{
                firstCordX = (int)(Math.random()*10);
                firstCordY = (int)(Math.random()*(10 - shipSize));
            }

            if(isValidPlacement(firstCordX, firstCordY, dir, shipSize) && (isNotAdjPlacement(firstCordX, firstCordY, dir, shipSize) || Math.random() > 0.8)) {
                placeShip(firstCordX, firstCordY, dir, shipSize, shipNum);
                break;
            }
        }
    }
    private int getWorstDiscoveryIndex(ArrayList<int[]> bestHits) {
        int worstIndex = 0;
        int minDiscoveries = bestHits.get(0)[2];

        for (int i = 1; i < bestHits.size(); i++) {
            if (bestHits.get(i)[2] < minDiscoveries) {
                minDiscoveries = bestHits.get(i)[2];
                worstIndex = i;
            }
        }

        return worstIndex;
    }
    private boolean recurseAdd(int currentIndex, int[] numbers, ArrayList<Integer> usedNumbers, int sum, int target) {
        if (currentIndex >= numbers.length) {
            return false;
        }
        sum = sum + numbers[currentIndex];
        usedNumbers.add(numbers[currentIndex]);
        if (sum == target && usedNumbers.size() == 5 - shipsRemaining) {
            return true;
        }

        if (sum > target || usedNumbers.size() > 5 - shipsRemaining) {
            return false;
        }

        ArrayList<Integer> usedNumbersCopy = new ArrayList<>(usedNumbers);
        usedNumbersCopy.remove(usedNumbersCopy.size() - 1);
        for (int i = currentIndex + 1; i < numbers.length; i++) {
            if(recurseAdd(i, numbers, new ArrayList<>(usedNumbers), sum, target)) return true;
        }
        return false;
    }
    private boolean unknownShipFound(){
        int[] possibleShipSizes = new int[5];
        possibleShipSizes[0] = 2;
        possibleShipSizes[1] = 3;
        possibleShipSizes[2] = 3;
        possibleShipSizes[3] = 4;
        possibleShipSizes[4] = 5;

        for(int i = 0; i < 5; i++) {
            if(recurseAdd(i, possibleShipSizes, new ArrayList<Integer>(), 0, shipCellsShot)) return true;
        }
        return false;
    }
    public ArrayList<int[]> getBestHitsDiscovery() {
        int[][] discoveryDensityMap = new int[10][10];
        loadDiscoveryDensityMap(discoveryDensityMap);

        ArrayList<int[]> bestHits = new ArrayList<>();
        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                if (board[row][col] < 2) {
                    if(bestHits.size() < 2){
                        int[] discovery = new int[3];
                        discovery[0] = col;
                        discovery[1] = row;
                        discovery[2] = discoveryDensityMap[row][col];
                        bestHits.add(discovery);
                    }
                    else {
                        int worstDiscoveryIndex = getWorstDiscoveryIndex(bestHits);
                        if(bestHits.get(worstDiscoveryIndex)[2] < discoveryDensityMap[row][col]){
                            bestHits.get(worstDiscoveryIndex)[0] = col;
                            bestHits.get(worstDiscoveryIndex)[1] = row;
                            bestHits.get(worstDiscoveryIndex)[2] = discoveryDensityMap[row][col];
                        }
                    }
                }
            }
        }
        return bestHits;
    }

    public boolean isAdjToHit(int hitX, int hitY){
        if(hitX - 1 >= 0 && board[hitY][hitX-1] == 2 ){
            return true;
        }
        if(hitX + 1 <= 9 && board[hitY][hitX+1] == 2){
            return true;
        }
        if(hitY - 1 >= 0 && board[hitY - 1][hitX] == 2){
            return true;
        }
        if(hitY + 1 <= 9 && board[hitY + 1][hitX] == 2){
            return true;
        }
        return false;
    }

    private ArrayList<int[]> getPossibleShipsToSink(int hitX, int hitY){
        ArrayList<int[]> ships = new ArrayList<>();

        for(int shipSize = 2; shipSize < 6; shipSize++){
            int lowerYBound = hitY - (shipSize - 1);
            int lowerXBound = hitX - (shipSize - 1);

            if(lowerYBound < 0) lowerYBound = 0;
            if(lowerXBound < 0) lowerXBound = 0;
            for(int x = lowerXBound; x <= hitX && x + shipSize - 1 < 10; x++) {
                int numHits = canSink(x, hitY, true, shipSize);
                int[] ship = new int[6];
                ship[0] = x;
                ship[1] = hitY;
                ship[2] = x + shipSize - 1;
                ship[3] = hitY;
                ship[4] = shipSize;
                ship[5] = numHits;

                if(ships.isEmpty() || ships.get(0)[5] == numHits) {
                    ships.add(ship);
                }
                else if(ships.get(0)[5] < numHits) {
                    ships.clear();
                    ships.add(ship);
                }
            }
            for(int y = lowerYBound; y <= hitY && y + shipSize - 1 < 10; y++) {
                int numHits = canSink(hitX, y, false, shipSize);
                int[] ship = new int[6];

                ship[0] = hitX;
                ship[1] = y;
                ship[2] = hitX;
                ship[3] = y + shipSize - 1;
                ship[4] = shipSize;
                ship[5] = numHits;

                if(ships.isEmpty() || ships.get(0)[5] == numHits)  ships.add(ship);
                else if(ships.get(0)[5] < numHits) {
                    ships.clear();
                    ships.add(ship);
                }
            }
        }
        return ships;
    }

    private void unknownGetSinkScores(int[][] sinkScores, int hitX, int hitY){
        ArrayList<int[]> ships = getPossibleShipsToSink(hitX, hitY);

        int[][] shipCellOverLap = new int[10][10];
        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++) {
                shipCellOverLap[row][col] = 0;
            }
        }

        for(int ship = 0; ship < ships.size(); ship++) {
            int shipXStart = ships.get(ship)[0];
            int shipYStart = ships.get(ship)[1];
            int shipXEnd = ships.get(ship)[2];
            int shipYEnd = ships.get(ship)[3];

            //Ship is horizontal
            if (shipYStart == shipYEnd) {
                for (int col = shipXStart; col < shipXEnd + 1; col++) {
                    if(board[shipYStart][col] < 2 && isAdjToHit(col, shipYStart)) shipCellOverLap[shipYStart][col] += 1;
                }
                //Ship is vertical
            } else {
                for (int row = shipYStart; row < shipYEnd + 1; row++) {
                    if(board[row][shipXStart] < 2 && isAdjToHit(shipXStart, row)) shipCellOverLap[row][shipXStart] += 1;
                }
            }
        }

        int mostOverlap = shipCellOverLap[0][0];
        int mostOverlapX = 0;
        int mostOverlapY = 0;

        for(int row = 0; row < 10; row++){
            for(int col = 0; col < 10; col++){
                if(shipCellOverLap[row][col] > mostOverlap){
                    mostOverlapX = col;
                    mostOverlapY = row;
                    mostOverlap = shipCellOverLap[row][col];
                }
            }
        }

        sinkScores[mostOverlapY][mostOverlapX] += mostOverlap;
    }

    private int canSink(int firstCordX, int firstCordY, boolean dir, int shipSize){
        boolean hasFreeCell = false;
        int numHits = 0;

        if(dir) { //horizontal
            if(firstCordX + shipSize - 1 >= 10) return -1;
            for(int col = firstCordX; col < firstCordX + shipSize; col++){
                if(board[firstCordY][col] == 3) return -1; //already missed
                if(board[firstCordY][col] == 2) numHits++;
                if(board[firstCordY][col] < 2) hasFreeCell = true; //not all ships cells are hit
            }
        }
        else{ //vertical
            if(firstCordY + shipSize - 1 >= 10) return -1;
            for(int row = firstCordY; row < firstCordY + shipSize; row++){
                if(board[row][firstCordX] == 3) return -1; //already missed
                else if(board[row][firstCordX] == 2) numHits++;
                else if(board[row][firstCordX] < 2) hasFreeCell = true; //not all ships cells are hit
            }
        }

        if(hasFreeCell) return numHits;
        return -1;
    }

    private boolean canDiscover(int firstCordX, int firstCordY, boolean dir, int shipSize){
        if(dir) { //horizontal
            if(firstCordX + shipSize - 1>= 10) return false;
                for(int col = firstCordX; col < firstCordX + shipSize; col++){
                    if(board[firstCordY][col] > 1) return false; //already hit or missed
                }
        }
        else{ //vertical
            if(firstCordY + shipSize - 1>= 10) return false;
            for(int row = firstCordY; row < firstCordY + shipSize; row++){
                if(board[row][firstCordX] > 1) return false; //already hit or missed
            }
        }

        return true;
    }

    private void loadDiscoveryDensityMap (int[][] discoveryDensityMap){
        for(int row = 0; row < 10; row++){
            int lowerYBound = row - 4;
            int upperYBound = row + 4;
            if(lowerYBound < 0) lowerYBound = 0;
            if(upperYBound > 9) upperYBound = 9;

            for(int col = 0; col < 10; col++){
                int lowerXBound = col - 4;
                int upperXBound = col + 4;
                if(lowerXBound < 0) lowerXBound = 0;
                if(upperXBound > 9) upperXBound = 9;

                discoveryDensityMap[row][col] = 0;
                for(int shipSize = 2; shipSize < 6; shipSize++){
                    for(int x = lowerXBound; x <= upperXBound; x++) {
                        if (canDiscover(x, row, true, shipSize)) discoveryDensityMap[row][col]++;
                    }
                    for(int y = lowerYBound; y <= upperYBound; y++) {
                        if (canDiscover(col, y, false, shipSize)) discoveryDensityMap[row][col]++;
                    }
                }
            }
        }
    }
}
