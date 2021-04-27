package fastfuse.tasks;

import java.util.Set;

import fastfuse.FastFusionEngineInterface;

/**
 * Task interface
 *
 * @author royer
 */
public interface TaskInterface
{

  /**
   * Checks if required images are available
   * 
   * @param pAvailableImagesSlotKeys
   *          set of available slot keys
   * @return true if all required images are available
   */
  public boolean checkIfRequiredImagesAvailable(Set<String> pAvailableImagesSlotKeys);

  /**
   * Enqueues the computation necessary to perform this task
   * 
   * @param pFastFusionEngine
   *          fast fusion engines
   * @param pWaitToFinish
   *          true -> waits for computation to finish
   * @return true if success in starting the task
   */
  public boolean enqueue(FastFusionEngineInterface pFastFusionEngine,
                         boolean pWaitToFinish);

}
