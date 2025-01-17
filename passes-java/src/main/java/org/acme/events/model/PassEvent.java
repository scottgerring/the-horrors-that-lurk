package org.acme.events.model;

public class PassEvent {
    public long passId;
    public String name;
    public String country;
    public int ascent;
    public EventType eventType;

    public PassEvent(long passId, String name, String country, int ascent, EventType eventType) {
        this.passId = passId;
        this.name = name;
        this.country = country;
        this.ascent = ascent;
        this.eventType = eventType;
    }
    public enum EventType {
        Created,
        Updated,
        Deleted
    }

}
