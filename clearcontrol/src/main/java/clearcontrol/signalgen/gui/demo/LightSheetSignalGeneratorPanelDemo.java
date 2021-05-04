package clearcontrol.signalgen.gui.demo;

import clearcontrol.devices.signalgen.devices.sim.SignalGeneratorSimulatorDevice;
import clearcontrol.signalgen.LightSheetSignalGeneratorDevice;
import clearcontrol.signalgen.gui.LightSheetSignalGeneratorPanel;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Timelapse panel demo
 *
 * @author royer
 */
public class LightSheetSignalGeneratorPanelDemo extends Application
{

  @Override
  public void start(Stage stage)
  {
    HBox root = new HBox();
    root.setAlignment(Pos.CENTER);
    Scene scene = new Scene(root, 600, 400);
    stage.setScene(scene);
    stage.setTitle("Slider Sample");
    // scene.setFill(Color.BLACK);

    SignalGeneratorSimulatorDevice lSignalGeneratorSimulatorDevice = new SignalGeneratorSimulatorDevice();

    LightSheetSignalGeneratorDevice lLightSheetSignalGeneratorDevice = new LightSheetSignalGeneratorDevice(lSignalGeneratorSimulatorDevice, true);

    LightSheetSignalGeneratorPanel lLightSheetSignalGeneratorPanel = new LightSheetSignalGeneratorPanel(lLightSheetSignalGeneratorDevice);

    root.getChildren().add(lLightSheetSignalGeneratorPanel);

    stage.show();
  }

  /**
   * Main
   *
   * @param args NA
   */
  public static void main(String[] args)
  {
    launch(args);
  }
}
