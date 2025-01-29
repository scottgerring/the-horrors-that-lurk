package org.acme.dto;

public class DiveSiteDTO {

    public long id;
    public String name;
    public String country;
    public int descent;
    public double latitude;
    public double longitude;

    public DiveSiteDTO() {

    }

    public DiveSiteDTO(long id, String name, String country, int descent, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.descent = descent;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
