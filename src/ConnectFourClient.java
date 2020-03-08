/*
 * COSC 190 Assign 4
 * Logan Olfert CST130
 * Matt Smith CST143
 * 
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

public class ConnectFourClient extends Application {
	
	//Window and Graphic Attributes
	private GameBoard obBoard;
	private BorderPane obBPane;
	private HBox hb;
	private Label lblPlayer;
	private DropShadow ds;
	private ChatPane obChat;
	
	private DataInputStream obIn;
	private DataOutputStream obOut;
	private Socket obSock = null;
	
	private String sClient = "Client";
	private String sHost;
	
	//Consumer used to communicate between board/server
	private Consumer<Integer> func;
	
	private boolean bMyTurn;
	
	//Helper to delay to End Game Screen
	private int count = 0;

	@Override
	public void start(Stage obStage) throws Exception {

		this.func = ((x)->sendMessage(x));
		this.obBPane = new BorderPane();
		
		//Creating a drop shadow effect to display on the label at the top of the screen
		ds = new DropShadow();
		ds.setColor(Color.WHITE);
		ds.setRadius(5);
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		
		//Starting background image
		ImageView obStart = new ImageView(new Image("file:images/startScreen.png"));
		obStart.setFitHeight(850);
		StackPane obStackStart = new StackPane();

		hb = new HBox();
		hb.setAlignment(Pos.CENTER);
		lblPlayer = new Label("Not connected to Server");
		lblPlayer.setFont(Font.font("Century Gothic", FontWeight.BOLD,30));
		lblPlayer.setEffect(ds);
		hb.getChildren().add(lblPlayer);
		
		HBox obOpen = new HBox();
		obOpen.setMinHeight(100);
		obOpen.setAlignment(Pos.BOTTOM_CENTER);
		obOpen.setPadding(new Insets(75));
		obOpen.setSpacing(20);
		
		Label lblName = new Label("Name: ");
		lblName.setFont(Font.font("Century Gothic", FontWeight.BOLD, 20));
		TextField txtName = new TextField();
		Button btnStart = new Button("Start");
		btnStart.setFont(Font.font("Century Gothic", FontWeight.BOLD, 13.5));
		btnStart.setDisable(true);
		
		//Ensures names longer than 2 characters are entered
		txtName.setOnKeyReleased(e-> {
			if(txtName.getText().length() > 2)
			{
				btnStart.setDisable(false);
			}
			else
			{
				btnStart.setDisable(true);
			}
			
		});
		
		//Once a name is entered, creates GameBoard
		btnStart.setOnAction(e-> {
			this.sClient = txtName.getText();
			makeGame();
			makeConnection();
		});
		obOpen.getChildren().addAll(lblName,txtName,btnStart);
		
		obStackStart.getChildren().addAll(obStart,obOpen);
		
		this.obBPane.setBottom(obStackStart);

		Scene obScene = new Scene(obBPane,700,850);

		obStage.setScene(obScene);
		obStage.setTitle("Connect Four Hyper Ultimate Client");
		obStage.setResizable(false);
		obStage.show();
		obStage.getIcons().add(new Image("file:images/redchip.png"));
	}
		
	/*
	 * Sets up the GameBoard and the Chat Window
	 * Sets up starting game conditions
	 * Begins the GameOverCheck which determines when win
	 * conditions are achieved
	 */
	public void makeGame()
	{
		obBoard = new GameBoard(this.func);
		this.obBoard.setPlayer(2);
		this.obBoard.gameOver = false;
		this.obBoard.winner = false;

		ImageView obView = new ImageView(new Image("file:images/top.png"));
		StackPane obStackTop = new StackPane();
		obStackTop.getChildren().addAll(obView,hb);
		this.obBPane.setCenter(obBoard);
		this.obChat = new ChatPane(this.sClient);
		obBPane.setTop(obStackTop);
		StackPane obStackBottom = new StackPane();
		ImageView obViewBottom = new ImageView(new Image("file:images/bottom.png"));
		obStackBottom.getChildren().addAll(obViewBottom,this.obChat);
		obBPane.setBottom(obStackBottom);
		
		this.obChat.getSendButton().setOnAction(e-> sendChat() );
		this.obChat.getClearButton().setOnAction(e-> this.obChat.clearChat());
		
		//Checks if there is a winning condition
		Platform.runLater(()->startGameOverCheck());
	}
		
	public void makeConnection()
	{
		Thread obThread = new Thread(()->runConnection());
		obThread.setDaemon(true);
		obThread.start();
	}
	
	/**
	 * Handles connection with client and communication
	 * protocol through String signal words
	 */
	public void runConnection()
	{
		String sPrompt;
		try
		{
			this.obSock = new Socket("localhost",ConnectFourServer.GAME_PORT);
	
			this.obOut = new DataOutputStream(obSock.getOutputStream());
			this.obIn = new DataInputStream(obSock.getInputStream());
			
			this.obOut.writeUTF(this.sClient);
			this.sHost = this.obIn.readUTF();
			this.bMyTurn = this.obIn.readBoolean();
			
			this.obBoard.setTurn(bMyTurn);

			if(this.bMyTurn)
			{
				myTurn();
			}
			else
			{
				notMyTurn();
			}
			
			while(true)
			{
				sPrompt = this.obIn.readUTF();
				
				//When opponent places a chip, the PLAY signal is sent by them
				if(sPrompt.equals("PLAY"))
				{
					int nCol = this.obIn.readInt();
					//System.out.printf("Received %d\n", nCol);
					this.obBoard.setOpposPos(nCol, 1);
					this.obBoard.setTurn(true);
					myTurn();
				}
				
				//When opponent hits the send button in the chat window
				//MSG signal sent from them
				if(sPrompt.equals("MSG"))
				{
					String sInp = this.obIn.readUTF();
					Platform.runLater(()->this.obChat.getChatHist().appendText(sInp));
				}
			}
		}
		catch(IOException exp)
		{
			
		}
	}
	
	/**
	 * When this player places a chip, it sends the signal
	 * String to the server, along with the chip position used
	 * on the GameBoard, then switches its turn to false
	 * 
	 * @param nCol
	 */
	public void sendMessage(int nCol)
	{
		try
		{
			notMyTurn();
			this.obOut.writeUTF("PLAY");
			this.obOut.writeInt(nCol);
			this.obBoard.setTurn(false);
		}
		catch(IOException e)
		{
			
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
	 * When it's the other players turn, updates
	 * text string at the top of the window
	 */
	private void notMyTurn()
	{
		Platform.runLater( () -> {
			this.lblPlayer.setText(sHost + "'s Turn");
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
			String sMessage = this.sClient+": " +this.obChat.getTextInput().getText()+"\n";
			this.obOut.writeUTF("MSG");
			this.obOut.writeUTF(sMessage);
			Platform.runLater(()-> this.obChat.getChatHist().appendText(sMessage));
			Platform.runLater(()-> this.obChat.getTextInput().clear());
		}
		catch(IOException e)
		{
			
		}
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
					if(obBoard.gameOver)
					{
						count++;
						if(obBoard.winner)
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
							Platform.runLater(()-> gameOverScreen());
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
		ImageView obGameOver = new ImageView(new Image("file:images/endScreen.png"));
		Button btnExit = new Button("Exit Game");
		btnExit.setFont(Font.font("Century Gothic", FontWeight.BOLD, 15));
		btnExit.setOnAction(e-> System.exit(0));
		btnExit.setPadding(new Insets(25));
		btnExit.setAlignment(Pos.BOTTOM_CENTER);
		HBox obH = new HBox();
		obH.setAlignment(Pos.BOTTOM_CENTER);
		obH.setPadding(new Insets(175));
		obH.getChildren().add(btnExit);
		obEndPane.getChildren().addAll(obGameOver,obH);
		
		obBPane.setCenter(obEndPane);
	}

	public static void main(String[] args) 
	{
		Application.launch(args);

	}

}
