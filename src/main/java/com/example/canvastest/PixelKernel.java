package com.example.canvastest;

import com.aparapi.Kernel;

public class PixelKernel extends Kernel {
    public int[] backgroundBuffer;

    public int[] points;
    public int colorARGB;
    public int pointColorARGB;

    // 0 = draw background, 1 = draw points
    public int mode = 0;

    public PixelKernel(int[] background, int colorARGB, int pointColorARGB) {
        this.backgroundBuffer = background;
        this.colorARGB = colorARGB;
        this.pointColorARGB = pointColorARGB;
        this.points = new int[10 * 2];
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setBackground(int[] background, int colorARGB) {
        this.backgroundBuffer = background;
        this.colorARGB = colorARGB;
    }

    public void setPoints(int[] points, int colorARGB) {
        this.points = points;
        this.pointColorARGB = colorARGB;
    }

    @Override
    public void run() {
        int i = getGlobalId();

        // draw pixel
        if(mode == 1) {
            if (i < backgroundBuffer.length - 1) {
                backgroundBuffer[(points[i] % 1280) + (points[i + 1] * 1280)] = pointColorARGB;
            }
        }
        // Draw background to clear canvas
        else if(mode == 0) {
            if (i < backgroundBuffer.length) {
                backgroundBuffer[i] = colorARGB;
            }
        }
    }
}
