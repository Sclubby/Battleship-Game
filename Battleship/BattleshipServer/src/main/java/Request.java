import java.io.Serializable;
import java.util.ArrayList;

public class Request implements Serializable {
    static final long serialVersionUID = 42L;

    /*
    0 - Request set username
    1 - Request new game
    2 - Request start game
    3 - Request update to game
    4 - Request cancellation of a previous new game request
    */
    int requestType;

    int shotXCord;
    int shotYCord;

    /*
    True - Hit
    False - Miss
    */
    boolean shotResult;

    /*
    NULL for game with AI
    Can contain either a player or opponent username.
    */
    String username;

    /*
    5 by 4 array of cords.
    Each row should hold the first and last cord of a ships position
    (e.g x1 y1 x2 y2 where x1 <= x2 and y1 <= y2).
     */
    int[][] playerShipPositions = new int[5][4];

    /*
    Request connection to server or a new game
    @param requestType 0 for connection to server request and 1 for new game request
    @param username The player username if requestType is 0 and the opponent username if requestType is 1
     */
    Request(int requestType, String username){
        this.requestType = requestType;
        this.username = username;
    }

    /*
    Request to start game
    @param requestType must be 2
    @param 5 by 4 array of ship coordinates (rows in format x1 y1 x2 y2 where x1 <= x2 and y1 <= y2)
     */
    Request(int requestType, int[][] playerShipPositions){
        this.requestType = requestType;
        for(int i = 0; i < 5; i++){
            System.arraycopy(playerShipPositions[i], 0, this.playerShipPositions[i], 0, 4);
        }
    }

    /*
    Request update to game
    @param requestType Must be 3
    @param shotXCord
    @param shotYCord
     */
    Request(int requestType, int shotXCord, int shotYCord) {
        this.requestType = requestType;
        this.shotXCord = shotXCord;
        this.shotYCord = shotYCord;
    }

    /*
    Request cancellation of a previous new game request
    @param requestType Must be 4
     */
    Request(int requestType){
        this.requestType = requestType;
    }
}
