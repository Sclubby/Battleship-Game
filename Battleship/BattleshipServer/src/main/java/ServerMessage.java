import java.io.Serializable;
import java.util.ArrayList;

public class ServerMessage implements Serializable {
    static final long serialVersionUID = 42L;

    int shotXCord;
    int shotYCord;
    /*
    True - Hit
    False - Miss
    */
    boolean shotResult;
    boolean shipSunk;
    boolean playerHasFirstMove;
    String username;
    ArrayList<String> playersOnServer;
    int numShipsSunk;
    /*
    RESPONSES
    0 - Response to valid server connection request
    1 - Response to valid set username request
    2 - Response to valid new game request
    3 - Response to start game request
    4 - Response to update game request

    ERRORS
    -1 - Invalid set username request (Username taken)
    -2 - Opponent quit game or disconnected
    -4 - Request to update game when it is not the players move
    -7 - Invalid start game request (improper ship placement)
    -8 - Requested opponent while game is pending
    -9 - Opponent denied request

    MESSAGES
    50 - Notify player of opponent move
    51 - Notify player of opponent game request
    52 - Notify player that they lost
    53 - Notify player that their ship placement was valid; waiting for opponent ship placement before fulfilling start game request
    */
    int messageType;

    /*
    Response that only has code
    @param messageType 0 - Connection to server successful
                       1 - Valid set username request
                       2 - Valid start game request
                       5 - Player lost Game
                      10 - Player placed all ships and is waiting for opponent
                      -1 - Username taken
                      -2 - Opponent quit game or disconnected
                      -8 - Opponent is pending in another match
                      -8 - Opponent notification sent to an opponent in a pending match
                      -9 - Opponent Denied request
     */
    ServerMessage(int messageType){
        this.messageType = messageType;
    }

     /*
    Response to new game request
    @param messageType must be 2
    @param username is the opponent for the user receiving the response
    @param playerHasFirstMove True if player receiving response has the first move, false otherwise.
     */
    ServerMessage(int messageType, String username, boolean playerHasFirstMove){
        this.messageType = messageType;
        this.username = username;
        this.playerHasFirstMove = playerHasFirstMove;
    }

    /*
    Response to game update request
    @param messageType must be 4
    @param shotResult True if a shot hit and false if a shot missed
    @param shipSunk True if a player previous request resulted in a ship being sunk
     */
    ServerMessage(int messageType, boolean shotResult, boolean shipSunk, int numShipsRemaining) {
        this.messageType = messageType;
        this.shipSunk = shipSunk;
        this.shotResult = shotResult;
        this.numShipsSunk = 5 - numShipsRemaining;
    }

    /*
    Notify player of opponent shot
    @param messageType must be 50
    @param shotResult True if opponent's shot hit and false if opponent shot missed
    @param shipSunk True if opponent's shot resulted in a ship being sunk
    @param shotXCord Opponent shot x coordinate
    @param shotYCord Opponent shot y coordinate
     */
    ServerMessage(int messageType, boolean shipSunk, boolean shotResult, int shotXCord, int shotYCord) {
        this.messageType = messageType;
        this.shipSunk = shipSunk;
        this.shotResult = shotResult;
        this.shotXCord = shotXCord;
        this.shotYCord = shotYCord;
    }

    /*
    Notify player of match request
    @param messageType must be 50
     */
    ServerMessage(int messageType, String username){
        this.messageType = messageType;
        this.username = username;
    }

    /*
    Send players on server to client for up-to-date list
    @param messageType must be 6
     */
    ServerMessage(int messageType, ArrayList<String> playersOnServer) {
        this.messageType = messageType;
        this.playersOnServer = playersOnServer;
    }


}
