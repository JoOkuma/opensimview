package clearcontrol.state.gui.demo;

import clearcontrol.core.concurrent.executors.AsynchronousExecutorFeature;
import clearcontrol.LightSheetMicroscope;
import clearcontrol.component.detection.DetectionArm;
import clearcontrol.component.lightsheet.LightSheet;
import clearcontrol.state.ControlPlaneLayout;
import clearcontrol.state.InterpolatedAcquisitionState;
import clearcontrol.state.gui.AcquisitionStateManagerPanel;
import clearcontrol.state.AcquisitionStateManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Acquisition state manager demo
 *
 * @author royer
 */
public class AcquisitionStateManagerPanelDemo extends Application implements AsynchronousExecutorFeature
{

  @Override
  public void start(Stage stage)
  {

    LightSheetMicroscope lLightSheetMicroscope = new LightSheetMicroscope("Dummy", null, 1, 1);
    DetectionArm lDetectionArm0 = new DetectionArm("D0");
    DetectionArm lDetectionArm1 = new DetectionArm("D1");
    lLightSheetMicroscope.addDevice(0, lDetectionArm0);
    lLightSheetMicroscope.addDevice(1, lDetectionArm1);

    LightSheet lLightSheet0 = new LightSheet("L0", 1, 2);
    LightSheet lLightSheet1 = new LightSheet("L1", 1, 2);
    lLightSheetMicroscope.addDevice(0, lLightSheet0);
    lLightSheetMicroscope.addDevice(1, lLightSheet1);

    final AcquisitionStateManager<InterpolatedAcquisitionState> lAcquisitionStateManager = new AcquisitionStateManager<>(null);
    AcquisitionStateManagerPanel lAcquisitionStateManagerPanel = new AcquisitionStateManagerPanel(lAcquisitionStateManager);

    InterpolatedAcquisitionState lState1 = new InterpolatedAcquisitionState("current", lLightSheetMicroscope);

    lState1.setupControlPlanes(5, -100, 100, ControlPlaneLayout.Linear);

    lAcquisitionStateManager.addState(lState1);

    for (int i = 0; i < 10; i++)
    {
      InterpolatedAcquisitionState lState = new InterpolatedAcquisitionState("State" + i, lLightSheetMicroscope);
      lState.setupControlPlanes(7, -100, 100, ControlPlaneLayout.Circular);

      lAcquisitionStateManager.addState(lState);
    }

    /*
    executeAsynchronously(() -> {
      try
      {
        for (int i = 0; i < 100; i++)
        {
          if (i % 50 == 0)
          {
            InterpolatedAcquisitionState lStateK =
                                                 new InterpolatedAcquisitionState("State2",
                                                                                  2,
                                                                                  4,
                                                                                  1);
            lStateK.setup(-100 + i, 50 + i, 100 + i, 2, 5, 5);
            lAcquisitionStateManager.addState(lStateK);
          }
          lState1.getInterpolationTables().set(LightSheetDOF.DZ, i);
    
          ThreadUtils.sleep(1, TimeUnit.SECONDS);
    
        }
      }
      catch (Throwable e)
      {
        e.printStackTrace();
      }
    });
    /**/

    Scene scene = new Scene(lAcquisitionStateManagerPanel, 1000, 1000);
    stage.setScene(scene);
    stage.setTitle("Interactive2DAcquisitionPanel Demo");

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
