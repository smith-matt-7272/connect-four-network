import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameHost extends Player {

    private static final String TITLE = "Connect Four Hyper Ultimate Server";

    @Override
    public void runGame() {
        ServerSocket obServer = null;
        Socket obSock = null;
        String sPrompt;

        try
        {
            obServer = new ServerSocket(ConnectFourServer.GAME_PORT);					//Setup the server socket
            obSock = obServer.accept();													//Wait for client to accept connection

            super.dataInputStream = new DataInputStream(obSock.getInputStream());		//Set the IO streams
            this.dataOutputStream = new DataOutputStream(obSock.getOutputStream());

            this.sClient = this.dataInputStream.readUTF();								//Read in the clients name sent at time of connection
            this.dataOutputStream.writeUTF(this.playerName);									//Send server's player name to client
            estOrder();																	//Sets up the player turn order

            this.gameBoard.setTurn(this.bTurn);										//Set the gameboards aesthetics per turn order

            if(this.bTurn)					//Set the styling of the gameboard depending on whose turn it is
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
     * Establish player order at start of game
     * @throws IOException - If any issues on output
     */
    private void estOrder() throws IOException
    {
        int nVal = (int)(Math.random()*100);
        if(nVal > 49)
        {
            this.bTurn = true;
        }

        this.dataOutputStream.writeBoolean(!bTurn);
    }

    @Override
    public int getPlayerNum() {
        return 1;
    }

    @Override
    public Image getImg_icon() {
        return new Image(IMG_YELLOWCHIP);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    public static void main(String[] args) {
        Application.launch(args);}
}
