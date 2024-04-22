package com.example.canvastest;

import com.aparapi.Kernel;

public class PolygonKernel extends Kernel {
    public int[] buffer;
    public int[] BACKGROUND; // 0 = sea, 1 = land

    public int[] lines; // format x1, x2, y1, y2, c, (0 = None, 1 = Part of polygon)
    public int[] SCANPOLYGON; // Stores points of visible polygons used for scanline filling
    public int[] mode = new int[1]; // 0 = draw background, 1 = draw lines, 2 = scanline fill
    public int[] size = new int[2]; // 0 = width, 1 = height

    public int[] transformedSize = new int[2]; // 0 = width, 1 = height
    public double[] transform = new double[4];

    public PolygonKernel(int[] buffer, int[] BACKGROUND, int width, int height) {
        this.buffer = buffer;
        this.BACKGROUND = BACKGROUND;
        this.lines = new int[10 * 2];
        this.SCANPOLYGON = new int[(width * height) * 2];
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
            int lineIndex = i * 5;
            int x1 = (int) ((lines[lineIndex] * transform[2]) + transform[0]);
            int y1 = (int) ((lines[lineIndex + 1] * transform[3]) + transform[1]);
            int x2 = (int) ((lines[lineIndex + 2] * transform[2]) + transform[0]);
            int y2 = (int) ((lines[lineIndex + 3] * transform[3]) + transform[1]);
            int colour = lines[lineIndex + 4];
            if((x1 > 0 && x1 < size[0] && y1 > 0 && y1 < size[1]) || (x2 > 0 && x2 < size[0] && y2 > 0 && y2 < size[1])){
                int dx = Math.abs(x2 - x1);
                int dy = Math.abs(y2 - y1);
                int sx = (x1 < x2) ? 1 : -1;
                int sy = (y1 < y2) ? 1 : -1;
                int err = dx - dy;
                int x = x1;
                int y = y1;

                while (x != x2 || y != y2) {
                    if (x > 0 && y > 0 && x < size[0] && y < size[1]) {
                        buffer[((x % size[0])
                                + (y * size[0]))]
                                = colour;
                        if(lines[lineIndex + 5] == 1) // Part of polygon
                        {
                            SCANPOLYGON[((x % size[0])
                                    + (y * size[0]))]
                                    = colour;
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
            int oldColour = buffer[((i * size[0]))]; // set old colour to first row
            int currentColour = buffer[((i * size[0]))]; // set current colour to first row
            boolean isDrawing = false;
            while(row < 700)
            {
                row += 1;
                int position = ((row % size[0]) + (i * size[0]));
                if(SCANPOLYGON[position] != currentColour && SCANPOLYGON[position] != 0) { // new polygon encountered?
                    buffer[position] = currentColour;
                    currentColour = SCANPOLYGON[position];
                    isDrawing = true;
                }
                else {
                    buffer[position] = currentColour;
                    currentColour = buffer[position];
                }
            }
        }
        // Draw background to clear canvas
        // Clear scan polygons too?
        else{
            if (i < buffer.length) {
                buffer[i] = BACKGROUND[0];
                SCANPOLYGON[i] = 0;
            }
        }
    }
}
/*
else if(mode[0] == 2){
            int row = -1;
            int oldColour = buffer[((i * size[0]))]; // set old colour to first row
            int currentColour = buffer[((i * size[0]))]; // set current colour to first row
            boolean isDrawing = false;
            while(row < 700)
            {
                row += 1;
                int position = ((row % size[0]) + (i * size[0]));
                if(isDrawing)
                {
                    if(SCANPOLYGON[position] != currentColour && SCANPOLYGON[position] != 0) { // end of polygon encountered
                        buffer[position] = currentColour;
                        currentColour = oldColour;
                        isDrawing = false;
                    }
                    else {
                        buffer[position] = currentColour;
                    }
                }
                else {
                    if(SCANPOLYGON[position] != currentColour && SCANPOLYGON[position] != 0) { // new polygon encountered?
                        buffer[position] = currentColour;
                        currentColour = SCANPOLYGON[position];
                        isDrawing = true;
                    }
                    else {
                        buffer[position] = currentColour;
                        currentColour = buffer[position];
                    }
                }
            }
        }
 */