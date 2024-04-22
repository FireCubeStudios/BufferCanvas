package com.example.canvastest;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.wolt.osm.parallelpbf.ParallelBinaryParser;
import com.wolt.osm.parallelpbf.entity.*;
import java.util.concurrent.atomic.AtomicLong;

public class PBFParser {
    private final StringBuilder output = new StringBuilder();
    private final AtomicLong nodesCounter = new AtomicLong();
    private final AtomicLong waysCounter = new AtomicLong();
    private final AtomicLong relationsCounter = new AtomicLong();

    private void processHeader(Header header) {
        synchronized (output) {
            output.append(header);
            output.append("\n");
        }
    }

    private void processBoundingBox(BoundBox bbox) {
        synchronized (output) {
            output.append(bbox);
            output.append("\n");
        }
    }

    private void processNodes(Node node) {
        nodesCounter.incrementAndGet();
    }

    private void processWays(Way way) {
        waysCounter.incrementAndGet();
    }

    private void processRelations(Relation way) {
        relationsCounter.incrementAndGet();
    }

    private long startTime = 0;
    private void printOnCompletions() {
        System.out.println("Elapsed Time: " + ((System.nanoTime() - startTime) / 1000000) + " milliseconds");
        output.append("Node count: ");
        output.append(nodesCounter.get());
        output.append("\n");

        output.append("Way count: ");
        output.append(waysCounter.get());
        output.append("\n");

        output.append("Relations count: ");
        output.append(relationsCounter.get());
        output.append("\n");

        System.out.println("Reading results:");
        System.out.println(output);
    }
    public void getMapData(InputStream inputStream) {

        startTime = System.nanoTime();
        new ParallelBinaryParser(inputStream, 100)
                .onHeader(this::processHeader)
                .onBoundBox(this::processBoundingBox)
                .onComplete(this::printOnCompletions)
                .onNode(this::processNodes)
                .onWay(this::processWays)
                .onRelation(this::processRelations)
                .parse();
    }
}
