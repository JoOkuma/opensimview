package clearcontrol.microscope.lightsheet.component.lightsheet.si;

import clearcontrol.devices.signalgen.staves.BinaryPatternSteppingStave;
import clearcontrol.devices.signalgen.staves.StaveInterface;

/**
 * Binary structure illumination pattern
 *
 * @author royer
 */
public class BinaryStructuredIlluminationPattern extends StructuredIlluminationPatternBase implements
                                                                                           StructuredIlluminationPatternInterface
{

  private final double mPatternPeriod = 2;
  private final double mPatternPhaseIndex = 0;
  private final double mPatternOnLength = 1;
  private final double mPatternPhaseIncrement = 1;

  private final BinaryPatternSteppingStave mStave;

  /**
   * Instantiates a binary structured illumination pattern
   */
  public BinaryStructuredIlluminationPattern()
  {
    super();
    mStave = new BinaryPatternSteppingStave("trigger.out.e");
  }

  @Override public StaveInterface getStave(double pMarginTimeRelativeUnits)
  {
    mStave.setSyncStart((float) clamp01(pMarginTimeRelativeUnits));
    mStave.setSyncStop((float) clamp01(1 - pMarginTimeRelativeUnits));
    mStave.setPatternPeriod((int) mPatternPeriod);
    mStave.setPatternPhaseIndex((int) mPatternPhaseIndex);
    mStave.setPatternOnLength((int) mPatternOnLength);
    mStave.setPatternPhaseIncrement((int) mPatternPhaseIncrement);
    return mStave;
  }

  @Override public int getNumberOfPhases()
  {
    final double lPatternPeriod = mPatternPeriod;
    final double lPatternPhaseIncrement = mPatternPhaseIncrement;
    final int lNumberOfPhases = (int) (lPatternPeriod / lPatternPhaseIncrement);
    return lNumberOfPhases;
  }

}
