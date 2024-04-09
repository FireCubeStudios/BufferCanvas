package com.example.canvastest.osm;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;

public class LineHelper {
    public static int line(int x1, int y1, int x2, int y2, int i, int[] xPoints, int[] yPoints, int[] cPoints, int colour)
    {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        // Determine the direction of the line
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        // Initialize the error variable
        int err = dx - dy;
        int x = x1;
        int y = y1;

        // Loop through all points along the line using Bresenham's algorithm
        while (true) {
            if(i > xPoints.length - 100) break;
            xPoints[i] = x;
            yPoints[i] = y;
            cPoints[i] = colour;
            i++;

            if ((x == x2 && y == y2) || i > xPoints.length - 100)
                break;

            int e2 = 2 * err;

            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }

            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
        return i;
    }
    public static int[] line(int x1, int y1, int x2, int y2)
    {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        // Determine the direction of the line
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        // Calculate the length of the line
        int length = Math.max(dx, dy) + 1; // Maximum possible length of the line

        int[] cacheArray = new int[length * 2]; // Pre-allocate array to hold x, y pairs

        // Initialize the error variable
        int err = dx - dy;
        int x = x1;
        int y = y1;

        // Index to keep track of where to insert points in the cacheArray
        int index = 0;

        // Loop through all points along the line using Bresenham's algorithm
        while (true) {
            cacheArray[index++] = x;
            cacheArray[index++] = y;

            if (x == x2 && y == y2)
                break;

            int e2 = 2 * err;

            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }

            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }

        // Trim the array to remove unused elements
        return Arrays.copyOf(cacheArray, index);
    }
}
