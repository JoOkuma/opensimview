package clearcontrol.devices.lasers.devices.omicron.adapters;

import clearcontrol.com.serial.adapters.SerialTextDeviceAdapter;
import clearcontrol.devices.lasers.devices.omicron.adapters.protocol.ProtocolOmicron;

/**
 * Serial device adapter to query and set laser on/off state.
 *
 * @author royer
 */
public class SetLaserOnOffAdapter extends OmicronAdapter<Boolean>
                                  implements
                                  SerialTextDeviceAdapter<Boolean>
{

  @Override
  public byte[] getGetValueCommandMessage()
  {
    return null;
  }

  @Override
  public Boolean parseValue(final byte[] pMessage)
  {
    return null;
  }

  @Override
  public byte[] getSetValueCommandMessage(final Boolean pOldValue,
                                          final Boolean pNewValue)
  {
    return pNewValue ? ProtocolOmicron.cSetLaserOnCommand.getBytes()
                     : ProtocolOmicron.cSetLaserOffCommand.getBytes();
  }

  @Override
  public boolean checkAcknowledgementSetValueReturnMessage(final byte[] pMessage)
  {
    return super.checkAcknowledgementSetValueReturnMessage(pMessage);
  }

}
