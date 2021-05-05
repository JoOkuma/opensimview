package clearcontrol.timelapse;

import clearcontrol.LightSheetMicroscope;
import clearcontrol.core.concurrent.timing.ElapsedTime;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.instructions.InstructionInterface;
import clearcontrol.processor.LightSheetFastFusionEngine;
import clearcontrol.processor.LightSheetFastFusionProcessor;
import clearcontrol.timelapse.io.ProgramWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * A LightSheetTimelapse is a list of instructions, which are executed one by one as long
 * as the timelapse is running.
 *
 * @author royer
 * @author haesleinhuepf
 */
public class LightSheetTimelapse extends TimelapseBase implements TimelapseInterface, LoggingFeature
{

  private static final long cTimeOut = 1000;
  private static final int cMinimumNumberOfAvailableStacks = 16;
  private static final int cMaximumNumberOfAvailableStacks = 16;
  private static final int cMaximumNumberOfLiveStacks = 16;

  private final LightSheetMicroscope mLightSheetMicroscope;

  private ArrayList<InstructionInterface> mCurrentProgram = new ArrayList<InstructionInterface>();

  private Variable<Integer> mInstructionIndexVariable = new Variable<Integer>("instructions index", 0);

  ArrayList<InstructionInterface> mInitializedInstructionsList;


  /**
   * @param pLightSheetMicroscope microscope
   */
  public LightSheetTimelapse(LightSheetMicroscope pLightSheetMicroscope)
  {
    super(pLightSheetMicroscope);
    mLightSheetMicroscope = pLightSheetMicroscope;

    this.getMaxNumberOfTimePointsVariable().set(999999L);
  }

  @Override
  public void startTimelapse()
  {
    super.startTimelapse();


    File lProgramFile = new File(getWorkingDirectory(), "program.txt");
    ProgramWriter writer = new ProgramWriter(mCurrentProgram, lProgramFile);
    writer.write();

    mInitializedInstructionsList = new ArrayList<InstructionInterface>();

    LightSheetFastFusionProcessor lLightSheetFastFusionProcessor = mLightSheetMicroscope.getDevice(LightSheetFastFusionProcessor.class, 0);
    LightSheetFastFusionEngine lLightSheetFastFusionEngine = lLightSheetFastFusionProcessor.getEngine();
    if (lLightSheetFastFusionEngine != null)
    {
      lLightSheetFastFusionEngine.reset(true);
    }
    mInstructionIndexVariable.set(0);
  }

  @Override
  public boolean programStep()
  {

    try
    {
      info("Executing timepoint: " + getTimePointCounterVariable().get());

      mLightSheetMicroscope.useRecycler("3DTimelapse", cMinimumNumberOfAvailableStacks, cMaximumNumberOfAvailableStacks, cMaximumNumberOfLiveStacks);

      InstructionInterface lNextInstructionToRun = mCurrentProgram.get(mInstructionIndexVariable.get());

      // if the instruction wasn't initialized yet, initialize it now!
      if (!mInitializedInstructionsList.contains(lNextInstructionToRun))
      {
        lNextInstructionToRun.initialize();
        mInitializedInstructionsList.add(lNextInstructionToRun);
      }

      info("Starting " + lNextInstructionToRun);
      double duration = ElapsedTime.measure("instructions execution", () ->
      {
        lNextInstructionToRun.enqueue(getTimePointCounterVariable().get());
      });
      info("Finished " + lNextInstructionToRun + "in " + duration + " ms");

      // Determine the next instruction
      mInstructionIndexVariable.set(mInstructionIndexVariable.get() + 1);
      if (mInstructionIndexVariable.get() > mCurrentProgram.size() - 1)
      {
        // At this point teh program loop has finished... we will restart a loop.
        mInstructionIndexVariable.set(0);
        info("Finished time point:" + getTimePointCounterVariable());
        getTimePointCounterVariable().increment();
        if (getStopSignalVariable().get())
        {
          return false;
        } else
        {
          waitForNextTimePoint();
          return true;
        }
      }

    } catch (Throwable e)
    {
      e.printStackTrace();
    }

    return false;
  }


  public long getTimeOut()
  {
    return cTimeOut;
  }

  /**
   * Deprecated: use getCurrentProgram() instead
   *
   * @return current program as list of instructions
   */
  @Deprecated
  public ArrayList<InstructionInterface> getListOfActivatedSchedulers()
  {
    return getCurrentProgram();
  }

  /**
   * @return current program as list of instructions
   */
  public ArrayList<InstructionInterface> getCurrentProgram()
  {
    return mCurrentProgram;
  }

  public ArrayList<InstructionInterface> getListOfAvailableSchedulers(String... pMustContainStrings)
  {
    ArrayList<InstructionInterface> lListOfAvailabeSchedulers = new ArrayList<>();
    for (InstructionInterface lScheduler : mLightSheetMicroscope.getDevices(InstructionInterface.class))
    {
      boolean lNamePatternMatches = true;
      for (String part : pMustContainStrings)
      {
        if (!lScheduler.toString().toLowerCase().contains(part.toLowerCase()))
        {
          lNamePatternMatches = false;
          break;
        }
      }
      if (lNamePatternMatches)
      {
        lListOfAvailabeSchedulers.add(lScheduler);
      }
    }

    lListOfAvailabeSchedulers.sort(new Comparator<InstructionInterface>()
    {
      @Override
      public int compare(InstructionInterface o1, InstructionInterface o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });

    return lListOfAvailabeSchedulers;
  }

  public Variable<Integer> getLastExecutedSchedulerIndexVariable()
  {
    return mInstructionIndexVariable;
  }

}
