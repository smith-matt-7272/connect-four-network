/**
 * COSC 190 Assign 4
 * Logan Olfert CST130
 * Matt Smith CST143
 * 
 */

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class GameBoard extends Pane
{
	//Represents each location on the GameBoard
	private Cell[][] GameCells;
	private ImageView[][] gameIV;
	
	private ImageView imgYellow;
	private ImageView imgYellow2;
	private ImageView imgRed;
	private ImageView imgRed2;
	
	//Height and width of the board
	private final int w = 700;
	private final int h = 600;
	
	private boolean bMyTurn;
	private boolean canPlace;
	private int nPlayer;
	
	public boolean winner = false;
	public boolean gameOver = false;
	
	Consumer<Integer> func;
	
	public GameBoard(Consumer<Integer> func)
	{
		super();
		this.func=func;
		ImageView obRect = new ImageView("file:images/board.png");
		obRect.setFitWidth(w);
		obRect.setFitHeight(h);
		this.getChildren().add(obRect);
		
		estBoard();
		
		for(int i=0; i<7;i++)
		{
			for(int j=0; j<6;j++)
			{
				this.getChildren().add(gameIV[i][j]);
				gameIV[i][j].setX(i*100);
				gameIV[i][j].setY(j*100);
			}
		}
	}

	private void estBoard()
	{
		this.gameIV = new ImageView[7][6];
		this.GameCells = new Cell[7][6];
		
		
		//imgRed and imgYellow have a chip image with transparent backgrounds
		this.imgYellow = new ImageView("file:images/yellowchip.png");
		this.imgRed = new ImageView("file:images/redchip.png");
		
		//imgRed2 and imgYellow2 have a chip image with a black background at 1% opacity
		//which makes the players able to click anywhere in the 100x100 square as opposed
		//to just being able to click on the circle
		this.imgYellow2 = new ImageView("file:images/yellowchip2.png");
		this.imgRed2 = new ImageView("file:images/redchip2.png");
		
		for(int i=0; i<this.gameIV.length; i++)
		{
			for(int j=0; j<this.gameIV[0].length; j++)
			{
				//nochip.png is basically just a blank image
				ImageView current = new ImageView("file:images/nochip.png");
				current.setOpacity(0.01);
				Cell obCell = new Cell(current, i, j, (i*100), (j*100));
				obCell.getIv().setFitWidth(100);
				obCell.getIv().setFitHeight(100);
				
				
				obCell.getIv().setOnMouseClicked( e ->{
					
					if(this.bMyTurn)
					{
						this.canPlace = false;
						
						//checking if the player can place a piece
						setPos(obCell.getRow(),this.nPlayer);
						if(canPlace)
						{
							this.func.accept(obCell.getRow());
							setTurn(false);
						}
					}
				});
				
				this.GameCells[i][j] = obCell;
				this.gameIV[i][j] = obCell.getIv();
			}
		}
	}

	public void setTurn(boolean bVal)
	{
		this.bMyTurn = bVal;
	}

	public void setPos(int nCol, int nPlayer)
	{
		
		for(int i=this.GameCells[nCol].length-1; i>=0; i--)
		{
			//if a piece has not been placed in the current cell
			if(GameCells[nCol][i].getPlayer() == 0 && !gameOver)
			{
				//Setting the image to either yellow or red based on nPlayer
				GameCells[nCol][i].setIv(nPlayer == 1 ? this.imgYellow : this.imgRed);
				GameCells[nCol][i].getIv().setOpacity(1);
				GameCells[nCol][i].setPlayer(nPlayer);
				
				gameIV[nCol][i].setImage(nPlayer == 1 ? this.imgYellow.getImage() : this.imgRed.getImage());
				gameIV[nCol][i].setOpacity(1);
				
				//Checking for win
				if(checkForWin(nPlayer))
				{
					winner = true;
					gameOver = true;
				}
				canPlace = true;
				startDrop(GameCells[nCol][i], gameIV[nCol][i], (GameCells[nCol][i].getCol()*100));
				break;
			}
		}
	}
	
	public void setOpposPos(int nCol, int nPlayer)
	{
		for(int i=this.GameCells[nCol].length-1; i>=0; i--)
		{
			if(GameCells[nCol][i].getPlayer() == 0 && !gameOver)
			{
				//Setting the image to either yellow or red based on nPlayer
				GameCells[nCol][i].setIv(nPlayer == 1 ? this.imgYellow : this.imgRed);
				GameCells[nCol][i].getIv().setOpacity(1);
				GameCells[nCol][i].setPlayer(nPlayer);
				
				gameIV[nCol][i].setImage(nPlayer == 1 ? this.imgYellow.getImage() : this.imgRed.getImage());
				gameIV[nCol][i].setOpacity(1);
				
				//Checking for opponent win
				if(checkForWin(nPlayer))
				{
					winner = false;
					gameOver = true;
				}
				canPlace = true;
				startDrop(GameCells[nCol][i], gameIV[nCol][i], (GameCells[nCol][i].getCol()*100));
				break;
			}
		}
	}
	
	//Starting the dropping animation of the pieces
	private void startDrop(Cell obCell, ImageView obView, int nVal)
	{
		Thread obThread = new Thread( () -> runDrop(obCell, obView, nVal));
		obThread.setDaemon(true);
		obThread.start();
	}
	
	private void runDrop(Cell obCell, ImageView obView, int nVal)
	{
		obCell.setY(0);
		try
		{
			//while the cell can still go down
			while(obCell.getY() <= nVal)
			{
				Platform.runLater( () -> {
					//Setting the Y position of the piece
					obCell.setY(obCell.getY()+10);
					obCell.getIv().setY(obCell.getY()+10);
					obView.setY(obCell.getY()+10);
				});
				Thread.sleep(10);
			}
			//sleeping for 50ms to "re-draw" the image
			//this stops images from being displayed in the wrong place
			Thread.sleep(50);
			obCell.setY(nVal);
			obCell.getIv().setY(nVal);
			obView.setY(nVal);

			
		}
		catch(InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}
	
	private boolean checkForWin(int nPlayer)
	{
		
		//checking for Vertical win
		for(int i=0; i<GameCells.length; i++)
		{
			for(int j=0; j<GameCells[0].length-3; j++)
			{
				if(GameCells[i][j].getPlayer() == nPlayer && GameCells[i][j+1].getPlayer() == nPlayer && GameCells[i][j+2].getPlayer() == nPlayer && GameCells[i][j+3].getPlayer() == nPlayer)
				{
					winHelper(gameIV[i][j],gameIV[i][j+1],gameIV[i][j+2],gameIV[i][j+3],nPlayer);
					return true;
				}
			}
		}
		
		//checking for Horizontal win
		for(int i=0; i<GameCells.length-3; i++)
		{
			for(int j=0; j<GameCells[0].length; j++)
			{
				if(GameCells[i][j].getPlayer() == nPlayer && GameCells[i+1][j].getPlayer() == nPlayer && GameCells[i+2][j].getPlayer() == nPlayer && GameCells[i+3][j].getPlayer() == nPlayer)
				{
					winHelper(gameIV[i][j],gameIV[i+1][j],gameIV[i+2][j],gameIV[i+3][j],nPlayer);
					return true;
				}
			}
		}

		//checking for Diagonal Down win
		for(int i=0; i<GameCells.length-3; i++)
		{
			for(int j=0; j<GameCells[0].length-3; j++)
			{
				if(GameCells[i][j].getPlayer() == nPlayer && GameCells[i+1][j+1].getPlayer() == nPlayer && GameCells[i+2][j+2].getPlayer() == nPlayer && GameCells[i+3][j+3].getPlayer() == nPlayer)
				{
					winHelper(gameIV[i][j],gameIV[i+1][j+1],gameIV[i+2][j+2],gameIV[i+3][j+3],nPlayer);
					return true;
				}
			}
		}
		
		//checking for Diagonal Up win
		for(int i=0; i<GameCells.length-3; i++)
		{
			for(int j=GameCells[0].length-1; j>=3; j--)
			{
				if(GameCells[i][j].getPlayer() == nPlayer && GameCells[i+1][j-1].getPlayer() == nPlayer && GameCells[i+2][j-2].getPlayer() == nPlayer && GameCells[i+3][j-3].getPlayer() == nPlayer)
				{
					winHelper(gameIV[i][j],gameIV[i+1][j-1],gameIV[i+2][j-2],gameIV[i+3][j-3],nPlayer);
					return true;
				}
			}
		}
		return false;
		
	}
	
	private void winHelper(ImageView iv1, ImageView iv2, ImageView iv3, ImageView iv4, int nP)
	{
		//Takes the current cell and applies an effect and changes the image
		DropShadow ds = new DropShadow();
		ds.setColor(nP == 1 ? Color.YELLOW:Color.RED);
		ds.setSpread(10);
		
		ImageView[] ivs = {iv1,iv2,iv3,iv4};
		
		for(ImageView iv:ivs)
		{
			iv.setImage(nP == 1 ? this.imgYellow2.getImage() : this.imgRed2.getImage());
			iv.setOpacity(1);
			iv.setEffect(ds);
		}
	}
	
	public void setPlayer(int n)
	{
		this.nPlayer = n;
	}
}
