package com.example.audiorecorder;

import java.nio.ByteBuffer;

import fm.audioboo.jni.FLACStreamEncoder;

/**
 * Created by chenkai on 2017/1/6.
 */

public class FLACEncoder implements Encoder, IAmplitude {

    private final FLACStreamEncoder mRealEncoder;

    public FLACEncoder(String fileName, int sampleRate, int channels, int bps) {
        mRealEncoder = new FLACStreamEncoder(fileName + ".flac", sampleRate, channels, bps);
    }

    @Override
    public int encode(ByteBuffer data, int length) {
        return mRealEncoder.write(data, length);
    }

    @Override
    public float getMaxAmplitude() {
        return mRealEncoder.getMaxAmplitude();
    }

    @Override
    public float getAverageAmplitude() {
        return mRealEncoder.getAverageAmplitude();
    }

    @Override
    public void flush() {
    	mRealEncoder.flush();
    }

    @Override
    public void release() {
    	mRealEncoder.release();
    }
}
