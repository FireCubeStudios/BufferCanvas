package com.example.canvastest.osm;

import java.util.HashSet;
import java.util.Set;

public class PolygonHelper {
    public int[] fill(int[] vertices)
    {
        int minX = vertices[0], maxX = vertices[0];
        int minY = vertices[1], maxY = vertices[1];
        Set<Integer> points = new HashSet<Integer>();
        for(int i = 2; i < vertices.length - 2; i += 2) // find bounds
        {
            int x = vertices[i];
            int y = vertices[i + 1];
            if (x < minX) minX = x;
            else if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            else if (y > maxY) maxY = y;


        }

       //  LineHelper.line(x, y, x2, y2);

        for(int i = minY; i < maxY; i++) // Draw scan lines
        {
            boolean isDrawing = false; // Is a line being generated
            for(int ii = minX; ii < maxX; ii++) // Draw scan lines
            {

            }
        }
        int[] lines = new int[(maxX - minX) * 2];
        return lines;
    }
}
