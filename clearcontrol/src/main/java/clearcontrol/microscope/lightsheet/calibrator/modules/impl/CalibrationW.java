package clearcontrol.microscope.lightsheet.calibrator.modules.impl;

import clearcontrol.core.math.functions.UnivariateAffineFunction;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.gui.jfx.custom.visualconsole.VisualConsoleInterface.ChartType;
import clearcontrol.microscope.lightsheet.LightSheetMicroscopeQueue;
import clearcontrol.microscope.lightsheet.calibrator.CalibrationEngine;
import clearcontrol.microscope.lightsheet.calibrator.modules.CalibrationModuleInterface;
import clearcontrol.microscope.lightsheet.calibrator.modules.CalibrationPerLightSheetBase;
import clearcontrol.microscope.lightsheet.calibrator.utils.ImageAnalysisUtils;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheet;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheetInterface;
import clearcontrol.microscope.lightsheet.configurationstate.ConfigurationState;
import clearcontrol.microscope.lightsheet.configurationstate.HasStateDescriptionPerLightSheet;
import clearcontrol.stack.OffHeapPlanarStack;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.stat.StatUtils;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Math.*;

/**
 * Lightsheet width calibration module
 *
 * @author royer
 */
public class CalibrationW extends CalibrationPerLightSheetBase implements CalibrationModuleInterface, HasStateDescriptionPerLightSheet
{
  private BoundedVariable<Integer> mNumberOfSamplesVariable = new BoundedVariable<Integer>("Number of samples", 32, 1, Integer.MAX_VALUE);

  BoundedVariable<Integer> mDetectionArmVariable;

  private HashMap<Integer, TDoubleArrayList> mIntensityLists = new HashMap<>();
  private TDoubleArrayList mWList = new TDoubleArrayList();

  /**
   * Instantiates a lightsheet width calibration module
   *
   * @param pCalibrator parent calibrator
   */
  public CalibrationW(CalibrationEngine pCalibrator)
  {
    super("W", pCalibrator);

    mDetectionArmVariable = new BoundedVariable<Integer>("Detection arm", 0, 0, pCalibrator.getLightSheetMicroscope().getNumberOfDetectionArms());
  }

  /**
   * Calibrates and the lightsheet width
   *
   * @return true when succeeded
   */
  public double calibrateAllLightSheets()
  {
    int lDetectionArmIndex = mDetectionArmVariable.get();
    if (calibrate(lDetectionArmIndex))
    {
      return apply();
    } else
    {
      return Double.MAX_VALUE;
    }
  }

  /**
   * Instantiates a lightsheet width calibration module
   *
   * @param pDetectionArmIndex detection arm index
   * @return true for success
   */
  private boolean calibrate(int pDetectionArmIndex)
  {
    mIntensityLists.clear();
    int lNumberOfLightSheets = getNumberOfLightSheets();
    for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      double[] lAverageIntensities = calibrate(lLightSheetIndex, pDetectionArmIndex, mNumberOfSamplesVariable.get());
      if (lAverageIntensities == null)
      {
        setConfigurationState(lLightSheetIndex, ConfigurationState.FAILED);
        return false;
      }

      mIntensityLists.put(lLightSheetIndex, new TDoubleArrayList(lAverageIntensities));

      setConfigurationState(lLightSheetIndex, ConfigurationState.SUCCEEDED);
    }

    return true;
  }

  /**
   * Calibrates the lightsheet width for a given lightsheet, detection arm, and number of
   * samples.
   *
   * @param pLightSheetIndex   lightsheet index
   * @param pDetectionArmIndex detection arm index
   * @param pNumberOfSamples   number of samples
   * @return metric value per plane.
   */
  private double[] calibrate(int pLightSheetIndex, int pDetectionArmIndex, int pNumberOfSamples)
  {

    try
    {
      LightSheetInterface lLightSheetDevice = getLightSheetMicroscope().getDeviceLists().getDevice(LightSheetInterface.class, pLightSheetIndex);

      BoundedVariable<Number> lWVariable = lLightSheetDevice.getWidthVariable();

      @SuppressWarnings("unused") UnivariateAffineFunction lWFunction = lLightSheetDevice.getWidthFunction().get();
      double lMinW = lWVariable.getMin().doubleValue();
      double lMaxW = lWVariable.getMax().doubleValue();
      double lStep = (lMaxW - lMinW) / pNumberOfSamples;

      // Building queue start:
      LightSheetMicroscopeQueue lQueue = getLightSheetMicroscope().requestQueue();
      lQueue.clearQueue();
      lQueue.zero();

      lQueue.setI(pLightSheetIndex);
      lQueue.setIX(pLightSheetIndex, 0);
      lQueue.setIY(pLightSheetIndex, 0);
      lQueue.setIZ(pLightSheetIndex, 0);
      lQueue.setIH(pLightSheetIndex, 0);

      lQueue.setDZ(pDetectionArmIndex, 0);
      lQueue.setC(pDetectionArmIndex, false);

      lQueue.setIZ(pLightSheetIndex, lMinW);
      lQueue.addCurrentStateToQueue();

      mWList.clear();
      for (double w = lMinW; w <= lMaxW; w += lStep)
      {
        mWList.add(w);
        lQueue.setIZ(pLightSheetIndex, w);

        lQueue.setC(pDetectionArmIndex, false);
        for (int i = 0; i < 10; i++)
          lQueue.addCurrentStateToQueue();

        lQueue.setC(pDetectionArmIndex, true);
        lQueue.addCurrentStateToQueue();
      }

      lQueue.addVoxelDimMetaData(getLightSheetMicroscope(), 10);

      lQueue.finalizeQueue();
      // Building queue end.

      getLightSheetMicroscope().useRecycler("adaptation", 1, 4, 4);
      final Boolean lPlayQueueAndWait = getLightSheetMicroscope().playQueueAndWaitForStacks(lQueue, lQueue.getQueueLength(), TimeUnit.SECONDS);

      if (!lPlayQueueAndWait) return null;

      final OffHeapPlanarStack lStack = (OffHeapPlanarStack) getLightSheetMicroscope().getCameraStackVariable(pDetectionArmIndex).get();

      long lWidth = lStack.getWidth();
      long lHeight = lStack.getHeight();

      System.out.format("Image: width=%d, height=%d \n", lWidth, lHeight);

      double[] lAverageIntensities = ImageAnalysisUtils.computeImageAverageIntensityPerPlane(lStack);

      System.out.format("Image: average intensities: \n");

      String lChartName = String.format("D=%d, I=%d", pDetectionArmIndex, pLightSheetIndex);

      getCalibrationEngine().configureChart(lChartName, "avg. intensity", "IW", "intensity", ChartType.Line);

      for (int i = 0; i < lAverageIntensities.length; i++)
      {
        System.out.println(lAverageIntensities[i]);
        getCalibrationEngine().addPoint(lChartName, "avg. intensity", i == 0, mWList.get(i), lAverageIntensities[i]);
      }

      return lAverageIntensities;
    } catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      e.printStackTrace();
      return null;
    }

  }

  /**
   * Applies the lighsheet width calibration corrections
   *
   * @return residual error
   */
  public double apply()
  {
    int lNumberOfLightSheets = getNumberOfLightSheets();

    double lError = 0;

    double lIntensityMin = Double.POSITIVE_INFINITY;
    double lIntensityMax = Double.NEGATIVE_INFINITY;

    TDoubleArrayList lSums = new TDoubleArrayList();
    for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      TDoubleArrayList lIntensityList = mIntensityLists.get(lLightSheetIndex);
      lSums.add(lIntensityList.sum());

      lIntensityMin = min(lIntensityMin, lIntensityList.min());
      lIntensityMax = max(lIntensityMax, lIntensityList.max());
    }

    double lLargestSum = Double.NEGATIVE_INFINITY;
    int lIndexOfLargestSum = -1;
    for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      double lSum = lSums.get(lLightSheetIndex);
      if (lSum > lLargestSum)
      {
        lIndexOfLargestSum = lLightSheetIndex;
        lLargestSum = lSum;
      }
    }

    TDoubleArrayList lReferenceIntensityList = mIntensityLists.get(lIndexOfLargestSum);

    TDoubleArrayList[] lOffsetsLists = new TDoubleArrayList[lNumberOfLightSheets];
    for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      lOffsetsLists[lLightSheetIndex] = new TDoubleArrayList();
    }
    double lStep = (lIntensityMax - lIntensityMin) / (lReferenceIntensityList.size());

    for (double lIntensity = lIntensityMin; lIntensity <= lIntensityMax; lIntensity += lStep)
    {
      for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
      {

        int lReferenceIndex = searchFirstAbove(lReferenceIntensityList, lIntensity);
        double lReferenceW = mWList.get(lReferenceIndex);

        int lOtherIndex = searchFirstAbove(lReferenceIntensityList, lIntensity);

        double lOtherW = mWList.get(lOtherIndex);

        double lOffsets = lOtherW - lReferenceW;

        lOffsetsLists[lLightSheetIndex].add(lOffsets);

      }
    }

    TDoubleArrayList lMedianOffsets = new TDoubleArrayList();
    for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      double lMedianOffset = StatUtils.percentile(lOffsetsLists[lLightSheetIndex].toArray(), 50);
      lMedianOffsets.add(lMedianOffset);
    }

    for (int lLightSheetIndex = 0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      LightSheetInterface lLightSheetDevice = getLightSheetMicroscope().getDeviceLists().getDevice(LightSheet.class, lLightSheetIndex);

      UnivariateAffineFunction lFunction = lLightSheetDevice.getWidthFunction().get();

      double lOffset = lMedianOffsets.get(lLightSheetIndex);

      System.out.format("Applying offset: %g to lightsheet %d \n", lOffset, lLightSheetIndex);

      lFunction.composeWith(UnivariateAffineFunction.axplusb(1, lOffset));

      System.out.format("Width function for lightsheet %d is now: %s \n", lLightSheetIndex, lFunction);

      lError += abs(lOffset);
    }

    System.out.format("Error after applying width offset correction: %g \n", lError);

    return lError;
  }

  private int searchFirstAbove(TDoubleArrayList pList, double pValue)
  {
    int lSize = pList.size();
    for (int i = 0; i < lSize; i++)
    {
      if (pList.getQuick(i) >= pValue)
      {
        return i;
      }
    }
    return lSize - 1;
  }

  /**
   * Resets calibration
   */
  @Override
  public void reset()
  {
    super.reset();

    for (int lLightSheetIndex = 0; lLightSheetIndex < this.getLightSheetMicroscope().getNumberOfLightSheets(); lLightSheetIndex++)
    {
      setConfigurationState(lLightSheetIndex, ConfigurationState.UNINITIALIZED);
    }
  }

  public BoundedVariable<Integer> getDetectionArmVariable()
  {
    return mDetectionArmVariable;
  }

  public BoundedVariable<Integer> getNumberOfSamplesVariable()
  {
    return mNumberOfSamplesVariable;
  }

  @Override
  public String getStateDescription(int pLightSheetIndex)
  {
    final LightSheetInterface lLightSheetDevice = getLightSheetMicroscope().getDeviceLists().getDevice(LightSheetInterface.class, pLightSheetIndex);

    UnivariateAffineFunction lUnivariateAffineFunction = lLightSheetDevice.getWidthFunction().get();

    return String.format("y = %.3f * x + %.3f", lUnivariateAffineFunction.getSlope(), lUnivariateAffineFunction.getConstant());
  }

  @Override
  public String getStateDescription()
  {
    return null;
  }
}
