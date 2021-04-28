package clearaudio.synthesizer;

import java.util.concurrent.locks.ReentrantLock;

import clearaudio.sound.SoundOut;
import clearaudio.synthesizer.sources.Source;

/**
 * Audio Synthesizer
 * 
 * This class must be given a source or filter - possibly belonging to a
 * source/filter graph and a SoundOut object to send the generated waveform to
 * the audio driver. The method playSamples(...) must e called repeatedly
 * (blocking call).
 * 
 * @author Loic Royer (2015)
 *
 */
public class Synthesizer
{

  private final Source mSource;

  private float[] mBuffer;
  private final SoundOut mSoundOut;

  /**
   * Constructs a synthesizer from a source/filter and a SoundOut object.
   * 
   * @param pSource
   *          source
   * @param pSoundOut
   *          sound out
   */
  public Synthesizer(Source pSource, SoundOut pSoundOut)
  {
    super();
    mSource = pSource;
    mSoundOut = pSoundOut;
  }

  /**
   * Plays a buffer of samples. The buffer length is determined internally.
   * 
   * @return estimated time in seconds spent in the call.
   */
  public double playSamples()
  {
    return playSamples(null);
  }

  /**
   * Plays a buffer of samples. The buffer length is determined internally. A
   * reentrant lock provided can be unlocked just before sending the data to the
   * driver, thus minimizing the locking time.
   * 
   * @param pReentrantLock
   *          reentrant lock to unlock.
   * @return estimated time in seconds spent in the call.
   */
  public double playSamples(ReentrantLock pReentrantLock)
  {
    return playSamples(512, pReentrantLock);
  }

  /**
   * Plays a buffer of samples of a given length.
   * 
   * @param pNumberOfSamples
   *          number of samples to play.
   * @return estimated time in seconds spent in the call.
   */
  public double playSamples(int pNumberOfSamples)
  {
    return playSamples(pNumberOfSamples, null);
  }

  /**
   * Plays a buffer of samples of a given length. A reentrant lock provided can
   * be unlocked just before sending the data to the driver, thus minimizing the
   * locking time.
   * 
   * @param pNumberOfSamples
   *          number of samples to play.
   * @param pReentrantLock
   *          reentrant lock to unlock.
   * @return duration in seconds
   */
  public double playSamples(int pNumberOfSamples,
                            ReentrantLock pReentrantLock)
  {
    if (mBuffer == null || mBuffer.length < pNumberOfSamples)
      mBuffer = new float[pNumberOfSamples];

    for (int i = 0; i < pNumberOfSamples; i++)
    {
      final float lValue = mSource.next();
      mBuffer[i] = lValue;
    }

    mSoundOut.play(mBuffer, pNumberOfSamples, pReentrantLock);

    return (((double) pNumberOfSamples) / mSoundOut.getSampleRate());

  }

}
