package com.example.audiorecorder;

import java.nio.ByteBuffer;


/**
 * Created by chenkai on 2017/1/6.
 */

public interface Encoder {

    public int encode(ByteBuffer data, int length);


    public void flush();

    public void release();
}
