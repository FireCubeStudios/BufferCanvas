package com.example.canvastest.osm;

import java.util.List;

public class OSMData {
    public final double minlat, maxlat, minlon, maxlon;
    public final List<Way> ways;

    public OSMData(List<Way> ways, double minlat, double maxlat, double minlon, double maxlon)
    {
        this.ways = ways;
        this.minlat = minlat;
        this.maxlat = maxlat;
        this.minlon = minlon;
        this.maxlon = maxlon;
    }
}
