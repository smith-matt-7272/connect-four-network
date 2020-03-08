/*
 * COSC190 Assign 4
 * Logan Olfert
 * Matt Smith
 * 
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
	private Consumer<Integer> func;

	//File paths for images
	private static final String IMG_STARTSCREEN = "file:images/startScreen.png";
	private static final String IMG_YELLOWCHIP = "file:images/yellowchip.png";
	private static final String IMG_TOP_BANNER = "file:images/top.png";
	private static final String IMG_BOTTOM_BANNER = "file:images/botton.png";
	private static final String IMG_ENDSCREEN = "file:images/endScreen.png";

	//Common Font
	private static final String COMMON_FONT = "Century Gothic";

	//Main Title
	private static final String TITLE = "Connect Four Hyper Ultimate Server";
	
	//Helper to delay to End Game Screen
	private int count = 0;

	//Validations
	private static final int MIN_NAME_LENGTH = 2;
	private static final int MAX_NAME_LENGTH = 30;
	private static final String NO_ONE_LISTENING = "No connections to a client. No one is listening.";
	
	@Override
	public void start(Stage obStage) {
		
		this.func = ((x)->sendMessage(x));
		this.mainBorderPane = new BorderPane();
		
		ds = new DropShadow();
		ds.setColor(Color.WHITE);
		ds.setRadius(5);
		ds.setOffsetX(0);
		ds.setOffsetY(0);

		ImageView obStart = new ImageView(new Image(IMG_STARTSCREEN));
		obStart.setFitHeight(850);
		StackPane obStackStart = new StackPane();

		hb = new HBox();
		hb.setAlignment(Pos.CENTER);
		lblPlayer = new Label("Waiting for Connection");
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
			if(txtName.getText().length() > MIN_NAME_LENGTH && txtName.getText().length() < MAX_NAME_LENGTH)
			{
				btnStart.setDisable(false);
			}
			else
			{
				btnStart.setDisable(true);
			}
		});
	
		btnStart.setOnAction(e-> {
			//Once a name is entered, creates GameBoard
			this.sHost = txtName.getText();
			makeGame();
			Platform.runLater(()->startGame());
		});
		
		obOpen.getChildren().addAll(lblName,txtName,btnStart);
		
		obStackStart.getChildren().addAll(obStart,obOpen);
		
		mainBorderPane.setCenter(obStackStart);

		Scene obScene = new Scene(mainBorderPane,700,850);
	
		obStage.setScene(obScene);
		obStage.setTitle(TITLE);
		obStage.setResizable(false);
		obStage.show();
		obStage.getIcons().add(new Image(IMG_YELLOWCHIP));
	}
	
	/*
	 * Sets up the GameBoard and the Chat Window
	 * Sets up starting game conditions
	 * Begins the GameOverCheck which determines when win
	 * conditions are achieved
	 */
	public void makeGame()
	{
		gameBoard = new GameBoard(this.func);
		this.gameBoard.setPlayer(1);
		this.gameBoard.gameOver = false;
		this.gameBoard.winner = false;
		
		ImageView obView = new ImageView(new Image(IMG_TOP_BANNER));
		StackPane obStackTop = new StackPane();
		obStackTop.getChildren().addAll(obView,hb);
		this.mainBorderPane.setCenter(gameBoard);
		this.chatPane = new ChatPane(this.sClient);
		mainBorderPane.setTop(obStackTop);
		StackPane obStackBottom = new StackPane();
		ImageView obViewBottom = new ImageView(new Image(IMG_BOTTOM_BANNER));
		obStackBottom.getChildren().addAll(obViewBottom,this.chatPane);
		mainBorderPane.setBottom(obStackBottom);
		
		this.chatPane.getSendButton().setOnAction(e-> sendChat() );
		this.chatPane.getClearButton().setOnAction(e-> this.chatPane.clearChat());

		Platform.runLater(()->startGameOverCheck());
	}
	
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
			obServer = new ServerSocket(ConnectFourServer.GAME_PORT);
			
			obSock = obServer.accept();
			
			this.dataInputStream = new DataInputStream(obSock.getInputStream());
			this.dataOutputStream = new DataOutputStream(obSock.getOutputStream());
			
			this.sClient = this.dataInputStream.readUTF();
			this.dataOutputStream.writeUTF(this.sHost);
			//Sets up the player turn order
			estOrder();
			
			this.gameBoard.setTurn(this.bHostTurn);
			
			if(this.bHostTurn)
			{
				myTurn();
			}
			else
			{
				notMyTurn();
			}
			
			while(true)
			{
				sPrompt = this.dataInputStream.readUTF();
				
				//When opponent places a chip, the PLAY signal is sent by them
				if(sPrompt.equals("PLAY"))
				{
					int nCol = this.dataInputStream.readInt();
					this.gameBoard.setOpposPos(nCol, 2);
					this.gameBoard.setTurn(true);
					myTurn();
				}
				
				//When a player hits the send button in the chat window
				//MSG signal gets sent from them
				if(sPrompt.equals("MSG"))
				{
					String sInp = this.dataInputStream.readUTF();
					Platform.runLater(()->this.chatPane.getChatHist().appendText(sInp));
				}
			}
		}
		catch(IOException exp)
		{
			exp.printStackTrace();
		}
		finally
		{
			try
			{
				obSock.close();
				dataInputStream.close();
				dataOutputStream.close();
			}
			catch(IOException exp)
			{
				exp.printStackTrace();
			}
		}
	}
	
	/**
	 * When this player places a chip, it sends the signal
	 * String to the client, along with the chip position used
	 * on the GameBoard, then switches its turn to false
	 * 
	 * @param nCol
	 */
	public void sendMessage(int nCol)
	{
		try
		{
			notMyTurn();
			this.dataOutputStream.writeUTF("PLAY");
			this.dataOutputStream.writeInt(nCol);
			this.gameBoard.setTurn(false);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * When it's this players turn, this updates
	 * text string at the top of the window
	 */
	private void myTurn()
	{
		Platform.runLater( () -> {
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
		Platform.runLater( () -> {
			this.lblPlayer.setText(sClient + "'s Turn");
		lblPlayer.setTextFill(Color.RED);
		ds.setColor(Color.RED);
		});
	}

	/**
	 * When the Send button in chat is activated, this method
	 * sends the MSG string signal word, followed by the text
	 * from the chat field, and appends the chat history
	 */
	public void sendChat()
	{
		try
		{
			String sMessage = this.sHost+": " +this.chatPane.getTextInput().getText()+"\n";
			this.dataOutputStream.writeUTF("MSG");
			this.dataOutputStream.writeUTF(sMessage);
			Platform.runLater(()-> this.chatPane.getChatHist().appendText(sMessage));
			Platform.runLater(()-> this.chatPane.getTextInput().clear());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally {
			Platform.runLater(()-> this.chatPane.getChatHist().appendText(NO_ONE_LISTENING));
		}
	}
	
	/*
	 * Determines player starting order
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
	
	private void startGameOverCheck()
	{
		Thread obThread = new Thread(()->runGameOverCheck());
		obThread.setDaemon(true);
		obThread.start();
	}
	
	/*
	 * Loops through to determine when the gameOver boolean is triggered
	 * If true, transitions to a gameOverScreen after ~5 seconds
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
						count ++;
						if(gameBoard.winner)
						{
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
						if(count == 9)
						{
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
	 * Sets up the end game screen
	 */
	private void gameOverScreen()
	{
		StackPane obEndPane = new StackPane();
		ImageView obGameOver = new ImageView(new Image(IMG_ENDSCREEN));
		Button btnExit = new Button("Exit Game");
		btnExit.setFont(Font.font(COMMON_FONT, FontWeight.BOLD, 15));
		btnExit.setOnAction(e-> System.exit(0));
		btnExit.setPadding(new Insets(25));
		btnExit.setAlignment(Pos.BOTTOM_CENTER);
		HBox obH = new HBox();
		obH.setAlignment(Pos.BOTTOM_CENTER);
		obH.setPadding(new Insets(175));
		obH.getChildren().add(btnExit);
		obEndPane.getChildren().addAll(obGameOver,obH);
		
		mainBorderPane.setCenter(obEndPane);
	}

	public static void main(String[] args) {

		Application.launch(args);
	}
}
