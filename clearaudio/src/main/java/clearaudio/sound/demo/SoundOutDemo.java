package clearaudio.sound.demo;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

import javax.sound.sampled.LineUnavailableException;

import clearaudio.sound.SoundOut;

import org.junit.Test;

/**
 * Demo of the facade API.
 *
 * @author Loic Royer (2015)
 *
 */
public class SoundOutDemo
{

  @Test
  public void testStartStop() throws LineUnavailableException,
                              InterruptedException
  {
    int Period = 200;

    double[] lNoise = new double[Period];
    for (int j = 1; j < lNoise.length - 1; j++)
      lNoise[j] += 1 * (Math.random() - 0.5);

    for (int r = 0; r < 2; r++)
      for (int j = 1; j < lNoise.length - 1; j++)
        lNoise[j] = 0.5
                    * (lNoise[j - 1] + 2 * lNoise[j] + lNoise[j + 1])
                    / 4;/**/

    SoundOut lSoundOut = new SoundOut();

    for (int i = 0; i < 10; i++)
    {
      System.out.println("i=" + i);

      lSoundOut.start();
      for (int j = 0; j < 200; j++)
        lSoundOut.play(lNoise, lNoise.length);
      lSoundOut.stop();

      Thread.sleep(100);
    }

    lSoundOut.close();
  }

  @Test
  public void testGuitarSound() throws LineUnavailableException
  {
    SoundOut lSoundOut = new SoundOut();

    lSoundOut.start();

    int Period = 200;

    double[] lNoise = new double[Period];
    for (int j = 1; j < lNoise.length - 1; j++)
      lNoise[j] += 1 * (Math.random() - 0.5);

    for (int r = 0; r < 2; r++)
      for (int j = 1; j < lNoise.length - 1; j++)
        lNoise[j] = 0.5
                    * (lNoise[j - 1] + 2 * lNoise[j] + lNoise[j + 1])
                    / 4;/**/

    double[] lBuffer = new double[Period];

    boolean lInversion = false;
    for (int i = 0; i < 10000; i++)
    {
      for (int j = 1; j < lBuffer.length - 1; j++)
        lBuffer[j] = 0.99
                     * (lBuffer[j - 1] + lBuffer[j] + lBuffer[j + 1])
                     / 3;

      if (i % ((100000) / Period) == 0)
      {
        for (int j = 1; j < lBuffer.length - 1; j++)
          lBuffer[j] += (lInversion ? 1 : -1) * lNoise[j];
        lInversion = !lInversion;
      }

      lSoundOut.play(lBuffer, lBuffer.length);

      for (int j = 0; j < lBuffer.length; j++)
        lBuffer[j] = -lBuffer[lBuffer.length - 1 - j];

    }

    lSoundOut.stop();
  }

  @Test
  public void testDevilStaircase() throws LineUnavailableException
  {
    SoundOut lSoundOut = new SoundOut();

    lSoundOut.start();

    int Period = 1024;
    double ws = 10;
    double we = 40;
    double dw = 2;
    double pw = 0;
    double dpw = 0.01;

    double[] lBuffer = new double[Period];

    for (int i = 0; i < 10000; i++)
    {
      for (int j = 0; j < lBuffer.length; j++)
        lBuffer[j] = 0;

      // for (double w = ws; w < we; w += dw)
      {
        double w = 10;
        double a = 0.5 * (1 + sin(2 * PI * (w - ws) / (we - ws)));
        for (int j = 0; j < lBuffer.length; j++)
          lBuffer[j] = 0.01 * sin((w + pw)
                                  * ((2 * PI * j) / lBuffer.length));
      }

      pw += dpw;
      // if (pw > dw)
      // pw = 0;

      lSoundOut.play(lBuffer, lBuffer.length);

    }

    lSoundOut.stop();
  }

}
