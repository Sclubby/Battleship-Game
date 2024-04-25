import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

/*
import com.sun.security.ntlm.Client;
import javafx.application.Platform;
import javafx.scene.control.ListView;
 */

public class Server{

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	TheServer server;
	private final Consumer<Serializable> callback;

	Server(Consumer<Serializable> call){
		callback = call;
		server = new TheServer();
		server.start();
	}


	public class TheServer extends Thread{

		public void run() {

			try(ServerSocket mySocket = new ServerSocket(5555)){
		    callback.accept("Server is waiting for a client!");

			//Listen for clients and add them to client array
		    while(true) {
				ClientThread c = new ClientThread(mySocket.accept(), count,"<empty>");
				callback.accept("client has connected to server: " + "client #" + count);
				clients.add(c);
				c.start();

				count++;
			    }
			}
				catch(Exception e) {
					callback.accept("Server socket did not launch");
				}
			}
		}

		class ClientThread extends Thread{
			Socket connection;
			int count;
			String username;
			String opponentUsername;
			ClientThread opponent;
			boolean pendingMatch;
			boolean inGame;
			boolean hasCurrentMove;
			boolean shipsPlaced;
			Board playerBoard = new Board();
			Board opponentBoard;
			ObjectInputStream in;
			ObjectOutputStream out;

			ClientThread(Socket s, int count, String username){
				this.connection = s;
				this.count = count;
				this.username = username;
			}

			public void run(){
				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);
					this.out.writeObject(new ServerMessage(0));
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}

				 while(true) {
					    try {
					    	Request incomingRequest = (Request) in.readObject();
							if (incomingRequest.requestType == -4) {

								/*

								 Client denied another players request

								 */

								OpponentDeniedMessage(incomingRequest.username);

							}

							else if (incomingRequest.requestType == -1) {

								/*

								Request for players on server

								Used to display the player in Match Making Screen

								response: list of usernames by string

								 */

								sendListOfUsers();

							}
							else if (incomingRequest.requestType == 0) {
								/*
								Request to set username
								Server checks if user's requested username is taken.
								Response type: 1 - Valid username
                       					      -1 - Error: Username taken
								 */
								System.out.println("connection request");
								setUsername(incomingRequest);
							} else if (incomingRequest.requestType == 1) {
								/*
								Request for a new game with either AI or a player.
								If the request is valid, server responds with whether player has the first move.
								Response type: 2 - Valid request
                       					  	  -3 - Error: Requested opponent in game or not connected to server
								 */
								newGame(incomingRequest);
							}
							else if (incomingRequest.requestType == 2) {
								System.out.println("Ships placed");
								/*
								Request to start game (provides server with ship placements)
								If the player has the first move, they must make a subsequent update game request
								Response type: 3 - Valid request (server response tells client whether they have the first move)
											  -2 - Error: Opponent quit game or disconnected
											  -7 - Error: Improper ship placement

								 */
								finishedShipPlacement(incomingRequest.playerShipPositions);
							}
							else if (incomingRequest.requestType == 3) {
								/*
								Request to update game (provides server with player move)
								If the request is valid, the server will notify the opponent client of the players shot
								Response type: 4 - Valid request (includes result of shot)
											  -2 - Error: Opponent quit game or disconnected
                       					  	  -5 - Error: Repeated Shot in update game request (already missed)
                       					  	  -6 - Error: Repeated Shot in update game request (already hit)
								 */
								makeShot(incomingRequest);
								//If game is against AI, server immediately makes move and sends it to player.
								if(opponent == null && !hasCurrentMove) makeBotMove();
							}
							else if(incomingRequest.requestType == 4){
								this.pendingMatch = false;
								this.opponent = null;
								this.opponentUsername = null;
							}
						}
					    catch(Exception e) {  //runs when a user leaves the server
							System.out.println(e);
					    	callback.accept(username + " Left the server");
					    	clients.remove(this);
                            try {
								if (opponent != null) {
									this.opponent.out.writeObject(new ServerMessage(-2));
									opponent.inGame = false;
									opponent.opponent = null;
								}

								sendListOfUsers(); //update all list of users now that a player left the server
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            break;
					    }
					}
				}//end of run

			//server checks if ship placements are valid.  If one player has placed ships wait for the other.  If both players are ready start game

			private void finishedShipPlacement(int[][] playerShipPositions) throws IOException, InterruptedException {
				System.out.println("Finished ship placement");
				if(opponent == null && playerBoard.initBoard(playerShipPositions)){
					out.writeObject(new ServerMessage(3,null,hasCurrentMove));
					if(!hasCurrentMove) makeBotMove();
					return;
				}

				if(this.playerBoard.initBoard(playerShipPositions)) {
					opponent.opponentBoard = playerBoard; //sets players board as the opponents board
					shipsPlaced = true;
					if (opponent.shipsPlaced) { //both players have placed ships (initialize game for both players)
						out.writeObject(new ServerMessage(3,null,hasCurrentMove));
						opponent.out.writeObject(new ServerMessage(3,null,opponent.hasCurrentMove));
					} else { //wait for other player
						out.writeObject(new ServerMessage(53));
					}
				}

				else out.writeObject(new ServerMessage(-7)); //ship placement error

			}

			//Sends message back to notification sender that the opponent denied request
			private void OpponentDeniedMessage(String opponent) {

				clients.forEach(client -> {

					if (client.username.equals(opponent)) {
						try {
							client.opponent = null;
							client.pendingMatch = false;
							client.opponentUsername = null;
							client.out.writeObject(new ServerMessage(-9)); // opponent denied
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				});
			}

			private void sendListOfUsers() throws IOException {
				ArrayList<String> playersOnServer = new ArrayList<>();
				for (ClientThread sendTo : clients) { //sends updated data to all clients (not including that clients username)
					playersOnServer.clear();
					for (ClientThread client : clients) {
						if (!client.username.equals("<empty>") && !sendTo.username.equals(client.username) && !client.inGame) { //username is empty if player is not offically connected to server with a username
							playersOnServer.add(client.username);
						}
					}
					sendTo.out.writeObject(new ServerMessage(5, playersOnServer));
				}
			}

			private void gameOver() throws IOException {
				//opponentBoard = null;
				opponent = null;
				opponentUsername = null;
				pendingMatch = false;
				inGame = false;
			}

			/*
			If the request is valid, updates opponent board, responds to player client with result of shot, and notifies opponent client of shot coordinates and result.
			@param updateGameRequest
			 */
			private void makeShot(Request updateGameRequest) throws IOException {
				if(!hasCurrentMove){
					//Not player to move error
					this.out.writeObject(new ServerMessage(-4));
					return;
				}
				int shotResult = opponentBoard.updateBoard(updateGameRequest.shotXCord, updateGameRequest.shotYCord);
				ServerMessage response;

				if(shotResult < 0){
					response = new ServerMessage(shotResult);
				}
				else{
					if (shotResult == 0) { //switch turns if player missed
						hasCurrentMove = false;
						if(opponent != null) opponent.hasCurrentMove = true;
					}
					if(shotResult < 0) {
						return;
					}
					System.out.println(opponentBoard.shipsRemaining);
					response = new ServerMessage(4, shotResult > 0, shotResult == 2, opponentBoard.shipsRemaining); //send to player
					if(opponent != null && shotResult >= 0) { //send to opponent
						ServerMessage playerShot = new ServerMessage(50, shotResult == 2, shotResult > 0, updateGameRequest.shotXCord, updateGameRequest.shotYCord);
						opponent.out.writeObject(playerShot);
					}
				}
				this.out.writeObject(response);

				if(opponentBoard.shipsRemaining == 0) { // Game over; player won
					if(opponent != null) {
						opponent.out.writeObject(new ServerMessage(52));
						opponent.gameOver();
					}
					gameOver();
				}
			}

			/*
			Sends error code 4 if requested username is taken, otherwise responds with valid set username request and sets client username
			@param updateGameRequest
			 */
			private void setUsername(Request request) throws IOException { //checks a given username with all others so that there are no duplicate usernames
				if(request.username.isEmpty() || request.username.equals("AI")){
					this.out.writeObject(new ServerMessage(-1));
					return;
				}

				for (ClientThread client : clients) {
				//Search through all usernames and check if username is taken
					if (request.username.equals(client.username)) {
					//Send error code -1 if username is taken
						this.out.writeObject(new ServerMessage(-1));
						return;
					}
				}

				//Send code 1 (valid set username request) if username is not taken
				System.out.println("Username not found");
				this.out.writeObject(new ServerMessage(1));
				callback.accept("client: #" + count + "- username is: " + request.username);
				this.username = request.username;
			}

			/*
			Notify player and opponent that they have been matched and whether they have the first move. Only called by one client.
			@param player1 (player)
			@param player2 (opponent)
			 */
			private void matchPlayerAgainstPlayer(ClientThread opponent) throws IOException {

				clients.forEach( client -> { //search for any other notifications and deny them
					if (client.opponentUsername != null && client.opponentUsername.equals(this.username)) {
						OpponentDeniedMessage(client.username);
					}
				});

				this.opponent = opponent;
				opponent.opponent = this;

				boolean player1HasFirstMove = (int)(Math.random()*2) == 0; //coin flip for who goes first
				pendingMatch = false;
				inGame = true;
				hasCurrentMove = player1HasFirstMove;
				shipsPlaced = false;

				opponent.pendingMatch = false;
				opponent.inGame = true;
				opponent.hasCurrentMove = !player1HasFirstMove;
				opponent.shipsPlaced = false;

				//Inform both players that their new game request was valid and whether they have the first move
				ServerMessage responsePlayer = new ServerMessage(2, opponent.username,player1HasFirstMove);
				ServerMessage responseOpponent = new ServerMessage(2, this.username, !player1HasFirstMove);
				this.out.writeObject(responsePlayer);
				opponent.out.writeObject(responseOpponent);
				sendListOfUsers(); //remove these users from peoples lists since they are in a game
			}

			//send back an error that an opponent is in a pending match
			private void requestedOpponentInPendingMatch() throws IOException {
				ServerMessage error = new ServerMessage(-8);
				this.out.writeObject(error);
			}

			private void sendMatchRequestToOpponent(ClientThread requestedOpponent) throws IOException{
				this.pendingMatch = true;
				this.opponentUsername = requestedOpponent.username;
				this.opponent = requestedOpponent;
				ServerMessage notification = new ServerMessage(51, this.username);
				requestedOpponent.out.writeObject(notification);
			}

			private void makeBotMove() throws IOException, InterruptedException {
				System.out.println("Making bot move");
				int[] shot = new int[3];
                do {
                    playerBoard.makeBestShot(shot);
                    out.writeObject(new ServerMessage(50, shot[2] == 2, shot[2] >= 1, shot[0], shot[1]));
					if(playerBoard.shipsRemaining == 0){
						System.out.println("GAME OVER");
						out.writeObject(new ServerMessage(52, shot[2] == 2, shot[2] >= 1, shot[0], shot[1]));
						gameOver();
						break;
					}
					else if(opponentBoard.shipsRemaining == 0){
						gameOver();
						break;
					}
                } while (shot[2] >= 1);

				hasCurrentMove = true;
			}

			private void matchPlayerAgainstBot() throws IOException {
				System.out.println("Matching player against bot");
				opponentBoard = new Board();
				opponent = null;
				opponentBoard.initBoardWithRandShips();

				hasCurrentMove = (int)(Math.random()*2) == 0;
				pendingMatch = false;
				inGame = true;

				//Inform player that their new game request was valid and whether they have the first move
				out.writeObject(new ServerMessage(2, "AI", hasCurrentMove));
			}

			private void newGame(Request request) throws IOException{
				System.out.println(request.username);
				//Game against AI
				if(request.username.isEmpty()){
					System.out.println("Game against AI requested");
					boolean playerHasFirstMove = (int)(Math.random()*2) == 0;
					matchPlayerAgainstBot();
					this.out.writeObject(new ServerMessage(2, "AI", playerHasFirstMove));
					return;
				}

				//Game against player
				for (ClientThread client : clients) {
					if (client.username.equals(request.username)) {
						if (client.opponentUsername != null && client.opponentUsername.equals(this.username)) {  //if player accepted a notification match both of them
							matchPlayerAgainstPlayer(client);
						}
						else { //send notification to opponent and wait
							if (client.pendingMatch) { requestedOpponentInPendingMatch(); break; }
							sendMatchRequestToOpponent(client);
							this.opponentUsername = request.username;
						}
						break;
					}
				}
			}

		}//end of client thread
}


	
	

	
