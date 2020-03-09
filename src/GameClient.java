import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class GameClient extends Player {

    private static final String TITLE = "Connect Four Hyper Ultimate Client";

    @Override
    public void runGame() {
        String sPrompt;
        Socket obSock = null;
        try
        {
            obSock = new Socket("localhost",ConnectFourServer.GAME_PORT);

            this.dataOutputStream = new DataOutputStream(obSock.getOutputStream());
            this.dataInputStream = new DataInputStream(obSock.getInputStream());

            this.dataOutputStream.writeUTF(this.playerName);
            this.sHost = this.dataInputStream.readUTF();
            this.bTurn = this.dataInputStream.readBoolean();

            this.gameBoard.setTurn(bTurn);

            if(this.bTurn)
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
                    this.gameBoard.setOpposPos(nCol, 1);
                    this.gameBoard.setTurn(true);
                    myTurn();
                }

                //When opponent hits the send button in the chat window
                //MSG signal sent from them
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
    }

    @Override
    public int getPlayerNum() {
        return 2;
    }

    @Override
    public Image getImg_icon() {
        return new Image(IMG_REDCHIP);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    public static void main(String[] args) {
        Application.launch(args);}
}
