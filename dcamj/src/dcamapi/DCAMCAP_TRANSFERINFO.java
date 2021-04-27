package dcamapi;

import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.CLong;
import org.bridj.ann.Field;
import org.bridj.ann.Library;

/**
 * <i>native declaration : lib\dcam\inc\dcamapi.h:423</i><br>
 * This file was autogenerated by
 * <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that
 * <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a
 * few opensource projects.</a>.<br>
 * For help, please visit
 * <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or
 * <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("dcamapi")
public class DCAMCAP_TRANSFERINFO extends StructObject
{
  public DCAMCAP_TRANSFERINFO()
  {
    super();
  }

  // / [in]
  @CLong
  @Field(0)
  public long size()
  {
    return this.io.getCLongField(this, 0);
  }

  // / [in]
  @CLong
  @Field(0)
  public DCAMCAP_TRANSFERINFO size(final long size)
  {
    this.io.setCLongField(this, 0, size);
    return this;
  }

  // / [in]
  @CLong
  @Field(1)
  public long reserved()
  {
    return this.io.getCLongField(this, 1);
  }

  // / [in]
  @CLong
  @Field(1)
  public DCAMCAP_TRANSFERINFO reserved(final long reserved)
  {
    this.io.setCLongField(this, 1, reserved);
    return this;
  }

  // / [out]
  @CLong
  @Field(2)
  public long nNewestFrameIndex()
  {
    return this.io.getCLongField(this, 2);
  }

  // / [out]
  @CLong
  @Field(2)
  public DCAMCAP_TRANSFERINFO nNewestFrameIndex(final long nNewestFrameIndex)
  {
    this.io.setCLongField(this, 2, nNewestFrameIndex);
    return this;
  }

  // / [out]
  @CLong
  @Field(3)
  public long nFrameCount()
  {
    return this.io.getCLongField(this, 3);
  }

  // / [out]
  @CLong
  @Field(3)
  public DCAMCAP_TRANSFERINFO nFrameCount(final long nFrameCount)
  {
    this.io.setCLongField(this, 3, nFrameCount);
    return this;
  }

  public DCAMCAP_TRANSFERINFO(final Pointer pointer)
  {
    super(pointer);
  }
}
