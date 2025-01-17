package org.acme.dto;

public class PassDTO {

    public long id;
    public String name;
    public String country;
    public int ascent;
    public double latitude;
    public double longitude;
    public String osmUrl;

    public PassDTO() {

    }

    public PassDTO(long id, String name, String country, int ascent, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.ascent = ascent;
        this.latitude = latitude;
        this.longitude = longitude;
        this.osmUrl = String.format("https://www.openstreetmap.org/#map=19/%f/%f", latitude, longitude);
    }
}
