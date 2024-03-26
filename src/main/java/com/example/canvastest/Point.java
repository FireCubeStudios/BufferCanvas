package com.example.canvastest;

import javafx.scene.paint.Color;

import java.util.ArrayList;

public class Point extends Drawable {
    public int x;
    public int y;
    public int size;
    public Point(int x, int y, int size, Color color)
    {
        super(color);
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public void draw() {
        if (size == 1)
            cache = new int[]{x, y};
        else {
            ArrayList<Integer> cacheList = new ArrayList<>();
            int radius = size;

            int d = (5 - radius * 4) / 4;
            int x0 = 0;
            int y0 = radius;

            do {
                for (int i = x - x0; i <= x + x0; i++) {
                    cacheList.add(i);
                    cacheList.add(y + y0);
                    cacheList.add(i);
                    cacheList.add(y - y0);
                }
                for (int i = x - y0; i <= x + y0; i++) {
                    cacheList.add(i);
                    cacheList.add(y + x0);
                    cacheList.add(i);
                    cacheList.add(y - x0);
                }

                if (d < 0) {
                    d += 2 * x0 + 1;
                } else {
                    d += 2 * (x0 - y0) + 1;
                    y0--;
                }
                x0++;
            } while (x0 <= y0);

            cache = cacheList.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public void transform(Transform Matrix)
    {
        if(cache == null) draw();
        this.x += Matrix.e;
        this.y += Matrix.f;
       // draw();
        for(int i = 0; i < cache.length; i += 2)
        {
            cache[i] += Matrix.e;
            cache[i + 1] += Matrix.f;
        }
    }
}
