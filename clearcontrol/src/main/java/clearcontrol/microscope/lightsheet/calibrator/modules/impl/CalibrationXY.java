package clearcontrol.microscope.lightsheet.calibrator.modules.impl;

import clearcontrol.core.math.functions.UnivariateAffineFunction;
import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.microscope.lightsheet.LightSheetMicroscopeQueue;
import clearcontrol.microscope.lightsheet.calibrator.CalibrationEngine;
import clearcontrol.microscope.lightsheet.calibrator.modules.CalibrationModuleInterface;
import clearcontrol.microscope.lightsheet.calibrator.modules.CalibrationPerLightSheetBase;
import clearcontrol.microscope.lightsheet.calibrator.utils.ImageAnalysisUtils;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheetInterface;
import clearcontrol.microscope.lightsheet.configurationstate.ConfigurationState;
import clearcontrol.microscope.lightsheet.configurationstate.HasStateDescriptionPerLightSheet;
import clearcontrol.stack.OffHeapPlanarStack;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.stat.StatUtils;
import org.ejml.factory.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 * Calibrates lightsheet position in the XY plane
 *
 * @author royer
 */
public class CalibrationXY extends CalibrationPerLightSheetBase implements CalibrationModuleInterface, HasStateDescriptionPerLightSheet
{

  private int mNumberOfDetectionArmDevices;

  private MultiKeyMap<Integer, Vector2D> mOriginFromX, mUnitVectorFromX, mOriginFromY, mUnitVectorFromY;

  private MultiKeyMap<Integer, SimpleMatrix> mTransformMatrices;

  private BoundedVariable<Integer> mMaxIterationsVariable = new BoundedVariable<Integer>("Maximum number of iterations", 3, 0, Integer.MAX_VALUE);

  BoundedVariable<Integer> mNumberOfPointsVariable = new BoundedVariable<Integer>("Number of points", 3, 0, Integer.MAX_VALUE);

  private BoundedVariable<Double> mLightSheetWidthWhileImaging = new BoundedVariable<Double>("Light sheet width while imaging", 0.45, 0.0, 1.0, 0.01);

  private BoundedVariable<Double> mStoppingConditionErrorThreshold = new BoundedVariable<Double>("Stopping condition error threshold", 0.05, 0.0, Double.MAX_VALUE, 0.001);

  /**
   * Instantiates a XY calibration module given a parent calibrator.
   *
   * @param pCalibrator parent calibrator
   */
  public CalibrationXY(CalibrationEngine pCalibrator)
  {
    super("XY", pCalibrator);

    mOriginFromX = new MultiKeyMap<>();
    mUnitVectorFromX = new MultiKeyMap<>();
    mOriginFromY = new MultiKeyMap<>();
    mUnitVectorFromY = new MultiKeyMap<>();

    mTransformMatrices = new MultiKeyMap<>();
  }

  public double calibrate(int pLightSheetIndex)
  {
    int lIteration = 0;
    double lError = Double.POSITIVE_INFINITY;
    do
    {
      setConfigurationState(pLightSheetIndex, ConfigurationState.fromProgressValue((double) lIteration / mMaxIterationsVariable.get()));

      lError = calibrateXY(pLightSheetIndex, 0, mNumberOfPointsVariable.get());
      info("############################################## Error = " + lError);

      if (getCalibrationEngine().isStopRequested())
      {
        setConfigurationState(pLightSheetIndex, ConfigurationState.CANCELLED);
        return Double.NaN;
      }
    } while (lError >= mStoppingConditionErrorThreshold.get() && lIteration++ < mMaxIterationsVariable.get());
    info("############################################## Done ");

    if (Double.isNaN(lError))
    {
      setConfigurationState(pLightSheetIndex, ConfigurationState.FAILED);
    } else if (lError < mStoppingConditionErrorThreshold.get())
    {
      setConfigurationState(pLightSheetIndex, ConfigurationState.SUCCEEDED);
    } else
    {
      setConfigurationState(pLightSheetIndex, ConfigurationState.ACCEPTABLE);
    }
    return lError;
  }

  /**
   * Calibrates the XY position of the lighthsheets
   *
   * @param pLightSheetIndex   lightshet index
   * @param pDetectionArmIndex detection arm index
   * @param pNumberOfPoints    number of points
   * @return true when succeeded
   */
  private double calibrateXY(int pLightSheetIndex, int pDetectionArmIndex, int pNumberOfPoints)
  {
    calibrate(pLightSheetIndex, pDetectionArmIndex, pNumberOfPoints);

    return apply(pLightSheetIndex, pDetectionArmIndex);
  }

  /**
   * Calibrates
   *
   * @param pLightSheetIndex   lightsheet index
   * @param pDetectionArmIndex detection arm index
   * @param pNumberOfPoints    number of points
   * @return true for success
   */
  private boolean calibrate(int pLightSheetIndex, int pDetectionArmIndex, int pNumberOfPoints)
  {
    boolean result = calibrate(pLightSheetIndex, pDetectionArmIndex, pNumberOfPoints, true) && calibrate(pLightSheetIndex, pDetectionArmIndex, pNumberOfPoints, false);
    if (!result)
    {
      setConfigurationState(pLightSheetIndex, ConfigurationState.FAILED);
    }
    return result;
  }

  private boolean calibrate(int pLightSheetIndex, int pDetectionArmIndex, int pNumberOfPoints, boolean pDoAxisX)
  {
    LightSheetInterface lLightSheet = getLightSheetMicroscope().getDeviceLists().getDevice(LightSheetInterface.class, pLightSheetIndex);

    double lMin, lMax;

    if (pDoAxisX)
    {
      BoundedVariable<Number> lLightSheetXFunction = lLightSheet.getXVariable();
      lMin = lLightSheetXFunction.getMin().doubleValue();
      lMax = lLightSheetXFunction.getMax().doubleValue();
    } else
    {
      BoundedVariable<Number> lLightSheetYFunction = lLightSheet.getYVariable();
      lMin = lLightSheetYFunction.getMin().doubleValue();
      lMax = lLightSheetYFunction.getMax().doubleValue();
    }

    try
    {
      TDoubleArrayList lOriginXList = new TDoubleArrayList();
      TDoubleArrayList lOriginYList = new TDoubleArrayList();

      TDoubleArrayList lUnitVectorXList = new TDoubleArrayList();
      TDoubleArrayList lUnitVectorYList = new TDoubleArrayList();

      double lMaxAbsY = min(abs(lMin), abs(lMax));
      for (double f = 0.5 * lMaxAbsY; f <= 0.7 * lMaxAbsY; f += (0.2 * lMaxAbsY / (pNumberOfPoints - 1)))
      {
        Vector2D lCenterP, lCenter0, lCenterN;

        int lNumberOfPreImages = 6;

        if (pDoAxisX)
        {
          lCenterP = lightSheetImageCenterWhenAt(pLightSheetIndex, pDetectionArmIndex, f, 0, lNumberOfPreImages);

          lCenter0 = lightSheetImageCenterWhenAt(pLightSheetIndex, pDetectionArmIndex, 0, 0, lNumberOfPreImages);

          lCenterN = lightSheetImageCenterWhenAt(pLightSheetIndex, pDetectionArmIndex, -f, 0, lNumberOfPreImages);
        } else
        {
          lCenterP = lightSheetImageCenterWhenAt(pLightSheetIndex, pDetectionArmIndex, 0, f, lNumberOfPreImages);

          lCenter0 = lightSheetImageCenterWhenAt(pLightSheetIndex, pDetectionArmIndex, 0, 0, lNumberOfPreImages);

          lCenterN = lightSheetImageCenterWhenAt(pLightSheetIndex, pDetectionArmIndex, 0, -f, lNumberOfPreImages);
        }

        System.out.format("center at %g: %s \n", f, lCenterP);
        System.out.format("center at %g: %s \n", -f, lCenterN);

        if (lCenterP == null && lCenterN == null) continue;

        lOriginXList.add(lCenter0.getX());
        lOriginYList.add(lCenter0.getY());

        if (f != 0)
        {
          double ux = (lCenterP.getX() + lCenterN.getX()) / 2f;
          double uy = (lCenterP.getY() + lCenterN.getY()) / 2f;

          System.out.format("Unit vector: (%g,%g) \n", ux, uy);

          lUnitVectorXList.add(ux);
          lUnitVectorYList.add(uy);
        }
      }

      double lOriginX = StatUtils.percentile(lOriginXList.toArray(), 50);
      double lOriginY = StatUtils.percentile(lOriginYList.toArray(), 50);

      double lUnitVectorX = StatUtils.percentile(lUnitVectorXList.toArray(), 50);
      double lUnitVectorY = StatUtils.percentile(lUnitVectorYList.toArray(), 50);

      if (pDoAxisX)
      {
        mOriginFromX.put(pLightSheetIndex, pDetectionArmIndex, new Vector2D(lOriginX, lOriginY));
        mUnitVectorFromX.put(pLightSheetIndex, pDetectionArmIndex, new Vector2D(lUnitVectorX, lUnitVectorY));

        System.out.format("From X axis: \n");
        System.out.format("Origin : %s \n", mOriginFromX);
        System.out.format("Unit Vector : %s \n", mUnitVectorFromX);
      } else
      {
        mOriginFromY.put(pLightSheetIndex, pDetectionArmIndex, new Vector2D(lOriginX, lOriginY));
        mUnitVectorFromY.put(pLightSheetIndex, pDetectionArmIndex, new Vector2D(lUnitVectorX, lUnitVectorY));

        System.out.format("From X axis: \n");
        System.out.format("Origin : %s \n", mOriginFromY);
        System.out.format("Unit Vector : %s \n", mUnitVectorFromY);
      }

    } catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      e.printStackTrace();
      return false;
    } finally
    {
    }

    return true;
  }

  private Vector2D lightSheetImageCenterWhenAt(int pLightSheetIndex, int pDetectionArmIndex, double pX, double pY, int pN) throws InterruptedException, ExecutionException, TimeoutException
  {
    // Building queue start:
    LightSheetMicroscopeQueue lQueue = getLightSheetMicroscope().requestQueue();
    lQueue.clearQueue();
    lQueue.zero();

    lQueue.setI(pLightSheetIndex);
    lQueue.setIW(pLightSheetIndex, mLightSheetWidthWhileImaging.get());
    lQueue.setIZ(pLightSheetIndex, 0);
    lQueue.setIH(pLightSheetIndex, 0);
    lQueue.setIZ(pLightSheetIndex, 0); // TODO: is this a type here? We do the
    // same two lines above...

    for (int i = 0; i < mNumberOfDetectionArmDevices; i++)
      lQueue.setDZ(i, 0);

    for (int i = 1; i <= pN; i++)
    {
      lQueue.setIX(pLightSheetIndex, pX);
      lQueue.setIY(pLightSheetIndex, pY);
      for (int d = 0; d < mNumberOfDetectionArmDevices; d++)
        lQueue.setC(d, i == pN);
      lQueue.addCurrentStateToQueue();
    }

    lQueue.addVoxelDimMetaData(getLightSheetMicroscope(), 10);

    lQueue.setTransitionTime(0.1);

    lQueue.finalizeQueue();
    // Building queue end.

    getLightSheetMicroscope().useRecycler("adaptation", 1, 4, 4);
    final Boolean lPlayQueueAndWait = getLightSheetMicroscope().playQueueAndWaitForStacks(lQueue, lQueue.getQueueLength(), TimeUnit.SECONDS);

    if (!lPlayQueueAndWait) return null;

    final OffHeapPlanarStack lStack = (OffHeapPlanarStack) getLightSheetMicroscope().getCameraStackVariable(pDetectionArmIndex).get();

    int lWidth = (int) lStack.getWidth();
    int lHeight = (int) lStack.getHeight();

    ImageAnalysisUtils.cleanWithMin(lStack);
    Vector2D lPoint = ImageAnalysisUtils.findCOMOfBrightestPointsForEachPlane(lStack)[0];

    lPoint = lPoint.subtract(new Vector2D(0.5 * lWidth, 0.5 * lHeight));

    System.out.format("image: lightsheet center at: %s \n", lPoint);

    Vector2D lNormalizedPoint = new Vector2D(2 * lPoint.getX() / lWidth, 2 * lPoint.getY() / lHeight);

    System.out.format("image: lightsheet center at normalized coord: %s \n", lNormalizedPoint);

    return lNormalizedPoint;
  }

  /**
   * Applies correction for the given lightsheet and detection arm
   *
   * @param pLightSheetIndex   lightsheet index
   * @param pDetectionArmIndex detection arm imdex
   * @return residual error
   */
  public double apply(int pLightSheetIndex, int pDetectionArmIndex)
  {
    System.out.format("Light sheet index: %d, detection arm index: %d \n", pLightSheetIndex, pDetectionArmIndex);

    Vector2D lOriginFromX = mOriginFromX.get(pLightSheetIndex, pDetectionArmIndex);
    Vector2D lOriginFromY = mOriginFromY.get(pLightSheetIndex, pDetectionArmIndex);

    System.out.format("lOriginFromX: %s \n", lOriginFromX);
    System.out.format("lOriginFromY: %s \n", lOriginFromY);

    Vector2D lOrigin = new Vector2D(0, 0);
    lOrigin = lOrigin.add(lOriginFromX);
    lOrigin = lOrigin.add(lOriginFromY);
    lOrigin = lOrigin.scalarMultiply(0.5);

    System.out.format("lOrigin: %s \n", lOrigin);

    Vector2D lUnitVectorU = mUnitVectorFromX.get(pLightSheetIndex, pDetectionArmIndex);
    Vector2D lUnitVectorV = mUnitVectorFromY.get(pLightSheetIndex, pDetectionArmIndex);

    System.out.format("lUnitVectorU: %s \n", lUnitVectorU);
    System.out.format("lUnitVectorV: %s \n", lUnitVectorV);

    SimpleMatrix lMatrix = new SimpleMatrix(2, 2);
    lMatrix.set(0, 0, lUnitVectorU.getX());
    lMatrix.set(1, 0, lUnitVectorU.getY());
    lMatrix.set(0, 1, lUnitVectorV.getX());
    lMatrix.set(1, 1, lUnitVectorV.getY());

    System.out.format("lMatrix: \n");

    mTransformMatrices.put(pLightSheetIndex, pDetectionArmIndex, lMatrix);
    SimpleMatrix lInverseMatrix;
    try
    {
      lMatrix.print(4, 3);
      lInverseMatrix = lMatrix.invert();
    } catch (SingularMatrixException e)
    {
      e.printStackTrace();
      setConfigurationState(pLightSheetIndex, ConfigurationState.FAILED);
      return Double.NaN;
    }
    System.out.format("lInverseMatrix: \n");
    lInverseMatrix.print(4, 6);

    SimpleMatrix lOriginAsMatrix = new SimpleMatrix(2, 1);
    lOriginAsMatrix.set(0, 0, lOrigin.getX());
    lOriginAsMatrix.set(1, 0, lOrigin.getY());

    System.out.format("lOriginAsMatrix: \n");
    lOriginAsMatrix.print(4, 3);

    SimpleMatrix lNewOffsets = lInverseMatrix.mult(lOriginAsMatrix);

    System.out.format("lNewOffsets:\n");
    lNewOffsets.print(4, 3);

    double lXOffset = lNewOffsets.get(0, 0);
    double lYOffset = lNewOffsets.get(1, 0);

    System.out.format("lXOffset: %s \n", lXOffset);
    System.out.format("lYOffset: %s \n", lYOffset);

    LightSheetInterface lLightSheetDevice = getLightSheetMicroscope().getDeviceLists().getDevice(LightSheetInterface.class, pLightSheetIndex);

    System.out.format("lLightSheetDevice: %s \n", lLightSheetDevice);

    Variable<UnivariateAffineFunction> lFunctionXVariable = lLightSheetDevice.getXFunction();
    Variable<UnivariateAffineFunction> lFunctionYVariable = lLightSheetDevice.getYFunction();

    System.out.format("lFunctionXVariable: %s \n", lFunctionXVariable);
    System.out.format("lFunctionYVariable: %s \n", lFunctionYVariable);

    // TODO: use pixel calibration here...
    lFunctionXVariable.get().composeWith(UnivariateAffineFunction.axplusb(1, -lXOffset));
    lFunctionYVariable.get().composeWith(UnivariateAffineFunction.axplusb(1, -lYOffset));

    lFunctionXVariable.setCurrent();
    lFunctionYVariable.setCurrent();

    System.out.format("Updated-> lFunctionXVariable: %s \n", lFunctionXVariable);
    System.out.format("Updated-> lFunctionYVariable: %s \n", lFunctionYVariable);

    // TODO: use pixel calibration here...
    BoundedVariable<Number> lHeightVariable = lLightSheetDevice.getHeightVariable();
    Variable<UnivariateAffineFunction> lHeightFunctionVariable = lLightSheetDevice.getHeightFunction();
    System.out.format("lHeightFunctionVariable: %s \n", lHeightFunctionVariable);
    UnivariateAffineFunction lHeightFunction = UnivariateAffineFunction.axplusb(1, 0);
    lHeightVariable.setMinMax(-1, 1);

    lHeightFunctionVariable.set(lHeightFunction);
    lHeightFunctionVariable.setCurrent();

    System.out.format("Updated-> lHeightFunctionVariable: %s \n", lHeightFunctionVariable);

    double lError = abs(lXOffset) + abs(lYOffset);

    System.out.format("lError: %s \n", lError);

    // setCalibrationState(pLightSheetIndex, ConfigurationState.SUCCEEDED);

    return lError;
  }

  /**
   * Resets calibration here
   */
  @Override
  public void reset()
  {
    // check if there is nothing to do here

    for (int i = 0; i < this.getLightSheetMicroscope().getNumberOfLightSheets(); i++)
    {
      setConfigurationState(i, ConfigurationState.UNINITIALIZED);
    }
  }

  /**
   * Returns the transformation matrix for a given ligthsheet and detection arm.
   *
   * @param pLightSheetIndex   lightsheet index
   * @param pDetectionArmIndex detection arm
   * @return transformation matrix
   */
  public SimpleMatrix getTransformMatrix(int pLightSheetIndex, int pDetectionArmIndex)
  {
    return mTransformMatrices.get(pLightSheetIndex, pDetectionArmIndex);
  }

  public BoundedVariable<Integer> getMaxIterationsVariable()
  {
    return mMaxIterationsVariable;
  }

  public BoundedVariable<Integer> getNumberOfPointsVariable()
  {
    return mNumberOfPointsVariable;
  }

  public BoundedVariable<Double> getStoppingConditionErrorThreshold()
  {
    return mStoppingConditionErrorThreshold;
  }

  public BoundedVariable<Double> getLightSheetWidthWhileImaging()
  {
    return mLightSheetWidthWhileImaging;
  }

  @Override
  public String getStateDescription(int pLightSheetIndex)
  {
    final LightSheetInterface lLightSheetDevice = getLightSheetMicroscope().getDeviceLists().getDevice(LightSheetInterface.class, pLightSheetIndex);

    UnivariateAffineFunction lUnivariateAffineFunctionX = lLightSheetDevice.getXFunction().get();
    UnivariateAffineFunction lUnivariateAffineFunctionY = lLightSheetDevice.getYFunction().get();

    return String.format("X: y = %.3f * x + %.3f", lUnivariateAffineFunctionX.getSlope(), lUnivariateAffineFunctionX.getConstant()) + "\n" + String.format("Y: y = %.3f * x + %.3f", lUnivariateAffineFunctionY.getSlope(), lUnivariateAffineFunctionY.getConstant());
  }

  @Override
  public String getStateDescription()
  {
    return "";
  }

}
