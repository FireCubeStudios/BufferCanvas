package com.example.canvastest;

import javafx.scene.paint.Color;

public interface IDrawable {
    public int[] getPoints();
    public Color getColor();
    public void transform(Transform matrix);
}
