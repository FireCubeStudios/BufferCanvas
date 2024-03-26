package com.example.canvastest;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class Polygon extends Drawable {
    public int[] xPoints;
    public int[] yPoints;

    public Polygon(int[] xPoints, int[] yPoints, Color color)
    {
        super(color);
        this.xPoints = xPoints;
        this.yPoints = yPoints;
    }

    public void draw()
    {
        ArrayList<Integer> cacheList = new ArrayList<>();

        // Find the minimum and maximum y-coordinate to determine the scanline range
        int minY = yPoints[0];
        int maxY = yPoints[0];
        for (int i = 1; i < yPoints.length; i++) {
            minY = Math.min(minY, yPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }

        // Initialize an array to store the intersection points of scanlines with polygon edges
        int[] intersections = new int[xPoints.length];

        // For each scanline from minY to maxY
        for (int y = minY; y <= maxY; y++) {
            int count = 0; // Count of intersections with the current scanline

            // Find intersections of the scanline with polygon edges
            for (int i = 0, j = xPoints.length - 1; i < xPoints.length; j = i++) {
                int yi = yPoints[i];
                int yj = yPoints[j];
                int xi = xPoints[i];
                int xj = xPoints[j];

                if ((yi < y && yj >= y) || (yj < y && yi >= y)) {
                    // Edge crosses the scanline
                    intersections[count++] = (int) (xi + (y - yi) / (double) (yj - yi) * (xj - xi));
                }
            }

            // Sort the intersection points by x-coordinate
            for (int i = 0; i < count - 1; i++) {
                for (int j = i + 1; j < count; j++) {
                    if (intersections[i] > intersections[j]) {
                        int temp = intersections[i];
                        intersections[i] = intersections[j];
                        intersections[j] = temp;
                    }
                }
            }

            // Fill the pixels between pairs of intersection points
            for (int i = 0; i < count; i += 2) {
                for (int x = intersections[i]; x <= intersections[i + 1]; x++) {
                    cacheList.add(x);
                    cacheList.add(y);
                }
            }
        }

        cache = cacheList.stream().mapToInt(Integer::intValue).toArray();
    }

    public void transform(Transform Matrix)
    {
        if(cache == null) draw();
        for(int i = 0; i < xPoints.length; i += 2)
        {
            xPoints[i] += Matrix.e;
            yPoints[i] += Matrix.f;
        }
        // draw();
        IntStream.range(0, cache.length - 1)
                .filter(i -> i % 2 == 0)
                .forEach(i -> {
                    cache[i] += Matrix.e;
                    cache[i + 1] += Matrix.f;
                });
    }
}
