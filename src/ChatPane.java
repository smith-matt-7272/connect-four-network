/**
 * COSC 190 Assign 4
 * Logan Olfert CST130
 * Matt Smith CST143
 * 
 */

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/*
 * The properties and objects for the 
 * chat window and text input fields
 */

public class ChatPane extends VBox
{
	private TextArea txtChatHist;
	private TextField txtChatEntry;
	private Button btnSend;
	private Button btnClear;
	private HBox obHBox;
	
	public ChatPane(String sHandle)
	{
		super();
		buildPane();
	}
	
	private void buildPane()
	{
		this.setPadding(new Insets(5));
		this.txtChatHist = new TextArea();
		this.txtChatHist.setMaxHeight(100);
		this.txtChatHist.setEditable(false);
		this.txtChatHist.setStyle("-fx-opacity: 1;");
		this.txtChatEntry = new TextField();
		this.txtChatEntry.setMinWidth(500);
		this.obHBox = new HBox();
		this.obHBox.setPadding(new Insets(10));
		this.obHBox.setSpacing(15);
		this.obHBox.setAlignment(Pos.CENTER);
		this.btnSend = new Button("Send");
		this.btnSend.setMinWidth(75);

		this.btnClear = new Button("Clear Chat");
		this.btnClear.setMinWidth(75);
		
		this.obHBox.getChildren().addAll(this.txtChatEntry, this.btnSend, this.btnClear);
		this.getChildren().addAll(this.txtChatHist, this.obHBox);
	}
	
	public TextArea getChatHist()
	{
		return this.txtChatHist;
	}
	
	public TextField getTextInput()
	{
		return this.txtChatEntry;
	}
	
	public Button getSendButton()
	{
		return this.btnSend;
	}
	
	public Button getClearButton()
	{
		return this.btnClear;
	}
	
	public void clearChat()
	{
		this.txtChatHist.clear();
	}
}
