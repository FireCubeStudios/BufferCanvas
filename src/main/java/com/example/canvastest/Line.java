package com.example.canvastest;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class Line implements IDrawable {
    public int x1;
    public int y1;
    public int x2;
    public int y2;

    public int[] cache;
    public int size;
    public Line(int x1, int y1, int x2, int y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int[] getPoints() {
        if(cache == null)
            draw();
        return cache;
    }

    public void draw() {
        ArrayList<Integer> cacheList = new ArrayList<>();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int x = x1;
        int y = y1;

        while (true) {
            cacheList.add(x);
            cacheList.add(y);

            if (x == x2 && y == y2) {
                break;
            }

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

        cache = cacheList.stream().mapToInt(Integer::intValue).toArray();
    }

    public void transform(Transform Matrix)
    {
        if(cache == null) draw();
        this.x1 += Matrix.e;
        this.y1 += Matrix.f;
        this.x2 += Matrix.e;
        this.y2 += Matrix.f;
        IntStream.range(0, cache.length - 1)
                .filter(i -> i % 2 == 0)
                .forEach(i -> {
                    cache[i] += Matrix.e;
                    cache[i + 1] += Matrix.f;
                });
       // draw();
    }
}
