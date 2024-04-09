package com.example.canvastest;

import com.aparapi.Kernel;

public class NEWERPIXELKERNEL extends Kernel {
    public int[] buffer;
    public int[] BACKGROUND;

    public int[] lines;

    public int[] xPoints;
    public int[] yPoints;
    public int[] cPoints;

    public int[] mode = new int[1]; // 0 = draw background, 1 = draw points
    public int[] size = new int[2]; // 0 = width, 1 = height
    public int[] transformedSize = new int[2]; // 0 = width, 1 = height
    public double[] transform = new double[4];
    // 0 = x translate, 1 = y translate, 2 = scaleX, 3 = scaleY

    public NEWERPIXELKERNEL(int[] buffer, int[] BACKGROUND, int width, int height) {
        this.buffer = buffer;
        this.BACKGROUND = BACKGROUND;
        this.xPoints = new int[10 * 2];
        this.yPoints = new int[10 * 2];
        this.cPoints = new int[10 * 2];
        this.mode[0] = 0;
        this.size[0] = width;
        this.size[1] = height;

        setExplicit(true);
        put(this.size);
        put(this.transformedSize);
        put(this.buffer);
        put(this.xPoints);
        put(this.yPoints);
        put(this.cPoints);
        put(this.BACKGROUND);
    }

    public void setTransform(Transform matrix) {
        this.transform[2] = matrix.a;
        this.transform[3] = matrix.d;
        this.transform[0] = matrix.e;
        this.transform[1] = matrix.f;
        this.transformedSize[0] = (int) ((this.size[0] * transform[2]) + transform[0]);
        this.transformedSize[1] = (int) ((this.size[0] * transform[3]) + transform[1]);
        put(this.transform);
        put(this.transformedSize);
    }

    public void resize(int[] BUFFER, int[] BACKGROUND, int width, int height)
    {
        this.size[0] = width;
        this.size[1] = height;
        this.transformedSize[0] = (int) ((this.size[0] * transform[2]) + transform[0]);
        this.transformedSize[1] = (int) ((this.size[0] * transform[3]) + transform[1]);
        this.BACKGROUND = BACKGROUND;
        this.buffer = BUFFER;
        put(this.size);
        put(this.transformedSize);
        put(this.BACKGROUND);
        put(this.buffer);
    }

    public void setPoints(int[] x, int[] y, int[] c) {
        this.xPoints = x;
        this.yPoints = y;
        this.cPoints = c;
        put(this.xPoints);
        put(this.yPoints);
        put(this.cPoints);
    }

    public void setMode(int mode) {
        this.mode[0] = mode;
        put(this.mode);
    }

    /*          int transformedX = (x * scaleX) + translateX;
                int transformedY = (y * scaleY) + translateY;
        pixels.set(transformedX, transformedY);     */
    @Override
    public void run() {
        int i = getGlobalId(0);

        if(mode[0] == 1) {
            if (cPoints[i] != 0) {
                if ((((xPoints[i] * transform[2]) + transform[0])) > 0
                        && (((yPoints[i] * transform[3]) + transform[1])) > 0
                        && (((xPoints[i] * transform[2]) + transform[0])) < size[0]
                        && (((yPoints[i] * transform[3]) + transform[1])) < size[1]) {

                 /*   if(transform[2] > 1 && transform[3] > 1) {
                            int t = -((int) (transform[2] / 2));
                            int t2 = (int) (transform[2] / 2);
                            for (int px = t; px <= t2; px += 1) {
                                for (int py = t; py <= t2; py += 1) {
                                    if ((((xPoints[i] * transform[2]) + transform[0]) + px) >= 0
                                            && (((xPoints[i] * transform[2]) + transform[0]) + px) < size[0]
                                            && (((yPoints[i] * transform[3]) + transform[1]) + py) >= 0
                                            && (((yPoints[i] * transform[3]) + transform[1]) + py) < size[1]) {
                                        buffer[(((int) (((xPoints[i] * transform[2]) + transform[0]) + px) % size[0])
                                                + (int) (((yPoints[i] * transform[3]) + transform[1]) + py) * size[0])]
                                                = cPoints[i];
                                    }
                                }
                            }
                        }*/
                       // else
                            buffer[((((int)((xPoints[i] * transform[2]) + transform[0])) % size[0])
                                    + ((int) ((yPoints[i] * transform[3]) + transform[1])) * size[0])]
                                    = cPoints[i];
                }
            }
        }

        // Draw background to clear canvas
        else if(mode[0] == 0) {
            if (i < BACKGROUND.length) {
                buffer[i] = BACKGROUND[0];
            }
        }
    }
}