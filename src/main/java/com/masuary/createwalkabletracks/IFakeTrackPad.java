package com.masuary.createwalkabletracks;

public interface IFakeTrackPad {
    void cwt$setPadRange(float padMinY, float padMaxY);

    float cwt$getPadMinY();

    float cwt$getPadMaxY();

    boolean cwt$isPadSet();
}
