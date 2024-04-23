package com.example.canvastest;

import com.aparapi.Kernel;

public class FixedPolygonKernel extends Kernel {
    public int[] buffer;
    public int[] BACKGROUND; // 0 = sea, 1 = land

    public int[] lines; // format x1, x2, y1, y2, c, (0 = None, 1 = Part of polygon)
    public int[] SCANPOLYGON; // Stores points of visible polygons used for scanline filling
    public int[] SCANCOLOURS; // Stores colours for scanline polygon filling algorithm stack

    public int[] mode = new int[1]; // 0 = draw background, 1 = draw lines, 2 = scanline fill
    public int[] size = new int[2]; // 0 = width, 1 = height

    public int[] transformedSize = new int[2]; // 0 = width, 1 = height
    public double[] transform = new double[4];

    public FixedPolygonKernel(int[] buffer, int[] BACKGROUND, int width, int height) {
        this.buffer = buffer;
        this.BACKGROUND = BACKGROUND;
        this.lines = new int[10 * 2];
        this.SCANPOLYGON = new int[buffer.length];
        this.SCANCOLOURS = new int[width * 100];
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
        put(this.SCANCOLOURS);
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
        put(this.size);
        put(this.transformedSize);
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
            int lineIndex = i * 6;
            int x1 = (int) ((lines[lineIndex] * transform[2]) + transform[0]);
            int y1 = (int) ((lines[lineIndex + 1] * transform[3]) + transform[1]);
            int x2 = (int) ((lines[lineIndex + 2] * transform[2]) + transform[0]);
            int y2 = (int) ((lines[lineIndex + 3] * transform[3]) + transform[1]);
            int colour = lines[lineIndex + 4];
            int midX = (x1 + x2) / 2;
            int midY = (y1 + y2) / 2;
            // The reason y < -300 is to draw top level things flat for filling
            if((x1 > 0 && x1 < size[0] && y1 > -300 && y1 < size[1])
                    || (x2 > 0 && x2 < size[0] && y2 > -300 && y2 < size[1])
                    || (midX > 0 && midX < size[0] && midY > -300 && midY < size[1])){
                int dx = Math.abs(x2 - x1);
                int dy = Math.abs(y2 - y1);
                int sx = (x1 < x2) ? 1 : -1;
                int sy = (y1 < y2) ? 1 : -1;
                int err = dx - dy;
                int x = x1;
                int y = y1;

                while (x != x2 || y != y2) {
                    if (x > 0 && y > -300 && x < size[0] && y < size[1]) {
                        buffer[((x % size[0]) + (y * size[0]))] = colour;
                        if(lines[lineIndex + 5] == 1) { // Part of polygon
                            if(y > 0) {
                                SCANPOLYGON[((x % size[0]) + (y * size[0]))] = colour;
                            }
                            else {
                                SCANPOLYGON[((x % size[0]) + (0 * size[0]))] = colour;
                            }
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
        // Scanline fill polygon
        else if(mode[0] == 2){
            int row = -1;
            int colourIndex = i * 10;
            SCANCOLOURS[colourIndex] = buffer[((i * size[0]))]; // set first colour to first row

            boolean startTracking = false;
            boolean endTracking = false;
            boolean startFilling = false; /* sometimes a polygon border can take multiple pixel rows so we track until it ends then start filling*/
            while(row < size[1])
            {
                row += 1;
                int position = ((i % size[0]) + (row * size[0]));
                // new polygon encountered
                if(SCANPOLYGON[position] != SCANCOLOURS[colourIndex] && SCANPOLYGON[position] != 0 && !startFilling && !startTracking && !endTracking)
                {
                    startTracking = true;
                    buffer[position] = SCANPOLYGON[position];
                    colourIndex = colourIndex + 1;
                    SCANCOLOURS[colourIndex] = SCANPOLYGON[position];
                }
                // Track until end of start border found
                else if((SCANPOLYGON[position] != SCANCOLOURS[colourIndex] || SCANPOLYGON[position] == 0) && !startFilling && startTracking && !endTracking)
                {
                    startFilling = true;
                    startTracking = false;
                    buffer[position] = SCANCOLOURS[colourIndex];
                }
                // start of end border of current polygon encountered
                else if(colourIndex > (i * 10) && SCANPOLYGON[position] == SCANCOLOURS[colourIndex] && startFilling && !startTracking && !endTracking)
                {
                    startFilling = false;
                    endTracking = true;
                    buffer[position] = SCANCOLOURS[colourIndex];
                }
                // Track until end of end border found
                else if(colourIndex > (i * 10) && (SCANPOLYGON[position] != SCANCOLOURS[colourIndex] || SCANPOLYGON[position] == 0) && !startFilling && !startTracking && endTracking)
                {
                    endTracking = false;
                    buffer[position] = SCANPOLYGON[position];
                    colourIndex = colourIndex - 1;
                }
                // filling polygon
                else if(colourIndex > (i * 10) && startFilling && !startTracking && !endTracking)
                {
                    buffer[position] = SCANCOLOURS[colourIndex];
                }
            }
        }
        else if(mode[0] == 3)
        {
            SCANCOLOURS[i] = 1;
        }
        else if(mode[0] == 4){
            int col = 0;
            int colourIndex = i * 10;
            SCANCOLOURS[colourIndex] = buffer[((i % size[0]))]; // set first colour to first column
            boolean startTracking = false;
            boolean endTracking = false;
            boolean startFilling = false; /* sometimes a polygon border can take multiple pixel rows so we track until it ends then start filling*/
            while(col < size[0])
            {
                col += 1;
                int position = ((col % size[0]) + (i * size[0]));
                // new polygon encountered
                if(SCANPOLYGON[position] != SCANCOLOURS[colourIndex] && SCANPOLYGON[position] != 0 && !startFilling && !startTracking && !endTracking)
                {
                    startTracking = true;
                    buffer[position] = SCANPOLYGON[position];
                    colourIndex = colourIndex + 1;
                    SCANCOLOURS[colourIndex] = SCANPOLYGON[position];
                }
                // Track until end of start border found
                else if((SCANPOLYGON[position] != SCANCOLOURS[colourIndex] || SCANPOLYGON[position] == 0) && !startFilling && startTracking && !endTracking)
                {
                    startFilling = true;
                    startTracking = false;
                    buffer[position] = SCANCOLOURS[colourIndex];
                }
                // start of end border of current polygon encountered
                else if(colourIndex > (i * 10) && SCANPOLYGON[position] == SCANCOLOURS[colourIndex] && startFilling && !startTracking && !endTracking)
                {
                    startFilling = false;
                    endTracking = true;
                    buffer[position] = SCANCOLOURS[colourIndex];
                }
                // Track until end of end border found
                else if(colourIndex > (i * 10) && (SCANPOLYGON[position] != SCANCOLOURS[colourIndex] || SCANPOLYGON[position] == 0) && !startFilling && !startTracking && endTracking)
                {
                    endTracking = false;
                    buffer[position] = SCANPOLYGON[position];
                    colourIndex = colourIndex - 1;
                }
                // filling polygon
                else if(colourIndex > (i * 10) && startFilling && !startTracking && !endTracking) {
                    buffer[position] = SCANCOLOURS[colourIndex];
                }
            }
        }
        // Draw background to clear canvas and scanned polygons
        else{
            if (i < buffer.length) {
                buffer[i] = BACKGROUND[0];
                SCANPOLYGON[i] = 0;
            }
        }
    }
}