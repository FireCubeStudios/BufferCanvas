package com.example.canvastest;

import com.aparapi.Kernel;

public class LinePixelKernel extends Kernel {
    public int[] buffer;
    public int[] BACKGROUND;

    public int[] lines; // format x1, x2, y1, y2, c

    public int[] mode = new int[1]; // 0 = draw background, 1 = draw points
    public int[] size = new int[2]; // 0 = width, 1 = height
    public int[] transformedSize = new int[2]; // 0 = width, 1 = height
    public double[] transform = new double[4];
    // 0 = x translate, 1 = y translate, 2 = scaleX, 3 = scaleY

    public LinePixelKernel(int[] buffer, int[] BACKGROUND, int width, int height) {
        this.buffer = buffer;
        this.BACKGROUND = BACKGROUND;
        this.lines = new int[10 * 2];
        this.mode[0] = 0;
        this.size[0] = width;
        this.size[1] = height;

        setExplicit(true);
        put(this.size);
        put(this.transformedSize);
        put(this.buffer);
        put(this.lines);
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

    public void setLines(int[] lines) {
        this.lines = lines;
        put(this.lines);
    }

    public void setMode(int mode) {
        this.mode[0] = mode;
        put(this.mode);
    }
    @Override
    public void run() {
        int i = getGlobalId(0);

        if(mode[0] == 1) {
            if (i < buffer.length) {
                // Check if x1, or x2, or y1, or y2 in bounds
                if(((((lines[i * 5] * transform[2]) + transform[0]) > 0) && (((lines[i * 5] * transform[2]) + transform[0]) < size[0]))
                || ((((lines[(i * 5) + 1] * transform[3]) + transform[1]) > 0) && (((lines[(i * 5) + 1] * transform[3]) + transform[1]) < size[1]))
                || ((((lines[(i * 5) + 2] * transform[2]) + transform[0]) > 0) && (((lines[(i * 5) + 2] * transform[2]) + transform[0]) < size[0]))
                || ((((lines[(i * 5) + 3] * transform[3]) + transform[1]) > 0) && (((lines[(i * 5) + 3] * transform[3]) + transform[1]) < size[1])))
                {
                    buffer[((((int)((lines[i * 5] * transform[2]) + transform[0])) % size[0])
                            + ((int) ((lines[(i * 5) + 1] * transform[3]) + transform[1])) * size[0])]
                            = lines[(i * 5) + 4];

                  /*  int dx = (int) (((lines[(i * 5) + 2] * transform[2]) + transform[0]) - ((lines[i * 5] * transform[2]) + transform[0]));
                    if (dx < 0) {
                        dx = -dx;
                    }

                    int dy = (int) (((lines[(i * 5) + 3] * transform[3]) + transform[1]) - ((lines[(i * 5) + 1] * transform[3]) + transform[1]));
                    if (dy < 0) {
                        dy = -dy;
                    }*/


                    // Determine the direction of the line
                    int sx = ((lines[i * 5] * transform[2]) + transform[0]) < ((lines[(i * 5) + 2] * transform[2]) + transform[0]) ? 1 : -1;
                    int sy = ((lines[(i * 5) + 1] * transform[3]) + transform[1]) < ((lines[(i * 5) + 3] * transform[3]) + transform[1]) ? 1 : -1;

                    // Initialize the error variable
                  //  int err = dx - dy;
                    int x = (int) ((lines[i * 5] * transform[2]) + transform[0]);
                    int y = (int) ((lines[(i * 5) + 1] * transform[3]) + transform[1]);

                    // Loop through all points along the line using Bresenham's algorithm
                    int b = 0;
              /*      while (b == 0) {
                        buffer[(x % size[0])
                                + (y * size[0])]
                                = lines[(i * 5) + 4];

                        if (x == ((lines[(i * 5) + 2] * transform[2]) + transform[0]) && y == ((lines[(i * 5) + 3] * transform[3]) + transform[1]))
                            b = 1;

                        int e2 = 2 * err;

                        if (e2 > -dy) {
                            err -= dy;
                            x += sx;
                        }

                        if (e2 < dx) {
                            err += dx;
                            y += sy;
                        }
                    }*/
                }
            }
        }

        // Draw background to clear canvas
        else{
            if (i < BACKGROUND.length) {
                buffer[i] = BACKGROUND[0];
            }
        }
    }
}
