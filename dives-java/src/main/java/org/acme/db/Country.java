package org.acme.db;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Country extends PanacheEntityBase {

    @Id
    @Column(length = 2, nullable = false)
    private String shortCode;

    private String name;

    public Country() {

    }

    public Country(String shortCode, String name) {
        this.shortCode = shortCode;
        this.name = name;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
