/**
 * COSC 190 Assign 4
 * Logan Olfert CST130
 * Matt Smith CST143
 * 
 */

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/*
 * Represents the "space" on the GameBoard
 * that can be played into
 */

public class Cell
{
	private ImageView iv;
	private int nRow, nCol;
	private int nPlayer;
	private double x,y;
	
	public Cell(ImageView iv, int nRow, int nCol, double x, double y)
	{
		this.iv = iv;
		this.nRow = nRow;
		this.nCol = nCol;
		this.nPlayer = 0;
		this.x = x;
		this.y = y;
	}
	

	public double getX()
	{
		return x;
	}

	public void setX(double x)
	{
		this.x = x;
	}

	public double getY()
	{
		return y;
	}

	public void setY(double y)
	{
		this.y = y;
	}

	public ImageView getIv()
	{
		return iv;
	}

	public void setIv(ImageView iv)
	{
		this.iv = iv;
	}

	public int getRow()
	{
		return nRow;
	}

	public void setRow(int nRow)
	{
		this.nRow = nRow;
	}

	public int getCol()
	{
		return nCol;
	}

	public void setCol(int nCol)
	{
		this.nCol = nCol;
	}

	public int getPlayer()
	{
		return nPlayer;
	}

	public void setPlayer(int nPlayer)
	{
		this.nPlayer = nPlayer;
	}
	
	public Image getImg()
	{
		return this.iv.getImage();
	}
	
	public void setImg(Image obImg)
	{
		this.iv.setImage(obImg);
	}
	
	
	
	
	
}
