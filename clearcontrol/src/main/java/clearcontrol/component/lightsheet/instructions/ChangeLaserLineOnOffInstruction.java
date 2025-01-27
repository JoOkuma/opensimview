package clearcontrol.component.lightsheet.instructions;

import clearcontrol.LightSheetMicroscope;
import clearcontrol.core.variable.Variable;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.instructions.LightSheetMicroscopeInstructionBase;
import clearcontrol.instructions.PropertyIOableInstructionInterface;
import clearcontrol.state.AcquisitionStateManager;
import clearcontrol.state.InterpolatedAcquisitionState;

/**
 * ChangeLightSheetYInstruction allows controlling the scan head to change the Y position
 * of light sheets
 * <p>
 * Author: @haesleinhuepf July 2018
 */
public class ChangeLaserLineOnOffInstruction extends LightSheetMicroscopeInstructionBase implements PropertyIOableInstructionInterface
{

  private final BoundedVariable<Integer> mLaserLineIndex;


  public ChangeLaserLineOnOffInstruction(LightSheetMicroscope pLightSheetMicroscope, int pLaserIndex)
  {
    super("Adaptation: Change laser line on/off state", pLightSheetMicroscope);

    mLaserLineIndex = new BoundedVariable<Integer>("Laser index", pLaserIndex, 0, pLightSheetMicroscope.getNumberOfLaserLines());
  }

  @Override
  public boolean initialize()
  {
    return true;
  }

  @Override
  public boolean execute(long pTimePoint)
  {
    InterpolatedAcquisitionState lState = (InterpolatedAcquisitionState) getLightSheetMicroscope().getDevice(AcquisitionStateManager.class, 0).getCurrentState();

    for (int la = 0; la < lState.getNumberOfLaserLines(); la++)
      lState.getLaserOnOffVariable(la).set(la == mLaserLineIndex.get());
    return true;
  }

  @Override
  public ChangeLaserLineOnOffInstruction copy()
  {
    return new ChangeLaserLineOnOffInstruction(getLightSheetMicroscope(), mLaserLineIndex.get());
  }

  public BoundedVariable<Integer> getLaserLineIndexVariable()
  {
    return mLaserLineIndex;
  }

  @Override
  public Variable[] getProperties()
  {
    return new Variable[]{getLaserLineIndexVariable()};
  }
}
