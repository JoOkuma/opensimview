package bindings;

import bindings.AO64_64b_Driver_CLibrary.U64;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * <i>native declaration : DriverFiles/66-16AO64/AO64eintface.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public abstract class GS_DMA_DESCRIPTOR extends Structure
{
  /**
   * BYTES to transfer (U32 = 4 Bytes)<br>
   * C type : U32
   */
  public NativeLong BytesDesc_1;
  /**
   * Max transfer size is 23 bits<br>
   * C type : U32
   */
  public NativeLong BytesDesc_2;
  /**
   * such that max U32 transfer size<br>
   * C type : U32
   */
  public NativeLong BytesDesc_3;
  /**
   * is (2097151) 0x1FFFFF<br>
   * C type : U32
   */
  public NativeLong BytesDesc_4;
  /**
   * Valid PHYSICAL address for contiguous<br>
   * C type : U64
   */
  public U64 PhyAddrDesc_1;
  /**
   * memory block<br>
   * C type : U64
   */
  public U64 PhyAddrDesc_2;
  /**
   * DO NOT use an address obtained from<br>
   * C type : U64
   */
  public U64 PhyAddrDesc_3;
  /**
   * malloc() or similiar functions<br>
   * C type : U64
   */
  public U64 PhyAddrDesc_4;
  /** Conversion Error : NumDescriptors:4 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : LocalToPciDesc_1:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : LocalToPciDesc_2:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : LocalToPciDesc_3:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : LocalToPciDesc_4:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : InterruptDesc_1:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : InterruptDesc_2:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : InterruptDesc_3:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : InterruptDesc_4:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /** Conversion Error : DmaChannel:1 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
  /**
   * C type : U64
   */
  public U64 Rsvrd;

  public GS_DMA_DESCRIPTOR()
  {
    super();
  }

  protected List<String> getFieldOrder()
  {
    return Arrays.asList("BytesDesc_1", "BytesDesc_2", "BytesDesc_3", "BytesDesc_4", "PhyAddrDesc_1", "PhyAddrDesc_2", "PhyAddrDesc_3", "PhyAddrDesc_4", "Rsvrd");
  }

  /**
   * @param BytesDesc_1   BYTES to transfer (U32 = 4 Bytes)<br>
   *                      C type : U32<br>
   * @param BytesDesc_2   Max transfer size is 23 bits<br>
   *                      C type : U32<br>
   * @param BytesDesc_3   such that max U32 transfer size<br>
   *                      C type : U32<br>
   * @param BytesDesc_4   is (2097151) 0x1FFFFF<br>
   *                      C type : U32<br>
   * @param PhyAddrDesc_1 Valid PHYSICAL address for contiguous<br>
   *                      C type : U64<br>
   * @param PhyAddrDesc_2 memory block<br>
   *                      C type : U64<br>
   * @param PhyAddrDesc_3 DO NOT use an address obtained from<br>
   *                      C type : U64<br>
   * @param PhyAddrDesc_4 malloc() or similiar functions<br>
   *                      C type : U64<br>
   * @param Rsvrd         C type : U64
   */
  public GS_DMA_DESCRIPTOR(NativeLong BytesDesc_1, NativeLong BytesDesc_2, NativeLong BytesDesc_3, NativeLong BytesDesc_4, U64 PhyAddrDesc_1, U64 PhyAddrDesc_2, U64 PhyAddrDesc_3, U64 PhyAddrDesc_4, U64 Rsvrd)
  {
    super();
    this.BytesDesc_1 = BytesDesc_1;
    this.BytesDesc_2 = BytesDesc_2;
    this.BytesDesc_3 = BytesDesc_3;
    this.BytesDesc_4 = BytesDesc_4;
    this.PhyAddrDesc_1 = PhyAddrDesc_1;
    this.PhyAddrDesc_2 = PhyAddrDesc_2;
    this.PhyAddrDesc_3 = PhyAddrDesc_3;
    this.PhyAddrDesc_4 = PhyAddrDesc_4;
    this.Rsvrd = Rsvrd;
  }

  public GS_DMA_DESCRIPTOR(Pointer peer)
  {
    super(peer);
  }

  public static abstract class ByReference extends GS_DMA_DESCRIPTOR implements Structure.ByReference
  {

  }

  ;

  public static abstract class ByValue extends GS_DMA_DESCRIPTOR implements Structure.ByValue
  {

  }

  ;
}
