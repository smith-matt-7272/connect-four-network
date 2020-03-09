/*
 * COSC190 Assign 4
 * Logan Olfert
 * Matt Smith
 * April 2019
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ConnectFourServer extends Application {

	public static final int GAME_PORT = 8965;
	
	//Window and Graphic Attributes
	private GameBoard gameBoard;
	private BorderPane mainBorderPane;
	private HBox hb;
	private Label lblPlayer;
	private ChatPane chatPane;
	private DropShadow ds;

	//Data streams
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;

	//Client and Host names
	private String sClient;
	private String sHost = "Host";

	//Game turn
	private boolean bHostTurn;

	//Consumer used to communicate between board/server
	private Consumer<Integer> comms_function;

	//File paths for images
	private static final String IMG_STARTSCREEN = "file:images/startScreen.png";
	private static final String IMG_YELLOWCHIP = "file:images/yellowchip.png";
	private static final String IMG_TOP_BANNER = "file:images/top.png";
	private static final String IMG_BOTTOM_BANNER = "file:images/botton.png";
	private static final String IMG_ENDSCREEN = "file:images/endScreen.png";

	//Common Font
	private static final String COMMON_FONT = "Century Gothic";

	//Labels
	private static final String TITLE = "Connect Four Hyper Ultimate Server";
	private static final String WAITING = "Waiting for Connection...";
	
	//Helper to delay to End Game Screen
	private int count = 0;

	//Validations and error messages
	private static final int MIN_NAME_LENGTH = 2;
	private static final int MAX_NAME_LENGTH = 30;
	private static final String NO_ONE_LISTENING = "No connections to a client. No one is listening.";
	
	@Override
	public void start(Stage obStage) {
		
		this.comms_function = ((x)->sendMessage(x));								//Establish our function with the sendMessage method
		this.mainBorderPane = new BorderPane();										//Set the main pane for the application

		ds = new DropShadow();														//Build the dropshadow for the header label
		ds.setColor(Color.WHITE);
		ds.setRadius(5);
		ds.setOffsetX(0);
		ds.setOffsetY(0);

		ImageView ivStartScreen = new ImageView(new Image(IMG_STARTSCREEN));				//Set the start screen image and stylings
		ivStartScreen.setFitHeight(850);
		StackPane stackStart = new StackPane();												//Setup the stackpane for the start screen

		hb = new HBox();																	//Setup the styling for the header
		hb.setAlignment(Pos.CENTER);
		lblPlayer = new Label(WAITING);
		lblPlayer.setFont(Font.font(COMMON_FONT, FontWeight.BOLD,30));
		lblPlayer.setEffect(ds);
		hb.getChildren().add(lblPlayer);
		
		HBox obOpen = new HBox();
		obOpen.setMinHeight(100);
		obOpen.setAlignment(Pos.BOTTOM_CENTER);
		obOpen.setPadding(new Insets(75));
		obOpen.setSpacing(20);
		
		Label lblName = new Label("Name: ");
		lblName.setFont(Font.font(COMMON_FONT, FontWeight.BOLD, 20));
		TextField txtName = new TextField();
		Button btnStart = new Button("Start");
		btnStart.setFont(Font.font(COMMON_FONT, FontWeight.BOLD, 13.5));
		btnStart.setDisable(true);
		
		//Ensures names longer than MIN_NAME_LENGTH characters are entered
		txtName.setOnKeyReleased(e-> {
				btnStart.setDisable(txtName.getText().length() > MIN_NAME_LENGTH && txtName.getText().length() < MAX_NAME_LENGTH);
		});
	
		btnStart.setOnAction(e-> {
			//Once a name is entered, creates GameBoard
			this.sHost = txtName.getText();
			makeGame();
			Platform.runLater(()->startGame());
		});
		
		obOpen.getChildren().addAll(lblName,txtName,btnStart);
		
		stackStart.getChildren().addAll(ivStartScreen,obOpen);
		
		mainBorderPane.setCenter(stackStart);

		Scene obScene = new Scene(mainBorderPane,700,850);
	
		obStage.setScene(obScene);
		obStage.setTitle(TITLE);
		obStage.setResizable(false);
		obStage.show();
		obStage.getIcons().add(new Image(IMG_YELLOWCHIP));
	}
	
	/**
	 * Sets up the GameBoard and the Chat Window
	 * Sets up starting game conditions
	 * Begins the GameOverCheck which determines when win
	 * conditions are achieved
	 */
	public void makeGame()
	{
		gameBoard = new GameBoard(this.comms_function);											//Make the gameboard, and send the function with it
		this.gameBoard.setPlayer(1);															//Set initial variables
		this.gameBoard.gameOver = false;
		this.gameBoard.winner = false;
		
		ImageView obView = new ImageView(new Image(IMG_TOP_BANNER));							//Set the stylings of the gameboard
		StackPane obStackTop = new StackPane();
		obStackTop.getChildren().addAll(obView,hb);
		this.mainBorderPane.setCenter(gameBoard);
		this.chatPane = new ChatPane(this.sClient);
		mainBorderPane.setTop(obStackTop);
		StackPane obStackBottom = new StackPane();
		ImageView obViewBottom = new ImageView(new Image(IMG_BOTTOM_BANNER));
		obStackBottom.getChildren().addAll(obViewBottom,this.chatPane);
		mainBorderPane.setBottom(obStackBottom);
		
		this.chatPane.getSendButton().setOnAction(e-> sendChat() );								//Handlers for send and clear buttons
		this.chatPane.getClearButton().setOnAction(e-> this.chatPane.clearChat());

		Platform.runLater(()->startGameOverCheck());											//Start the thread to watch for gameover flag
	}

	/**
	 * Initializes thread to set the server and comms protocols
	 */
	private void startGame()
	{
		Thread obThread = new Thread(()-> runGame());
		obThread.setDaemon(true);
		obThread.start();
	}
	
	/**
	 * Handles connection with client and communication
	 * protocol through String signal words
	 */
	private void runGame()
	{
		ServerSocket obServer = null;
		Socket obSock = null;
		String sPrompt;
		
		try
		{
			obServer = new ServerSocket(ConnectFourServer.GAME_PORT);					//Setup the server socket
			obSock = obServer.accept();													//Wait for client to accept connection
			
			this.dataInputStream = new DataInputStream(obSock.getInputStream());		//Set the IO streams
			this.dataOutputStream = new DataOutputStream(obSock.getOutputStream());
			
			this.sClient = this.dataInputStream.readUTF();								//Read in the clients name sent at time of connection
			this.dataOutputStream.writeUTF(this.sHost);									//Send server's player name to client
			estOrder();																	//Sets up the player turn order
			
			this.gameBoard.setTurn(this.bHostTurn);										//Set the gameboards aesthetics per turn order

			if(this.bHostTurn)					//Set the styling of the gameboard depending on whose turn it is
			{
				myTurn();
			}
			else
			{
				notMyTurn();
			}
			
			while(true)
			{
				sPrompt = this.dataInputStream.readUTF();								//Read in messages from client

				if(sPrompt.equals("PLAY"))												//When opponent places a chip, the PLAY signal is sent by them
				{
					int nCol = this.dataInputStream.readInt();							//Play gets followed by a column
					this.gameBoard.setOpposPos(nCol, 2);						//Place the piece on the gameboard
					this.gameBoard.setTurn(true);										//Server player's turn
					myTurn();															//Set our turn
				}

				if(sPrompt.equals("MSG"))												//Send button in chat sends MSG signal
				{
					String sInp = this.dataInputStream.readUTF();						//Read in the input
					Platform.runLater(()->this.chatPane.getChatHist().appendText(sInp));//Add the input to our chat window
				}
			}
		}
		catch(IOException exp)	{	exp.printStackTrace();	}
		finally
		{
			try
			{
				obSock.close();									//Close the socket when done
				dataInputStream.close();						//Close input stream
				dataOutputStream.close();						//Close output stream
			}
			catch(IOException exp)	{	exp.printStackTrace();}
		}
	}
	
	/**
	 * When this player places a chip, it sends the signal
	 * String to the client, along with the chip position used
	 * on the GameBoard, then switches its turn to false
	 * 
	 * @param nCol - Column player placed a chip into
	 */
	public void sendMessage(int nCol)
	{
		try
		{
			notMyTurn();
			this.dataOutputStream.writeUTF("PLAY");			//Protocol message for client
			this.dataOutputStream.writeInt(nCol);				//Send the column played to client
			this.gameBoard.setTurn(false);						//No longer server's turn
		}
		catch(IOException e) {e.printStackTrace();}
	}

	/**
	 * Updates the graphics and text content of the
	 * header to indicate that it is player's turn
	 */
	private void myTurn()
	{
		Platform.runLater( () ->
		{
			this.lblPlayer.setText("Your Turn");
			lblPlayer.setTextFill(Color.GREEN);
			ds.setColor(Color.GREEN);
		});
	}
	
	/**
	 * When the other players turn, updates
	 * text string at the top of the window
	 */
	private void notMyTurn()
	{
		Platform.runLater( () ->
		{
			this.lblPlayer.setText(sClient + "'s Turn");
			lblPlayer.setTextFill(Color.RED);
			ds.setColor(Color.RED);
		});
	}

	/**
	 * When the Send button in chat is activated, this method
	 * sends the MSG string signal word, followed by the text
	 * from the chat field, and appends the server's chat history
	 */
	public void sendChat()
	{
		try
		{
			String sMessage = this.sHost+": " +this.chatPane.getTextInput().getText()+"\n";		//Establish the message
			this.dataOutputStream.writeUTF("MSG");											//Protocol MSG to indicate handling on Client end
			this.dataOutputStream.writeUTF(sMessage);											//Send the message
			Platform.runLater(()-> this.chatPane.getChatHist().appendText(sMessage));			//Append the message to server chat window
			Platform.runLater(()-> this.chatPane.getTextInput().clear());						//Clear the input
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(NullPointerException npe)
		{
			Platform.runLater(()-> this.chatPane.getChatHist().appendText(NO_ONE_LISTENING));	//If not connected to a client, indicate messages
			Platform.runLater(()-> this.chatPane.getTextInput().clear());						//are going no where
			npe.printStackTrace();
		}
	}

	/**
	 * Establish player order at start of game
	 * @throws IOException - If any issues on output
	 */
	private void estOrder() throws IOException
	{
		int nVal = (int)(Math.random()*100);
		if(nVal > 49)
		{
			this.bHostTurn = true;
		}
		
		this.dataOutputStream.writeBoolean(!bHostTurn);
	}

	/**
	 * Create a thread to watch for gameover flag
	 */
	private void startGameOverCheck()
	{
		Thread obThread = new Thread(()->runGameOverCheck());
		obThread.setDaemon(true);
		obThread.start();
	}

	/**
	 * Loop during gameplay to deteremine watch for the gameOver flag
	 * to be set
	 * Once set, the loop begins a 5 second counter (using a sleep)
	 * before it transitions to GameOverScreen
	 */
	private void runGameOverCheck()
	{
		try
		{
			while(count<10)
			{
				Platform.runLater( () -> {
					if(gameBoard.gameOver)
					{
						count ++;											//Once the flag is set, start incrementing the counter
						if(gameBoard.winner)								//If the server player is a winner, set the styling and messaging
						{													//appropriately
							this.lblPlayer.setText("You Win!");
							lblPlayer.setTextFill(Color.GREEN);
							ds.setColor(Color.GREEN);
						}
						else
						{
							this.lblPlayer.setText("You Lose!");
							lblPlayer.setTextFill(Color.RED);
							ds.setColor(Color.RED);
						}
						if(count == 9)										//Once the counter reaches 9th step (5 seconds)
						{													//transition to gameOverScreen
							Platform.runLater(() -> gameOverScreen());
						}
					}
				});
				Thread.sleep(500);
			}
		}
		catch(InterruptedException exp)
		{
			exp.printStackTrace();
		}
	}
	
	/**
	 * Sets up the end game screen after someone wins the game
	 */
	private void gameOverScreen()
	{
		StackPane obEndPane = new StackPane();								//Create the stackpane for the screen
		ImageView obGameOver = new ImageView(new Image(IMG_ENDSCREEN));		//Setup the background image
		Button btnExit = new Button("Exit Game");						//Add the exit button
		btnExit.setFont(Font.font(COMMON_FONT, FontWeight.BOLD, 15));	//Set the button styling and text
		btnExit.setPadding(new Insets(25));
		btnExit.setAlignment(Pos.BOTTOM_CENTER);
		btnExit.setOnAction(e-> System.exit(0));						//Click handler on the button to exit the program
		HBox obH = new HBox();												//Horizontal layout for the button
		obH.setAlignment(Pos.BOTTOM_CENTER);								//Set the styling for the HBox
		obH.setPadding(new Insets(175));
		obH.getChildren().add(btnExit);										//Add the button to the HBox
		obEndPane.getChildren().addAll(obGameOver,obH);						//Add the imageview and HBox to the stackpane
		
		mainBorderPane.setCenter(obEndPane);								//Set the stackpane to the main display screen
	}

	/**
	 * Main method to run the server
	 * @param args - UNUSED
	 */
	public static void main(String[] args) {Application.launch(args);}
}
