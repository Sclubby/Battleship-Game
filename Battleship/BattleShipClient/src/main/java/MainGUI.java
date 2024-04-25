import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.WindowEvent;
import javafx.scene.text.Text;
import javafx.util.Callback;
import java.util.ArrayList;
import java.util.HashMap;

public class MainGUI extends Application {
	HashMap<String,Scene> sceneMap = new HashMap<>();  //map of all scenes
   Client clientConnection; //sends data to server
   Stage primaryStage = new Stage(); //display window
	String username; //players username
	String opponent; //opponents username

	@Override
	public void start(Stage x) {

		clientConnection = new Client(data->{ //runs everytime a data is sent from the server
			Platform.runLater(()->{
				ServerMessage incomingData = (ServerMessage) data; //data from server

				// User connections with server
				if (incomingData.messageType == 0) {
					primaryStage.setScene(sceneMap.get("decision"));
				}
				//accepted player username (put them on waiting screen)
				else if (incomingData.messageType == 1) {
					primaryStage.setScene(sceneMap.get("choosePlayer"));
					clientConnection.sendRequest(new Request(-1)); //update Userlist for all clients
				}
				//matched with opponent & reset gui elements (person or AI)
				else if (incomingData.messageType == 2) {
					resetMainScreen(incomingData.username);
				}
				// initializes game after ships are placed
				else if (incomingData.messageType == 3) {
					flipButton.setVisible(false);
					if (incomingData.playerHasFirstMove) { //player going first
						gameAction.setText("You Attack First");
						enemyBoard.lockboard = false;

					} else { //player going second
						gameAction.setText("Opponent Goes First");
					}
				}
				//updates all gui elements on players own shot
				else if (incomingData.messageType == 4) {
					updateEnemyBoard(incomingData.shotResult,incomingData.shipSunk, incomingData.numShipsSunk);
				}
				//get players on server (updates every time a player joins or leaves)
				else if (incomingData.messageType == 5) {
					UpdatePlayerList(incomingData.playersOnServer);
				}
				//opponent shot data received (During opponents turn)
				else if (incomingData.messageType == 50) {
					playerBoard.setCell(incomingData.shotXCord,incomingData.shotYCord,incomingData.shotResult);
					if (!incomingData.shotResult && incomingData.numShipsSunk != 5) { //opponent missed (don't change when game ends)
						gameAction.setText("Attack Enemy");
						enemyBoard.lockboard = false;
					}
				}
				//notification request recieved from other player
				else if (incomingData.messageType == 51) {
					if (primaryStage.getScene() == sceneMap.get("choosePlayer")) {
						addNotification(incomingData.username);
					}
				}
				// player lost the game
				else if (incomingData.messageType == 52) {
					StackPane overlay = createWinOverlay(0);
					root.getChildren().add(overlay);
					enemyBoard.lockboard = true;
				}
				//client placed ships and is waiting for opponent to place ships
				else if (incomingData.messageType == 53) {
					gameAction.setText("WAITING FOR ENEMY");
					flipButton.setVisible(false);
				}

				/////////////////ERROR CODES/////////////////////////////////////

				//Server rejected player username
				else if (incomingData.messageType == -1) {
					errorMessage.setVisible(true);
				}
				//other player disconnected during game
				else if (incomingData.messageType == -2) {
					StackPane overlay = createWinOverlay(2);
					root.getChildren().add(overlay);
					enemyBoard.lockboard = true;
				}
				//user sent a notification to a player pending in another match
				else if (incomingData.messageType == -8) {
					lobbyError.setText("Opponent Pending Another Match");
					lobbyError.setVisible(true);
					primaryStage.setScene(sceneMap.get("choosePlayer"));
				}
				//opponent denied notification
				else if (incomingData.messageType == -9) {
					waitingText.setText("Request Denied");
					waitingBackButton.setVisible(true);
				}
			});
		});

		clientConnection.start(); //runs the run() function in client

		sceneMap.put("decision", createDecisionScreen()); //screen that lets the player choose between playing a computer and AI
		sceneMap.put("username",createUsernameScreen()); //Screen to input username
		sceneMap.put("main",createMainScreen());  //main play screen
		sceneMap.put("waiting",createWaitingScreen()); //screen used when waiting to connect to server or find an opponent
		sceneMap.put("choosePlayer",createChoosePlayerScreen()); //Lobby Screen where players can match make

	            primaryStage.setTitle("BattleShip Client");
	            primaryStage.setScene(sceneMap.get("waiting")); //set waiting screen until server connects
				primaryStage.setResizable(false);
	            primaryStage.show();
			 primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				 @Override
				 public void handle(WindowEvent t) {
					 Platform.exit();
					 System.exit(0);
				 }
			 });
	}

	//resets all elements on main screen
	private void resetMainScreen(String opponentName) {
		opponent = opponentName;
		flipButton.setVisible(true);
		opponentLabel.setText("Opponent: " + opponent);
		gameAction.setText("PLACE SHIPS");
		SetUpNumberOfShipsLabel(0);
		primaryStage.setScene(sceneMap.get("main"));
	}

	//creates game end and disconnected player overlay
	private StackPane createWinOverlay(int messageNumber) {
		StackPane overlay = new StackPane();
		overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
		overlay.setPrefSize(300, 530);

		// Message box
		VBox messageBox = new VBox(20);
		messageBox.setAlignment(Pos.CENTER);

		// Win label
		Label winLabel = new Label();
        switch (messageNumber) {
            case 0:  winLabel.setText("YOU LOSE!"); break;
            case 1:  winLabel.setText("YOU WIN!"); break;
            case 2:  winLabel.setText("Player Disconnected"); break;
        }
		winLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
		winLabel.setTextFill(Color.WHITE);

		// Buttons container
		HBox buttonBox = new HBox(10);
		buttonBox.setAlignment(Pos.CENTER);

		// Configure buttons
		Button backButton = new Button("Back");
		Button newGameButton = new Button("New Game");
		backButton.setStyle("-fx-background-color: darkgray; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 1px;");
		backButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
		newGameButton.setStyle("-fx-background-color: darkgray; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 1px;");
		newGameButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));

		backButton.setOnAction(e -> {
			overlay.setVisible(false);
			playerBoard.clearBoard(false); enemyBoard.clearBoard(true); //clear boards
			if (opponent.equals("AI")) {
				primaryStage.setScene(sceneMap.get("decision"));
			}else {
				clientConnection.sendRequest(new Request(-4,opponent)); //deny any other players requests
				clientConnection.sendRequest(new Request(-1)); //update Userlist for all clients
				primaryStage.setScene(sceneMap.get("choosePlayer"));
			}
		});

		newGameButton.setOnAction(e -> {
			playerBoard.clearBoard(false); enemyBoard.clearBoard(true);
			overlay.setVisible(false);
			resetMainScreen(opponent);
			if (opponent.equals("AI")){clientConnection.sendRequest(new Request(1,"")); }
		     else {
				 clientConnection.sendRequest(new Request(1,opponent));
				 waitingText.setText("Waiting for " + opponent + " To Accept...");
				 waitingBackButton.setVisible(false);
				 primaryStage.setScene(sceneMap.get("waiting"));
			 }
		});
		if (messageNumber == 2) { 	buttonBox.getChildren().addAll(backButton);} //if player disconnected hide new game button
		else { buttonBox.getChildren().addAll(backButton, newGameButton); }
		messageBox.getChildren().addAll(winLabel, buttonBox);
		overlay.getChildren().add(messageBox);
		StackPane.setAlignment(messageBox, Pos.CENTER);

		return overlay;
	}

	//updates all elements of the enemy board and other gui fields when the server sends back update on players attack
	private void updateEnemyBoard(boolean shotResult,boolean shipSunk,int numberOfShipsSunk) {
		if (shotResult) { //hit
			enemyBoard.ShotAtCell.setFill(Color.RED);
			if (shipSunk) { //if a ship was sunk update screen
				SetUpNumberOfShipsLabel(numberOfShipsSunk);
				if(numberOfShipsSunk == 5) { //if all 5 ships are sunk end game (win)
					StackPane overlay = createWinOverlay(1);
					root.getChildren().add(overlay);
					enemyBoard.lockboard = true;

				}
			}
		} else { //miss and switch turn to opponent
			enemyBoard.ShotAtCell.setFill(Color.WHITE);
			enemyBoard.lockboard = true;
			gameAction.setText("Opponents Turn");
		}
	}

	//main screen global variables
	Board playerBoard = new Board(false); //Bottom Board (holds ships)
	Board enemyBoard = new Board(true);  //Top Board (For Attacking)
	Label gameAction = new Label("PLACE SHIPS");  //tells player what to do next in bottom right corner
	Label opponentLabel = new Label("Opponent Name"); //opponent name in top right corner
	Button flipButton = new Button("Flip"); //Button that allows ships to be flipped form horizontal to vertical
	Label NumberOfShips = new Label(""); //label showing number of ships sunk by that player in the game
	StackPane root = new StackPane();

	//creates main screen
	private Scene createMainScreen() {

		playerBoard.setOnMouseClicked(event -> { //runs on every mouse click of player board
			if (!playerBoard.lockboard && playerBoard.ships[playerBoard.currentShip] == 0) { //player has placed all ships
				playerBoard.lockboard = true;
				clientConnection.sendRequest(new Request(2, playerBoard.shipPositions));
			}
		});

		enemyBoard.setOnMouseClicked(event -> { //runs on every mouse click of enemy board (sends shot data to server)
					if (!enemyBoard.lockboard) { //player has placed all ships
						clientConnection.sendRequest(new Request(3, enemyBoard.ShotAtCell.x, enemyBoard.ShotAtCell.y));
					}
		});
		enemyBoard.lockboard = true;

		// Setting up Label styles
		SetUpNumberOfShipsLabel(0);
		NumberOfShips.setStyle("-fx-fill: #b22020; -fx-font-size: 18px; -fx-background-color: #b9b9b9; -fx-border-color: black; -fx-border-width: 3px; -fx-font-weight: bold");
		NumberOfShips.setPadding(new Insets(5));

		opponentLabel.setStyle("-fx-fill: white; -fx-background-color: #b9b9b9; -fx-font-size: 18px; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 3px");
		opponentLabel.setPadding(new Insets(5));

		gameAction.setTextFill(Color.WHITESMOKE);
		gameAction.setStyle("-fx-background-color: #626262FF; -fx-font-size: 18px; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 3px; -fx-fill: white");
		gameAction.setPadding(new Insets(5));

		flipButton.setOnAction(event -> playerBoard.vertical = !playerBoard.vertical);
		flipButton.setTextFill(Color.web("#ffffff"));
		flipButton.setStyle("-fx-background-color: #606060; -fx-border-color: #c0c0c0; -fx-border-radius: 3px; -fx-border-width: 3px;");
		flipButton.setOnMouseEntered(e -> flipButton.setStyle("-fx-background-color: #c9c9c9; -fx-border-radius: 3px; -fx-border-color: #c0c0c0; -fx-border-width: 3px;"));
		flipButton.setOnMouseExited(e -> flipButton.setStyle("-fx-background-color: #606060; -fx-border-radius: 3px; -fx-border-color: #c0c0c0; -fx-border-width: 3px;"));

		// Layout for the  text
		HBox top = new HBox(10, NumberOfShips, opponentLabel);
		top.setAlignment(Pos.CENTER);
		HBox bottomText = new HBox(10, gameAction,flipButton);
		bottomText.setAlignment(Pos.CENTER);

		// Adding a visual divider
		Separator separator = new Separator();
		separator.setPadding(new Insets(0)); // Reduced padding for the separator

		// Main layout containing all components
		VBox mainBox = new VBox(5, top, enemyBoard, separator, playerBoard, bottomText);
		mainBox.setAlignment(Pos.CENTER);
		mainBox.setPadding(new Insets(5)); // Minimized padding to reduce extra spacing
		mainBox.setStyle(" -fx-font-family: 'serif';");

		Image image = new Image("res/background.png"); // Adjust path to where your image is stored
		BackgroundImage bgImage = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
		mainBox.setBackground(new Background(bgImage));

		root.getChildren().addAll(mainBox);
		return new Scene(root, 300, 530);
	}

	//creates and resets the number of ships label on the top left of screen
	private void SetUpNumberOfShipsLabel(int numberOfShipsSunk) {
		Text preText = new Text("Sunk Ships: ");
		preText.setStyle("-fx-fill: #2c2c2c");
		Text shipsText = new Text(String.valueOf(numberOfShipsSunk));
		shipsText.setStyle("-fx-fill: #d93535");
		TextFlow textFlow = new TextFlow(preText, shipsText);
		NumberOfShips.setGraphic(textFlow);
	}

	//notifcation that is displayed on chooseOpponent Screen has a string and two buttons
	private class Notification{
		String text;
		Button accept, deny;

		Notification(String text, Button accept, Button deny) {
			this.text = text;
			this.accept = accept;
			this.deny = deny;
		}
	}

	//creates a notification and adds it to players chooseOpponent Screen
	private void addNotification(String username) {
		Button acceptButton = new Button("✔");
		Button denyButton = new Button("X");
		acceptButton.setMinSize(12,8);
		denyButton.setMinSize(12,8);
		acceptButton.setStyle("-fx-background-color: #64da64");
		denyButton.setStyle("-fx-background-color: #e74f4f");
		Notification notification = new Notification("Play " + username + "?",acceptButton,denyButton);
		acceptButton.setOnAction(e -> {
			notifications.getItems().remove(notification); //removes notification
			clientConnection.sendRequest(new Request(1,username)); //sends request to start game
		});
		denyButton.setOnAction(e -> { //if button is pressed accepts notification
			notifications.getItems().remove(notification); //removes notification
			lobbyError.setVisible(false);
			clientConnection.sendRequest(new Request(-4,username)); //sends denied request to opponent
		});
		notifications.getItems().add(notification);
	}

	private void UpdatePlayerList(ArrayList<String> playersOnServer) {
		players.getItems().clear();
		for (String player : playersOnServer) {//add all players as buttons to list
			Button playerButton = new Button(player);
			playerButton.setStyle("-fx-border-width: 2px; -fx-text-fill: white; -fx-border-color: black; -fx-background-color: #707070");
			playerButton.setOnAction( e -> { //sends request to play that user if button is pressed
				clientConnection.sendRequest(new Request(1,player)); //send request to opponent

				primaryStage.setScene(sceneMap.get("waiting")); //go on waiting screen
				waitingText.setText("Waiting For " + player + " To Accept...");
				waitingBackButton.setVisible(false);
				lobbyError.setVisible(false);
			});
			players.getItems().add(playerButton); //add button to players list
		}
	}

	//choosePlayer Screen global variables
	Text lobbyError = new Text("Error Text");
	ListView<Button> players = new ListView<>();
	ListView<Notification> notifications = new ListView<>();
	//create match making screen
	private Scene createChoosePlayerScreen() {
		//allows notification to have a string and two buttons in the listview
		notifications.setCellFactory(new Callback<ListView<Notification>, ListCell<Notification>>() {
			@Override
			public ListCell<Notification> call(ListView<Notification> notificationListView) {
				return new ListCell<Notification>() {
					@Override
					protected void updateItem(Notification item, boolean empty) {
						super.updateItem(item, empty);
						setBackground(null); // Clear the background for each cell
						if (empty || item == null) {
							setText(null);
							setGraphic(null);
						} else {
							Text text = new Text(item.text);
							text.setFill(Color.WHITESMOKE);
							HBox hBox = new HBox(text, item.accept, item.deny);
							hBox.setSpacing(4);
							setGraphic(hBox);
						}
					}
				};
			}
		});

		//creates player buttons
		players.setCellFactory(lv -> new ListCell<Button>() {
			@Override
			protected void updateItem(Button item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setBackground(null);
				} else {
					setGraphic(item);  // Set the button as the graphic for the list cell
				}
			}
		});

		lobbyError.setStyle("-fx-fill: #cb1313; -fx-font-size: 11px;");

		//set image of player panel
		Image listViewImageLeft = new Image("res/leftPanel.jpg");
		BackgroundImage playerBackground = new BackgroundImage(
				listViewImageLeft,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(100, 300, true, true, false, true)
		);

		//set image of notification panel
		Image listViewImageRight = new Image("res/rightPanel.jpg");
		BackgroundImage notBackground = new BackgroundImage(
				listViewImageRight,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(300, 200, true, true, false, true)
		);

		players.setBackground(new Background(playerBackground));
		notifications.setBackground(new Background(notBackground));
		notifications.setStyle("-fx-font-size: 13px; -fx-font-family: 'serif'; -fx-fill: #d9d9d9; -fx-border-width: 4px; -fx-border-color: black;");
		players.setStyle("-fx-border-width: 4px; -fx-border-color: black;");
		// Text for player list
		Text leftText = new Text("Pick Opponent:");
		leftText.setStyle("-fx-fill: #c5c5c5;");
		lobbyError.setVisible(false);
		// Text for notifications
		Text rightText = new Text("Notifications:");
		rightText.setStyle("-fx-fill: #c5c5c5;");

		// ListView for players - make it narrower
		players.setMaxWidth(130);
		players.setMaxHeight(150);
		notifications.setMinWidth(120);  // Wider width

		// VBox for players and top text
		VBox playerBox = new VBox(5, leftText, players);
		playerBox.setStyle("-fx-font-size: 14px; -fx-font-family: 'serif';");
		playerBox.setAlignment(Pos.TOP_CENTER);
		playerBox.setPadding(new Insets(5));

		// VBox for notifications and middle text
		VBox notificationBox = new VBox(5, rightText, notifications,lobbyError);
		notificationBox.setStyle("-fx-font-size: 14px; -fx-font-family: 'serif';");
		notificationBox.setAlignment(Pos.TOP_CENTER);
		notificationBox.setPadding(new Insets(5));

		// HBox to hold both VBoxes
		HBox root = new HBox(8, playerBox, notificationBox);
		root.setStyle("-fx-background-color: #bebebe;");
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(10));  // Padding around the entire layout

		Image backgroundImage = new Image("res/SteelPanelBackgrouind.jpg");
		BackgroundImage bgImage = new BackgroundImage(
				backgroundImage,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(100, 100, true, true, false, true)
		);
		root.setStyle("-fx-font-family: 'serif'; -fx-font-weight: bold; -fx-font-size: 15px;");
		root.setBackground(new Background(bgImage));

		return new Scene(root, 300, 200);  // Adjusted width for better layout spacing
	}

	//global variables for the waiting screen
	Text waitingText = new Text("WAITING FOR SERVER...");
	Button waitingBackButton = new Button("Back");

	//create waiting screen when waiting to connect to the server and waiting for player to accept request
	private Scene createWaitingScreen() {

		Image logoImage = new Image("res/BattleShipLogo.png");
		ImageView logoView = new ImageView(logoImage);
		logoView.setPreserveRatio(true);
		logoView.setFitHeight(50);

		waitingBackButton.setStyle("-fx-background-color: darkgray; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 1px;");
		waitingBackButton.setOnAction( e-> {
			clientConnection.sendRequest(new Request(4));
			primaryStage.setScene(sceneMap.get("choosePlayer"));
		});
		waitingBackButton.setVisible(false);

		waitingText.setFill(Color.web("#a43a3a"));

		VBox box = new VBox(20,logoView,waitingText,waitingBackButton);
		box.setStyle(" -fx-font-size: 18px; -fx-font-family: 'serif'; -fx-background-color: #bebebe; " );
		box.setAlignment(Pos.CENTER);
		//set background image
		Image backgroundImage = new Image("res/SteelPanelBackgrouind.jpg");
		BackgroundImage bgImage = new BackgroundImage(
				backgroundImage,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(100, 100, true, true, false, true)
		);
		box.setStyle("-fx-font-family: 'serif'; -fx-font-weight: bold; -fx-font-size: 22px;");
		box.setBackground(new Background(bgImage));

		return new Scene(box, 300, 200);
	}

	//fist screen displayed, lets player choose between a AI opponent or go into match making
	private Scene createDecisionScreen() {
		//create game logo on top of screen
		Image logoImage = new Image("res/BattleShipLogo.png");
		ImageView logoView = new ImageView(logoImage);
		logoView.setPreserveRatio(true);
		logoView.setFitHeight(75);

		//create buttons
		Button computerButton = new Button("COMPUTER");
		Button personButton = new Button("PERSON");
		computerButton.setStyle("-fx-background-color: #5e5e5e; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2px;");
		personButton.setStyle("-fx-background-color: #5e5e5e; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2px;");
		computerButton.setMinSize(100,40);
		personButton.setMinSize(100,40);

		computerButton.setOnAction(e -> {  //starts game against AI, tells server player is playing against an AI
			clientConnection.sendRequest(new Request(1,""));
			opponentLabel.setText("COMPUTER");
			primaryStage.setScene(sceneMap.get("main"));
		});
		computerButton.setOnMouseEntered(e->{
			computerButton.setStyle("-fx-background-color: #adadad; -fx-text-fill: #1f1f1f; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2px;");

		});
		computerButton.setOnMouseExited(e-> {
			computerButton.setStyle("-fx-background-color: #5e5e5e; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2px;");
		});

		personButton.setOnAction(e -> { //user will have to create a username to play against another player
			primaryStage.setScene(sceneMap.get("username"));
		});
		personButton.setOnMouseEntered(e->{
			personButton.setStyle("-fx-background-color: #adadad; -fx-text-fill: #1f1f1f; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2px;");
		});
		personButton.setOnMouseExited(e-> {
			personButton.setStyle("-fx-background-color: #5e5e5e; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2px;");
		});

		HBox buttons = new HBox(20, computerButton, personButton);
		buttons.setAlignment(Pos.CENTER);

		VBox mainBox = new VBox(20, logoView, buttons);
		mainBox.setAlignment(Pos.CENTER);
		mainBox.setStyle("-fx-font-family: 'serif';");

		// Load background image and set it to fit the scene
		Image backgroundImage = new Image("res/cruiserbackground.jpg");
		BackgroundImage bgImage = new BackgroundImage(
				backgroundImage,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(300, 200, true, true, false, true)
		);
		mainBox.setBackground(new Background(bgImage));

		return new Scene(mainBox, 300, 200);  // Scene size can be adjusted to match your needs
	}

	//global variable for username Screen
	Text errorMessage = new Text("Invalid Username");
	private Scene createUsernameScreen() {
		//set up logo
		TextField usernameBox = new TextField();
		Image logoImage = new Image("res/BattleShipLogo.png");
		ImageView logoView = new ImageView(logoImage);
		logoView.setPreserveRatio(true);
		logoView.setFitHeight(75);  // Set the height of the image (adjust as necessary)
		logoView.setTranslateY(20);

		Text text = new Text("Username: ");
		text.setFill(Color.WHITESMOKE);
		Text text2 = new Text(" Press Enter ");
		text2.setFill(Color.WHITESMOKE);

		errorMessage.setVisible(false);
		errorMessage.setFont(Font.font("Verdana", 12));
		errorMessage.setFill(Color.rgb(255,0,0));

		usernameBox.setStyle("-fx-background-color: lightgray; -fx-border-color: black; -fx-border-width: 3px;");
		usernameBox.setOnKeyPressed(e-> {  //on enter if amount is valid allows user to start
			if (e.getCode() == KeyCode.ENTER) {
				username = usernameBox.getText();
				primaryStage.setTitle("BattleShip - " + username);
				clientConnection.sendRequest(new Request(0,usernameBox.getText())); //sends Username to server to confirm that it is valid
			}
		});

		HBox contents = new HBox(text,usernameBox,text2);
		contents.setTranslateY(-10);
		contents.setAlignment(Pos.CENTER);
		VBox clientBox = new VBox(25,logoView,errorMessage,contents);
		clientBox.setAlignment(Pos.CENTER);
		clientBox.setPadding(new Insets(20));
		clientBox.setStyle("-fx-font-family: 'serif'; -fx-font-weight: bold; -fx-font-size: 15px;");

		//set up background image
		Image backgroundImage = new Image("res/cruiserbackground.jpg");
		BackgroundImage bgImage = new BackgroundImage(
				backgroundImage,
				BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER,
				new BackgroundSize(100, 100, true, true, false, true)
		);
		clientBox.setBackground(new Background(bgImage));

		return new Scene(clientBox, 300, 200);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
