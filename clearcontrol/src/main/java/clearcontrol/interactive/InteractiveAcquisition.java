package clearcontrol.interactive;

import clearcontrol.core.device.VirtualDevice;
import clearcontrol.core.device.change.ChangeListener;
import clearcontrol.core.device.task.PeriodicLoopTaskDevice;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.VariableSetListener;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.devices.cameras.StackCameraDeviceInterface;
import clearcontrol.LightSheetMicroscope;
import clearcontrol.LightSheetMicroscopeInterface;
import clearcontrol.LightSheetMicroscopeQueue;
import clearcontrol.component.detection.DetectionArmInterface;
import clearcontrol.component.lightsheet.LightSheet;
import clearcontrol.component.lightsheet.LightSheetInterface;
import clearcontrol.stack.MetaDataView;
import clearcontrol.stack.MetaDataViewFlags;
import clearcontrol.state.InterpolatedAcquisitionState;
import clearcontrol.stack.metadata.MetaDataAcquisitionType;
import clearcontrol.state.AcquisitionStateInterface;
import clearcontrol.state.AcquisitionStateManager;
import clearcontrol.state.AcquisitionType;
import clearcontrol.stack.metadata.StackMetaData;

import java.util.concurrent.TimeUnit;

/**
 * Interactive acquisition for lightseet microscope
 *
 * @author royer
 */
public class InteractiveAcquisition extends PeriodicLoopTaskDevice implements LoggingFeature
{

  private static final int cRecyclerMinimumNumberOfAvailableStacks = 60;
  private static final int cRecyclerMaximumNumberOfAvailableStacks = 60;
  private static final int cRecyclerMaximumNumberOfLiveStacks = 60;

  private final LightSheetMicroscopeInterface mLightSheetMicroscope;
  private final AcquisitionStateManager<InterpolatedAcquisitionState> mAcquisitionStateManager;

  private volatile InteractiveAcquisitionModes mCurrentAcquisitionMode = InteractiveAcquisitionModes.None;

  private final BoundedVariable<Double> mExposureVariableInSeconds;
  private final Variable<Boolean> mTriggerOnChangeVariable, mUseCurrentAcquisitionStateVariable;
  private final Variable<Boolean> mSyncDetectionArmsVariable, mSyncLightSheetsVariable, mSyncLightSheetsAndDetectionArmsVariable;
  private final BoundedVariable<Number> m2DAcquisitionZVariable;

  private final Variable<Long> mAcquisitionCounterVariable;

  private volatile boolean mUpdate = true;

  private ChangeListener<VirtualDevice> mMicroscopeChangeListener;
  private ChangeListener<AcquisitionStateInterface<LightSheetMicroscopeInterface, LightSheetMicroscopeQueue>> mAcquisitionStateChangeListener;
  private LightSheetMicroscopeQueue mQueue;

  /**
   * Instantiates an interactive acquisition for lightsheet microscope
   *
   * @param pDeviceName           device name
   * @param pLightSheetMicroscope lightsheet microscope
   */
  @SuppressWarnings("unchecked")
  public InteractiveAcquisition(String pDeviceName, LightSheetMicroscope pLightSheetMicroscope)
  {
    super(pDeviceName, 1, TimeUnit.SECONDS);
    mLightSheetMicroscope = pLightSheetMicroscope;
    mAcquisitionStateManager = (AcquisitionStateManager<InterpolatedAcquisitionState>) pLightSheetMicroscope.getAcquisitionStateManager();

    @SuppressWarnings("rawtypes") VariableSetListener lListener = (o, n) ->
    {
      if (o != n || (o == null && n != null) || !o.equals(n)) mUpdate = true;
    };

    mExposureVariableInSeconds = new BoundedVariable<Double>(pDeviceName + "Exposure", 0.0, 0.0, Double.POSITIVE_INFINITY, 0.0);

    mTriggerOnChangeVariable = new Variable<Boolean>(pDeviceName + "TriggerOnChange", false);

    mUseCurrentAcquisitionStateVariable = new Variable<Boolean>(pDeviceName + "UseCurrentAcquisitionState", false);

    Variable<Number> lMinVariable = mLightSheetMicroscope.getDevice(DetectionArmInterface.class, 0).getZVariable().getMinVariable();
    Variable<Number> lMaxVariable = mLightSheetMicroscope.getDevice(DetectionArmInterface.class, 0).getZVariable().getMaxVariable();

    mSyncDetectionArmsVariable = new Variable<Boolean>("Sync detection arms", false);

    mSyncLightSheetsVariable = new Variable<Boolean>("Sync lightsheets", false);

    mSyncLightSheetsAndDetectionArmsVariable = new Variable<Boolean>("Sync lightsheets and detection arms", false);

    m2DAcquisitionZVariable = new BoundedVariable<Number>("2DAcquisitionZ", 0.0, lMinVariable.get(), lMaxVariable.get());

    mAcquisitionCounterVariable = new Variable<Long>("AcquisitionCounter", 0L);

    lMinVariable.sendUpdatesTo(m2DAcquisitionZVariable.getMinVariable());
    lMaxVariable.sendUpdatesTo(m2DAcquisitionZVariable.getMaxVariable());

    getExposureVariable().addSetListener(lListener);
    getTriggerOnChangeVariable().addSetListener(lListener);
    getLoopPeriodVariable().addSetListener(lListener);
    get2DAcquisitionZVariable().addSetListener(lListener);
    getSyncDetectionArmsVariable().addSetListener(lListener);
    getSyncLightSheetsAndDetectionArmsVariable().addSetListener(lListener);

    getLoopPeriodVariable().set(1.0);
    getExposureVariable().set(0.010);

    mMicroscopeChangeListener = (o) ->
    {
      // info("Received request to update queue from:" + o.toString());

      mUpdate = true;
    };
    mAcquisitionStateChangeListener = (o) ->
    {
      // info("Received request to update queue from:" + o.toString());
      mUpdate = true;
    };

    getSyncDetectionArmsVariable().addSetListener((o, n) ->
    {
      if (o != n) syncDetectionArms(n);
    });

    getSyncLightSheetsVariable().addSetListener((o, n) ->
    {
      if (o != n) syncLightSheets(n);
    });

    getSyncLightSheetsAndDetectionArmsVariable().addSetListener((o, n) ->
    {
      if (o != n) syncLightSheetsAndDetectionArms(n);
    });
  }

  private void syncDetectionArms(Boolean pSync)
  {

    DetectionArmInterface lFirstDetectionArm = getLightSheetMicroscope().getDetectionArm(0);

    int lNumberOfDetectionArms = getLightSheetMicroscope().getNumberOfDetectionArms();
    for (int d = 0; d < lNumberOfDetectionArms; d++)
      if (d != 0)
      {
        DetectionArmInterface lDetectionArm = getLightSheetMicroscope().getDetectionArm(d);
        if (pSync)
        {
          lDetectionArm.getZVariable().syncWith(lFirstDetectionArm.getZVariable());
          lDetectionArm.getZVariable().set(lFirstDetectionArm.getZVariable().get());
        } else lDetectionArm.getZVariable().doNotSyncWith(lFirstDetectionArm.getZVariable());
      }

  }

  private void syncLightSheets(Boolean pSync)
  {

    LightSheetInterface lFirstLightSheet = getLightSheetMicroscope().getLightSheet(0);

    int lNumberofLaserLines = getLightSheetMicroscope().getNumberOfLaserLines();

    int lNumberOfLightSheets = getLightSheetMicroscope().getNumberOfLightSheets();
    for (int l = 0; l < lNumberOfLightSheets; l++)
      if (l != 0)
      {
        LightSheetInterface lLightSheet = getLightSheetMicroscope().getLightSheet(l);

        if (pSync)
        {
          lLightSheet.getXVariable().syncWith(lFirstLightSheet.getXVariable());
          lLightSheet.getXVariable().set(lFirstLightSheet.getXVariable().get());

          lLightSheet.getYVariable().syncWith(lFirstLightSheet.getYVariable());
          lLightSheet.getYVariable().set(lFirstLightSheet.getYVariable().get());

          lLightSheet.getZVariable().syncWith(lFirstLightSheet.getZVariable());
          lLightSheet.getZVariable().set(lFirstLightSheet.getZVariable().get());

          lLightSheet.getAlphaInDegreesVariable().syncWith(lFirstLightSheet.getAlphaInDegreesVariable());
          lLightSheet.getAlphaInDegreesVariable().set(lFirstLightSheet.getAlphaInDegreesVariable().get());

          lLightSheet.getBetaInDegreesVariable().syncWith(lFirstLightSheet.getBetaInDegreesVariable());
          lLightSheet.getBetaInDegreesVariable().set(lFirstLightSheet.getBetaInDegreesVariable().get());

          lLightSheet.getHeightVariable().syncWith(lFirstLightSheet.getHeightVariable());
          lLightSheet.getHeightVariable().set(lFirstLightSheet.getHeightVariable().get());

          lLightSheet.getWidthVariable().syncWith(lFirstLightSheet.getWidthVariable());
          lLightSheet.getWidthVariable().set(lFirstLightSheet.getWidthVariable().get());

          lLightSheet.getPowerVariable().syncWith(lFirstLightSheet.getPowerVariable());
          lLightSheet.getPowerVariable().set(lFirstLightSheet.getPowerVariable().get());

          for (int la = 0; la < lNumberofLaserLines; la++)
          {
            lLightSheet.getLaserOnOffArrayVariable(la).syncWith(lFirstLightSheet.getLaserOnOffArrayVariable(la));
            lLightSheet.getLaserOnOffArrayVariable(la).set(lFirstLightSheet.getLaserOnOffArrayVariable(la).get());
          }

        } else
        {
          lLightSheet.getXVariable().doNotSyncWith(lFirstLightSheet.getXVariable());

          lLightSheet.getYVariable().doNotSyncWith(lFirstLightSheet.getYVariable());

          lLightSheet.getZVariable().doNotSyncWith(lFirstLightSheet.getZVariable());

          lLightSheet.getAlphaInDegreesVariable().doNotSyncWith(lFirstLightSheet.getAlphaInDegreesVariable());

          lLightSheet.getBetaInDegreesVariable().doNotSyncWith(lFirstLightSheet.getBetaInDegreesVariable());

          lLightSheet.getHeightVariable().doNotSyncWith(lFirstLightSheet.getHeightVariable());

          lLightSheet.getWidthVariable().doNotSyncWith(lFirstLightSheet.getWidthVariable());

          lLightSheet.getPowerVariable().doNotSyncWith(lFirstLightSheet.getPowerVariable());

          for (int la = 0; la < lNumberofLaserLines; la++)
            lLightSheet.getLaserOnOffArrayVariable(la).doNotSyncWith(lFirstLightSheet.getLaserOnOffArrayVariable(la));

        }
      }

  }

  private void syncLightSheetsAndDetectionArms(Boolean pSync)
  {
    int lNumberOfLightSheets = getLightSheetMicroscope().getNumberOfLightSheets();
    int lNumberOfDetectionArms = getLightSheetMicroscope().getNumberOfDetectionArms();

    DetectionArmInterface lFirstDetectionArm = getLightSheetMicroscope().getDetectionArm(0);

    for (int d = 0; d < lNumberOfDetectionArms; d++)
      for (int l = 0; l < lNumberOfLightSheets; l++)
      {
        DetectionArmInterface lDetectionArm = getLightSheetMicroscope().getDetectionArm(d);
        LightSheetInterface lLightSheet = getLightSheetMicroscope().getLightSheet(l);

        if (pSync)
        {
          lDetectionArm.getZVariable().syncWith(lLightSheet.getZVariable());
          lDetectionArm.getZVariable().set(lFirstDetectionArm.getZVariable().get());
          lLightSheet.getZVariable().set(lFirstDetectionArm.getZVariable().get());
        } else lDetectionArm.getZVariable().doNotSyncWith(lLightSheet.getZVariable());
      }

  }

  @Override
  public boolean open()
  {
    getLightSheetMicroscope().addChangeListener(mMicroscopeChangeListener);
    return super.open();
  }

  @Override
  public boolean close()
  {
    getLightSheetMicroscope().removeChangeListener(mMicroscopeChangeListener);
    return super.close();
  }

  @Override
  public void run()
  {
    try
    {
      super.run();
    } finally
    {
      getLightSheetMicroscope().getCurrentTask().set(null);
    }
  }

  @Override
  public boolean loop()
  {
    try
    {
      // info("begin of loop");
      final boolean lCachedUpdate = mUpdate;

      InterpolatedAcquisitionState lCurrentState = (InterpolatedAcquisitionState) mAcquisitionStateManager.getCurrentState();

      if (getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition3D || getUseCurrentAcquisitionStateVariable().get())
      {
        if (!lCurrentState.isChangeListener(mAcquisitionStateChangeListener))
          lCurrentState.addChangeListener(mAcquisitionStateChangeListener);
      }

      if (lCachedUpdate || mQueue == null || mQueue.getQueueLength() == 0)
      {

        double lCurrentZ = get2DAcquisitionZVariable().get().doubleValue();

        if (getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition2D)
        {
          // info("Building 2D Acquisition queue");
          if (getUseCurrentAcquisitionStateVariable().get())
          {
            // info("Building 2D Acquisition queue using the current acquisition
            // state");

            mQueue = getLightSheetMicroscope().requestQueue();

            mQueue.clearQueue();

            mQueue.addVoxelDimMetaData(getLightSheetMicroscope(), 1);

            lCurrentState.applyAcquisitionStateAtZ(mQueue, lCurrentZ);
            mQueue.addCurrentStateToQueue();

            mQueue.setFinalisationTime(0.00005);
            mQueue.finalizeQueue();

          } else
          {
            getLightSheetMicroscope().useRecycler("2DInteractive", cRecyclerMinimumNumberOfAvailableStacks, cRecyclerMaximumNumberOfAvailableStacks, cRecyclerMaximumNumberOfLiveStacks);

            mQueue = getLightSheetMicroscope().requestQueue();
            mQueue.clearQueue();
            mQueue.addVoxelDimMetaData(getLightSheetMicroscope(), 1);
            mQueue.setC(true);
            mQueue.setExp(mExposureVariableInSeconds.get().doubleValue());

            mQueue.addCurrentStateToQueue();
            // mQueue.setTransitionTime(0.005);
            mQueue.setFinalisationTime(0.00005);
            mQueue.finalizeQueue();

          }
        } else if (getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition3D)
        {
          // info("Building Acquisition3D queue");
          getLightSheetMicroscope().useRecycler("3DInteractive", cRecyclerMinimumNumberOfAvailableStacks, cRecyclerMaximumNumberOfAvailableStacks, cRecyclerMaximumNumberOfLiveStacks);

          if (lCurrentState != null)
          {
            lCurrentState.updateQueue(true);
            mQueue = lCurrentState.getQueue();
          }
        }

        if (lCachedUpdate) mUpdate = false;
      }

      if (mQueue.getQueueLength() == 0)
      {
        // this leads to a call to stop() which stops the loop
        warning("Queue empty stopping interactive acquisition loop");
        return false;
      }

      // Setting meta-data:
      for (int c = 0; c < getNumberOfCameras(); c++)
      {
        StackMetaData lMetaData = mQueue.getCameraDeviceQueue(c).getMetaDataVariable().get();

        lMetaData.addEntry(MetaDataView.Camera, c);

        for (int l = 0; l < getNumberOfLightsSheets(); l++)
          lMetaData.addEntry(MetaDataViewFlags.getLightSheet(l), mQueue.getI(l));

        lMetaData.addEntry(MetaDataAcquisitionType.AcquisitionType, AcquisitionType.Interactive);
      }

      if (getCurrentAcquisitionMode() != InteractiveAcquisitionModes.None)
      {
        if (getTriggerOnChangeVariable().get() && !lCachedUpdate) return true;

        if (getUseCurrentAcquisitionStateVariable().get() || getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition3D)
          lCurrentState.prepareAcquisition(100, TimeUnit.SECONDS);

        // info("play queue");
        // play queue
        // info("Playing LightSheetMicroscope Queue...");
        boolean lSuccess = getLightSheetMicroscope().playQueueAndWaitForStacks(mQueue, 30, TimeUnit.SECONDS);

        if (lSuccess)
        {
          // info("play queue success");
          mAcquisitionCounterVariable.increment();
        }

        // info("... done waiting!");
      }

    } catch (Throwable e)
    {
      e.printStackTrace();
    }

    // info("end of loop");

    return true;
  }

  /**
   * Starts 2D acquisition
   */
  public void start2DAcquisition()
  {
    if (getLightSheetMicroscope().getCurrentTask().get() != null)
    {
      warning("Another task (%s) is already running, please stop it first.", getLightSheetMicroscope().getCurrentTask());
      return;
    }

    getLightSheetMicroscope().getCurrentTask().set(this);

    if (getIsRunningVariable().get() && getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition3D)
    {
      warning("Please stop 3D acquisition first!");
      return;
    }
    if (getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition2D)
    {
      warning("Already doing 2D acquisition!");
      return;
    }

    info("Starting 2D Acquisition...");
    setCurrentAcquisitionMode(InteractiveAcquisitionModes.Acquisition2D);
    mAcquisitionCounterVariable.set(0L);
    mUpdate = true;
    startTask();
  }

  /**
   * Starts 3D acquisition
   */
  public void start3DAcquisition()
  {
    if (getLightSheetMicroscope().getCurrentTask().get() != null)
    {
      warning("Another task (%s) is already running, please stop it first.", getLightSheetMicroscope().getCurrentTask());
      return;
    }

    if (getIsRunningVariable().get() && getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition2D)
    {
      warning("Please stop 2D acquisition first!");
      return;
    }
    if (getCurrentAcquisitionMode() == InteractiveAcquisitionModes.Acquisition3D)
    {
      warning("Already doing 3D acquisition!");
      return;
    }

    info("Starting 3D Acquisition...");
    setCurrentAcquisitionMode(InteractiveAcquisitionModes.Acquisition3D);
    mAcquisitionCounterVariable.set(0L);
    mUpdate = true;

    startTask();
  }

  /**
   * Stops acquisition
   */
  public void stopAcquisition()
  {
    info("Stopping Acquisition...");
    setCurrentAcquisitionMode(InteractiveAcquisitionModes.None);
    stopTask();

  }

  /**
   * Returns the exposure variable
   *
   * @return exposure variable (unit: seconds)
   */
  public BoundedVariable<Double> getExposureVariable()
  {
    return mExposureVariableInSeconds;
  }

  /**
   * Returns the trigger-on-change variable
   *
   * @return trigger-on-change variable
   */
  public Variable<Boolean> getTriggerOnChangeVariable()
  {
    return mTriggerOnChangeVariable;
  }

  /**
   * Returns the use-current-acquisition-state variable
   *
   * @return use-current-acquisition-state variable
   */
  public Variable<Boolean> getUseCurrentAcquisitionStateVariable()
  {
    return mUseCurrentAcquisitionStateVariable;
  }

  /**
   * Returns lightsheet microscope
   *
   * @return lightsheet microscope
   */
  public LightSheetMicroscopeInterface getLightSheetMicroscope()
  {
    return mLightSheetMicroscope;
  }

  /**
   * Returns the number of cameras
   *
   * @return number of cameras
   */
  public int getNumberOfCameras()
  {
    int lNumberOfCameras = mLightSheetMicroscope.getNumberOfDevices(StackCameraDeviceInterface.class);
    return lNumberOfCameras;
  }

  private int getNumberOfLightsSheets()
  {
    int lNumberOfLightsSheets = mLightSheetMicroscope.getNumberOfDevices(LightSheet.class);
    return lNumberOfLightsSheets;
  }

  /**
   * Returns the sync detection arms variable
   *
   * @return sync detection arms variable
   */
  public Variable<Boolean> getSyncDetectionArmsVariable()
  {
    return mSyncDetectionArmsVariable;
  }

  /**
   * Returns the sync lightsheets variable
   *
   * @return sync lightsheets variable
   */
  public Variable<Boolean> getSyncLightSheetsVariable()
  {
    return mSyncLightSheetsVariable;
  }

  /**
   * Returns the sync lightsheets and detection arms variable
   *
   * @return sync lightsheets and detection arms variable
   */
  public Variable<Boolean> getSyncLightSheetsAndDetectionArmsVariable()
  {
    return mSyncLightSheetsAndDetectionArmsVariable;
  }

  /**
   * Returns the 2D acquisition Z variable
   *
   * @return 2D acquisition Z variable
   */
  public BoundedVariable<Number> get2DAcquisitionZVariable()
  {
    return m2DAcquisitionZVariable;
  }

  /**
   * Returns the acquisition counter variable
   *
   * @return acquisition counter variable
   */
  public Variable<Long> getAcquisitionCounterVariable()
  {
    return mAcquisitionCounterVariable;

  }

  /**
   * Returns current acquisition mode
   *
   * @return current acquisition mode
   */
  public InteractiveAcquisitionModes getCurrentAcquisitionMode()
  {
    return mCurrentAcquisitionMode;
  }

  /**
   * Sets current acquisition mode
   *
   * @param pNewAcquisitionMode new acquisition mode
   */
  public void setCurrentAcquisitionMode(InteractiveAcquisitionModes pNewAcquisitionMode)
  {
    mCurrentAcquisitionMode = pNewAcquisitionMode;
  }

}
