package com.example.canvastest.osm;

import javax.xml.stream.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSMParser {
    private XMLStreamReader streamReader;
    private Map<Long, Node> idNodeMap;
    private ArrayList<Node> currentWayNodes;
    private ArrayList<Tag> currentWayTags;
    private boolean currentlyCoast;

    private double minlat, maxlat, minlon, maxlon;
    private List<Way> ways = new ArrayList<>();

    public OSMParser() {

    }

    public OSMData getMapData(InputStream inputStream) throws XMLStreamException {
        this.streamReader = XMLInputFactory.newInstance().createXMLStreamReader(new InputStreamReader(inputStream));
        idNodeMap = new HashMap<>();
        currentWayNodes = new ArrayList<>();
        currentWayTags = new ArrayList<>();

        currentlyCoast = false;
        parseOSM();
        return new OSMData(ways, minlat, maxlat, minlon, maxlon);
    }

    //region Attribute Getters

    private String getAttribute(String attributeName) { return streamReader.getAttributeValue(null, attributeName); }
    private Double getDoubleAttribute(String attributeName) { return Double.parseDouble(getAttribute(attributeName)); }
    private Long getLongAttribute(String attributeName) { return Long.parseLong(getAttribute(attributeName)); }

    //endregion

    private void parseOSM() throws XMLStreamException {
        while (streamReader.hasNext()) parseTag();
    }

    private void parseTag() throws XMLStreamException {
        int tagKind = streamReader.next();
        boolean startTag = tagKind == XMLStreamConstants.START_ELEMENT;
        boolean endTag = tagKind == XMLStreamConstants.END_ELEMENT;

        if (startTag) parseStartTag();
        else if (endTag) parseEndTag();
    }

    //region Start Tag


    private void parseStartTag()
    {
        String tagName = streamReader.getLocalName();
        switch (tagName)
        {
            case "bounds":
                setBounds();
                break;
            case "node":
                loadNode();
                break;
            case "way":
                newWay();
                break;
            case "tag":
                loadTag();
                break;
            case "nd":
                addWayNode();
                break;
        }
    }

    //region Tag Functions

    private void setBounds()
    {
        minlat = getDoubleAttribute("minlat");
        maxlat = getDoubleAttribute("maxlat");
        minlon = getDoubleAttribute("minlon");
        maxlon = getDoubleAttribute("maxlon");
    }

    private void loadNode()
    {
        long id = getLongAttribute("id");
        double lat = getDoubleAttribute("lat");
        double lon = getDoubleAttribute("lon");

        Node node = new Node(lat, lon);
        idNodeMap.put(id, node);
    }

    private void newWay()
    {
        currentWayNodes.clear();
        currentWayTags.clear();
        currentlyCoast = false;
    }

    private void loadTag()
    {
        String type = getAttribute("k");
        String data = getAttribute("v");
        if (data.equals("coastline")) {
            currentlyCoast = true;
        }

        Tag tag = new Tag(type, data);
        currentWayTags.add(tag);
    }

    private void addWayNode()
    {
        long nodeIdReference = getLongAttribute("ref");
        Node wayNode = idNodeMap.get(nodeIdReference);
        currentWayNodes.add(wayNode);
    }

    //endregion

    //endregion

    //region End Tag

    private void parseEndTag()
    {
        String tagName = streamReader.getLocalName();
        // If you wish to only draw coastline -- if (name == "way" && coast) {
        if (tagName.equals("way")) {
            Way way = new Way(currentWayNodes, currentWayTags);
            ways.add(way);


        }
    }

    //endregion

}
