package clearcontrol.scripting.gui;

import clearcontrol.scripting.engine.ScriptingEngine;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

import java.awt.*;

public class ScriptingWindow extends BorderPane
{

  private static final long serialVersionUID = 1L;
  private ScriptingPanel mScriptingPanel;
  private boolean mChanged = false;

  public ScriptingWindow() throws HeadlessException
  {
    this("Scripting Window", null, 60, 80);
  }

  public ScriptingWindow(String pTitle, ScriptingEngine pScriptingEngine, int pNumberOfRows, int pNumberOfCols) throws HeadlessException
  {
    mScriptingPanel = new ScriptingPanel(pTitle, pScriptingEngine,

            pNumberOfRows, pNumberOfCols);
    final SwingNode node = new SwingNode();

    sceneProperty().addListener(new ChangeListener<Scene>()
    {
      @Override
      public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue)
      {
        if (newValue != null)
        {
          node.setContent(mScriptingPanel);
          setCenter(node);
        }
      }
    });

    focusedProperty().addListener((observable, oldValue, newValue) ->
    {
      if (newValue) node.setContent(mScriptingPanel);
    });

    setOnMouseClicked(event ->
    {
      this.requestFocus();
    });
  }

  public void loadLastLoadedScriptFile()
  {
    mScriptingPanel.loadLastLoadedScriptFile();
  }

}
