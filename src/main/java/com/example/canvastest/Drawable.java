package com.example.canvastest;

import javafx.scene.paint.Color;

public abstract class Drawable implements IDrawable {
    public int[] cache;
    public Color color;

    public Drawable(Color color)
    {
        this.color = color;
    }

    public abstract void draw();
    public abstract void transform(Transform Matrix);

    @Override
    public int[] getPoints() {
        if(cache == null)
            draw();
        return cache;
    }

    @Override
    public Color getColor() {
        return color;
    }
}
