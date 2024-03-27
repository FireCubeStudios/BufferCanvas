package com.example.canvastest.osm;

import java.util.ArrayList;

public class Way {

    public final Tag[] tags;
    double[] coords;

    public Way(ArrayList<Node> way, ArrayList<Tag> tags) {
        this.tags = tags.toArray(new Tag[0]);
        coords = new double[way.size() * 2];
        for (int i = 0 ; i < way.size() ; ++i) {
            Node node = way.get(i);
            coords[2 * i] = 0.56 * node.lon;
            coords[2 * i + 1] = -node.lat;
        }
    }
}
