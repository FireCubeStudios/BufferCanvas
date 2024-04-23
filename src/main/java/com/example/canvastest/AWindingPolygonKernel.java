package com.example.canvastest;

import com.aparapi.Kernel;

public class AWindingPolygonKernel extends Kernel {
    public int[] buffer;
    public int[] BACKGROUND; // 0 = sea, 1 = land

    public int[] lines; // format x1, x2, y1, y2, c, (0 = None, 1 = Part of polygon)
    public int[] SCANPOLYGON; // Stores points of visible polygons used for scanline filling
    public int[] WindingDown; // Stores windings that go down of all polygon lines at the position
    public int[] WindingUp; // Stores windings that go up of all polygon lines at the position

    public int[] mode = new int[1]; // 0 = draw background, 1 = draw lines, 2 = scanline fill
    public int[] size = new int[2]; // 0 = width, 1 = height

    public int[] transformedSize = new int[2]; // 0 = width, 1 = height
    public double[] transform = new double[4];

    public AWindingPolygonKernel(int[] buffer, int[] BACKGROUND, int width, int height) {
        this.buffer = buffer;
        this.BACKGROUND = BACKGROUND;
        this.lines = new int[10 * 2];
        this.SCANPOLYGON = new int[buffer.length];
        this.WindingDown = new int[buffer.length];
        this.WindingUp = new int[buffer.length];
        this.mode[0] = 0;
        this.size[0] = width;
        this.size[1] = height;

        setExplicit(true);
        put(this.size);
        put(this.transformedSize);
        put(this.buffer);
        put(this.lines);
        put(this.BACKGROUND);
        put(this.SCANPOLYGON);
        put(this.WindingDown);
        put(this.WindingUp);
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

    public void resize(int[] BUFFER, int width, int height)
    {
        this.size[0] = width;
        this.size[1] = height;
        this.transformedSize[0] = (int) ((this.size[0] * transform[2]) + transform[0]);
        this.transformedSize[1] = (int) ((this.size[0] * transform[3]) + transform[1]);
        this.buffer = BUFFER;
        this.SCANPOLYGON = new int[buffer.length];
        this.WindingDown = new int[buffer.length];
        this.WindingUp = new int[buffer.length];
        put(this.size);
        put(this.transformedSize);
        put(this.buffer);
        put(this.SCANPOLYGON);
        put(this.WindingDown);
        put(this.WindingUp);
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
            int lineIndex = i * 6;
            int x1 = (int) ((lines[lineIndex] * transform[2]) + transform[0]);
            int y1 = (int) ((lines[lineIndex + 1] * transform[3]) + transform[1]);
            int x2 = (int) ((lines[lineIndex + 2] * transform[2]) + transform[0]);
            int y2 = (int) ((lines[lineIndex + 3] * transform[3]) + transform[1]);
            int colour = lines[lineIndex + 4];
            int midX = (x1 + x2) / 2;
            int midY = (y1 + y2) / 2;
            if((x1 > 0 && x1 < size[0] && y1 > 0 && y1 < size[1])
                    || (x2 > 0 && x2 < size[0] && y2 > 0 && y2 < size[1])
                    || (midX > 0 && midX < size[0] && midY > 0 && midY < size[1])){
                int dx = Math.abs(x2 - x1);
                int dy = Math.abs(y2 - y1);
                int sx = (x1 < x2) ? 1 : -1;
                int sy = (y1 < y2) ? 1 : -1;
                int err = dx - dy;
                int x = x1;
                int y = y1;

                while (x != x2 || y != y2) {
                    if (x > 0 && y > 0 && x < size[0] && y < size[1]) {
                        buffer[((x % size[0]) + (y * size[0]))] = colour;
                        if(lines[lineIndex + 5] == 1) { // Part of polygon
                            SCANPOLYGON[((x % size[0]) + (y * size[0]))] = colour;
                            if (y2 > y1)
                                WindingUp[((x % size[0]) + (y * size[0]))] = 1; // Sloping up
                            else
                                WindingDown[((x % size[0]) + (y * size[0]))] = 1; // Sloping down
                        }
                    }

                    int err2 = 2 * err;
                    if (err2 > -dy) {
                        err -= dy;
                        x += sx;
                    }
                    if (err2 < dx) {
                        err += dx;
                        y += sy;
                    }
                }
            }
        }
        else if(mode[0] == 2){
            int x = 0;
            int y = i;
            int windings = 0;
            int position = 0;
            while(x < size[0]) {
                x += 1;
                position = ((x % size[0]) + (y * size[0]));
                if(WindingUp[position] == WindingDown[position] && WindingDown[position] != 0 && WindingUp[position] != 0) {
                    windings = 0;
                    x += 10;
                }
                else if(WindingUp[position] == 1)
                    windings = windings + 1;
                else if(WindingDown[position] == 1)
                    windings = windings - 1;

                if(windings != 0) // inside polygon
                {
                    buffer[position] = 0;
                }
            }
        }
        // Draw background to clear canvas and scanned polygons
        else if(mode[0] == 0){
            if (i < buffer.length) {
                buffer[i] = BACKGROUND[0];
                SCANPOLYGON[i] = 0;
                WindingDown[i] = 0;
                WindingUp[i] = 0;
            }
        }
    }
}
