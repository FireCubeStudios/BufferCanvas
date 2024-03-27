package com.example.canvastest.osm;

import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.xml.stream.*;
public class Model {
    OSMData mapData;

    // Constructor for Model
    public Model(String filename) throws XMLStreamException, FactoryConfigurationError, IOException {
        try {
            InputStream osmInputStream = getOSMInputStream(filename);
            OSMParser osmParser = new OSMParser();
            mapData = osmParser.getMapData(osmInputStream);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private InputStream getOSMInputStream(String filename) throws IOException {
        if (filename.endsWith(".osm.zip")) {
            ZipInputStream input = new ZipInputStream(new FileInputStream(filename));
            input.getNextEntry();
            return input;
        } else if (filename.endsWith(".osm")) {
            return new FileInputStream(filename);
        }

        throw new IOException("Unknown file extension.");
    }

    void save(String filename) throws FileNotFoundException, IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this);
        }
    }

    static Model load(String filename) throws FileNotFoundException, IOException, ClassNotFoundException, XMLStreamException, FactoryConfigurationError {
        if (filename.endsWith(".obj")) {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
                return (Model) in.readObject();
            }
        }
        return new Model(filename);
    }
}