package com.example.canvastest.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolygonHelper {
    // Return the points which make up the filled polygon
    public static int[] polygon(int[] vertices) // input: vertices of polygon in format x, y, x, y, x, y
    {
    List<Integer> pointsInside = new ArrayList<>();

    // Extract vertices coordinates
    int n = vertices.length;
    int[] xCoords = new int[n / 2];
    int[] yCoords = new int[n / 2];
        for (int i = 0; i < n; i += 2) {
        xCoords[i / 2] = vertices[i];
        yCoords[i / 2] = vertices[i + 1];
    }

    // Find min and max coordinates to determine bounds
    int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < xCoords.length; i++) {
        minX = Math.min(minX, xCoords[i]);
        maxX = Math.max(maxX, xCoords[i]);
        minY = Math.min(minY, yCoords[i]);
        maxY = Math.max(maxY, yCoords[i]);
    }

    // Ray casting algorithm
        for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
            if (isInside(x, y, xCoords, yCoords)) {
                pointsInside.add(x);
                pointsInside.add(y);
            }
        }
    }

    // Convert list to array
    int[] result = new int[pointsInside.size()];
        for (int i = 0; i < pointsInside.size(); i++) {
        result[i] = pointsInside.get(i);
    }
        return result;
}

// Helper function to check if a point is inside the polygon
private static boolean isInside(int x, int y, int[] xCoords, int[] yCoords) {
    int count = 0;
    for (int i = 0, j = xCoords.length - 1; i < xCoords.length; j = i++) {
        if ((yCoords[i] > y) != (yCoords[j] > y) &&
                x < (xCoords[j] - xCoords[i]) * (y - yCoords[i]) / (yCoords[j] - yCoords[i]) + xCoords[i]) {
            count++;
        }
    }
    return count % 2 == 1;
}


   /* public static int[] polygon(int[] vertices) // input: vertices of polygon in format x, y, x, y, x, y
    {
        // Find the min and max y-coordinates
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 1; i < vertices.length; i += 2) {
            int y = vertices[i];
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        List<Integer> lines = new ArrayList<>();

        // Iterate through each scanline
        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();

            // Find intersections with edges
            for (int i = 0; i < vertices.length; i += 2) {
                int x1 = vertices[i];
                int y1 = vertices[(i + 1) % vertices.length];
                int x2 = vertices[(i + 2) % vertices.length];
                int y2 = vertices[(i + 3) % vertices.length];

                // Check if the edge crosses the scanline
                if ((y1 <= y && y < y2) || (y2 <= y && y < y1)) {
                    // Calculate x-coordinate of intersection
                    int x = (int) (x1 + (double) (y - y1) / (y2 - y1) * (x2 - x1));
                    intersections.add(x);
                }
            }

            // Sort intersections
            intersections.sort(Integer::compareTo);

            // Add horizontal lines to result
            for (int i = 0; i < intersections.size(); i += 2) {
                lines.add(intersections.get(i));
                lines.add(y);
                lines.add(intersections.get(i + 1));
                lines.add(y);
            }
        }

        // Convert list to array
        int[] result = new int[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            result[i] = lines.get(i);
        }
        return result; // return a array of lines where each line is represented in the form: x1, y1, x2, y2 etc.
    }*/
}
