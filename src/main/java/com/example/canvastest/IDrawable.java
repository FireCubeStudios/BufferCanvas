package com.example.canvastest;

public interface IDrawable {
    public int[] getPoints();
    public void transform(Transform matrix);
}
