package com.example.canvastest;

import com.aparapi.Kernel;

public class NEWPixelKernel  extends Kernel {
    public int[] buffer;
    public int[] BACKGROUND;

    public int[] points;
    public int[] pointColorARGB;
    public int[] mode = new int[1]; // 0 = draw background, 1 = draw points
    public int[] size = new int[2]; // 0 = width, 1 = height
    public int[] transformedSize = new int[2]; // 0 = width, 1 = height
    public double[] transform = new double[4];
    // 0 = x translate, 1 = y translate, 2 = scaleX, 3 = scaleY

    public NEWPixelKernel(int[] buffer, int[] BACKGROUND, int pointColorARGB, int width, int height) {
        this.buffer = buffer;
        this.BACKGROUND = BACKGROUND;
        this.pointColorARGB[0] = pointColorARGB;
        this.points = new int[10 * 2];
        this.mode[0] = 0;
        this.size[0] = width;
        this.size[1] = height;

        setExplicit(true);
        put(this.size);
        put(this.transformedSize);
        put(this.pointColorARGB);
        put(this.buffer);
        put(this.points);
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

    public void setPoints(int[] points) {
        this.points = points;
        put(this.points);
    }

    public void setMode(int mode) {
        this.mode[0] = mode;
        put(this.mode);
    }

    @Override
    public void run() {
        int i = getGlobalId();

        if(mode[0] == 1) {
            if (i < buffer.length && i % 2 == 0) {
                /*int x = (int)((points[i] * transform[2]) + transform[0]);
                int y = (int)((points[i + 1] * transform[3]) + transform[1]);
                if(x > 0 && y > 0 && x < size[0] && y < size[1])
                    buffer[((x % size[0])
                            + ((y) % size[1]) * size[0])]
                            = pointColorARGB[0];


                                if(((int)((points[i] * transform[2]) + transform[0])) > 0
                        && ((int)((points[i + 1] * transform[3]) + transform[1])) > 0
                        && ((int)((points[i] * transform[2]) + transform[0])) < size[0]
                        && ((int)((points[i + 1] * transform[3]) + transform[1])) < size[1])
                    buffer[((((int)((points[i] * transform[2]) + transform[0])) % size[0])
                            + (((int)((points[i + 1] * transform[3]) + transform[1])) % size[1]) * size[0])]
                            = pointColorARGB[0];*/

                /*
                x = i * 2
                y = (i * 2) + 1
                 */
                if(transform[2] > 1 && transform[3] > 1) {
                   // special scaling code
                }
                else {
                    if (((int) ((points[i * 2] * transform[2]) + transform[0])) > 0
                            && ((int) ((points[(i * 2) + 1] * transform[3]) + transform[1])) > 0
                            && ((int) ((points[i * 2] * transform[2]) + transform[0])) < size[0]
                            && ((int) ((points[(i * 2) + 1] * transform[3]) + transform[1])) < size[1])
                        buffer[((((int) ((points[i * 2] * transform[2]) + transform[0])) % size[0])
                                + ((int) ((points[(i * 2) + 1] * transform[3]) + transform[1])) * size[0])]
                                = pointColorARGB[0];
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
