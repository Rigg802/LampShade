package com.kuxhausen.huemore.net.lifx;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.kuxhausen.huemore.net.NetworkBulb;
import com.kuxhausen.huemore.persistence.Utils;
import com.kuxhausen.huemore.state.BulbState;

import lifx.java.android.entities.LFXHSBKColor;
import lifx.java.android.entities.LFXTypes;
import lifx.java.android.light.LFXLight;

public class LifxBulb implements NetworkBulb, LFXLight.LFXLightListener {

  //In milis
  private final static long TRANSMIT_TIMEOUT_TIME = 10000;

  LifxConnection mConnection;
  private Context mContext;

  private Long mBaseId;
  private String mName;
  private String mDeviceId;
  private ExtraData mExtraData;

  private LFXLight mLight;
  private long mInitializedTime;

  private BulbState mDesiredState;
  // In SystemClock.elapsedRealtime();
  private Long mDesiredLastChanged;

  private Integer mMaxBri;

  public LifxBulb(Context c, Long bulbBaseId, String bulbName,
                  String bulbDeviceId, ExtraData bulbData,
                  LifxConnection lifxConnection) {
    mBaseId = bulbBaseId;
    mName = bulbName;
    mDeviceId = bulbDeviceId;
    mExtraData = bulbData;

    mContext = c;
    mConnection = lifxConnection;
    mDesiredState = new BulbState();

  }

  protected void onInitialize() {
    mInitializedTime = SystemClock.elapsedRealtime();
  }

  public void lightConnected(LFXLight light) {
    mLight = light;
    mLight.addLightListener(this);

    if (!mDesiredState.isEmpty()) {
      setState(mDesiredState, false);
      mDesiredState = new BulbState();
    }
  }

  public void lightDisconnected() {
    if (mLight != null) {
      mLight.removeLightListener(this);
    }
    mLight = null;
  }


  @Override
  public ConnectivityState getConnectivityState() {
    if (mLight != null && mLight.getReachability()
        .equals(LFXTypes.LFXDeviceReachability.REACHABLE)) {
      return ConnectivityState.Connected;
    } else if (SystemClock.elapsedRealtime() > (mInitializedTime + this.TRANSMIT_TIMEOUT_TIME)) {
      return ConnectivityState.Unreachable;
    }
    return ConnectivityState.Unknown;
  }

  @Override
  public void setState(BulbState bs, boolean broadcast) {
    Log.d("lifx", "setState but mLight?null " + (mLight == null));

    mDesiredLastChanged = SystemClock.elapsedRealtime();

    if (mLight != null && bs != null) {
      //TODO cache so don't have to guess when SDK dosn't know
      float lifxBrightness = .5f;
      float lifxHue = 0;
      float lifxSat = 0;
      int lifxCt = 3500;
      if (mLight.getColor() != null) {
        lifxBrightness = mLight.getColor().getBrightness();
        lifxHue = mLight.getColor().getHue();
        lifxSat = mLight.getColor().getSaturation();
        lifxCt = mLight.getColor().getKelvin();
      }

      if (bs.get255Bri() != null) {
        lifxBrightness = (bs.get255Bri() / 255f) * (getMaxBrightness(true) / 100f);
      } else if (isMaxBriModeEnabled()) {
        lifxBrightness = (getMaxBrightness(true) / 100f);
      }

      //clip brightness to ensure proper behavior (0 brightness not allowed)
      lifxBrightness = Math.max(.01f, lifxBrightness);

      if (bs.getOn() != null) {
        if (bs.getOn()) {
          mLight.setPowerState(LFXTypes.LFXPowerState.ON);
        } else {
          mLight.setPowerState(LFXTypes.LFXPowerState.OFF);
        }
      }

      //Send full color, color temp, or just brightness
      if (bs.xy != null || bs.getKelvinCT() != null || bs.get255Bri() != null) {
        if (bs.xy != null) {
          Float[] hs = Utils.xyTOhs(bs.xy);
          lifxHue = 360 * hs[0];
          lifxSat = hs[1];

          LFXHSBKColor newColor = LFXHSBKColor.getColor(lifxHue, lifxSat, lifxBrightness, 3500);
          mLight.setColor(newColor);

        } else if (bs.getKelvinCT() != null) {
          lifxHue = 0;
          lifxSat = 0;
          lifxCt = bs.getKelvinCT();

          LFXHSBKColor newColor = LFXHSBKColor.getColor(0, 0, lifxBrightness, lifxCt);
          mLight.setColor(newColor);
        }

        LFXHSBKColor newColor = LFXHSBKColor.getColor(lifxHue, lifxSat, lifxBrightness, lifxCt);

        if (bs.getTransitionTime() != null) {
          mLight.setColorOverDuration(newColor, bs.getTransitionTime() * 100);
        } else {
          mLight.setColor(newColor);
        }
      }

    } else {
      //cache for when light not connected yet
      mDesiredState.merge(bs);
    }
  }

  protected boolean hasPendingWork() {
    if (mDesiredLastChanged != null
        && (mDesiredLastChanged + this.TRANSMIT_TIMEOUT_TIME) > SystemClock.elapsedRealtime()) {
      return true;
    }
    return false;
  }

  @Override
  public BulbState getState(boolean guessIfUnknown) {
    BulbState result = new BulbState();

    if (mLight != null && mLight.getColor() != null) {
      LFXHSBKColor color = mLight.getColor();
      result.set255Bri((int) ((color.getBrightness() * 255f) * (100f / getMaxBrightness(true))));
    } else if (guessIfUnknown) {
      result.setPercentBri(50);
    }

    return result;
  }

  @Override
  public Integer getMaxBrightness(boolean guessIfUnknown) {
    if (mMaxBri != null) {
      return Math.max(1, Math.min(100, mMaxBri));
    } else if (guessIfUnknown) {
      return 100;
    } else {
      return null;
    }
  }

  /**
   * @param guessIfUnknown will guess value instead of returning null if unknown
   * @result 1-100
   */
  @Override
  public Integer getCurrentBrightness(boolean guessIfUnknown) {
    if (mLight != null && mLight.getColor() != null) {
      LFXHSBKColor color = mLight.getColor();
      return (int) ((color.getBrightness() * 100f) * (100f / getMaxBrightness(true)));
    } else if (guessIfUnknown) {
      return 50;
    } else {
      return null;
    }
  }

  @Override
  public void setBrightness(Integer desiredMaxBrightness, Integer desiredCurrentBrightness) {
    Integer oldCurerntBri = this.getCurrentBrightness(false);
    Integer oldMaxBri = mMaxBri;

    boolean currentChanged = false;
    if (oldCurerntBri == null ^ desiredCurrentBrightness == null) {
      currentChanged = true;
    } else if (oldCurerntBri != null && desiredCurrentBrightness != null && !oldCurerntBri
        .equals(desiredCurrentBrightness)) {
      currentChanged = true;
    }

    boolean maxChanged = false;
    if (mMaxBri == null ^ desiredMaxBrightness == null) {
      maxChanged = true;
    } else if (mMaxBri != null && desiredMaxBrightness != null && !mMaxBri
        .equals(desiredMaxBrightness)) {
      maxChanged = true;
    }

    mMaxBri = desiredMaxBrightness;

    if (desiredMaxBrightness == null && maxChanged && desiredCurrentBrightness == null
        && currentChanged) {
      oldCurerntBri = (int) (oldCurerntBri * oldMaxBri / 100f);
    } else if (desiredCurrentBrightness != null) {
      oldCurerntBri = desiredCurrentBrightness;
    }

    if (maxChanged || currentChanged) {
      if (oldCurerntBri != null) {
        BulbState change = new BulbState();
        change.set255Bri((int) (oldCurerntBri * 2.55f));
        change.setTransitionTime(BulbState.TRANSITION_TIME_DEFAULT);
        setState(change, true);
      }
    }
  }

  @Override
  public boolean isMaxBriModeEnabled() {
    return mMaxBri != null;
  }

  @Override
  public void setState(BulbState state) {
    //TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public BulbState getState(GetStateConfidence confidence) {
    //TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    if (mLight != null) {
      return mLight.getLabel();
    }
    return "";
  }

  @Override
  public void rename(String name) {
    if (mLight != null) {
      mLight.setLabel(name);
    }
  }

  @Override
  public Long getBaseId() {
    return mBaseId;
  }

  @Override
  public void lightDidChangeLabel(LFXLight light, String label) {

  }

  @Override
  public void lightDidChangeColor(LFXLight light, LFXHSBKColor color) {

  }

  @Override
  public void lightDidChangePowerState(LFXLight light, LFXTypes.LFXPowerState powerState) {

  }

  public static class ExtraData {

  }
}