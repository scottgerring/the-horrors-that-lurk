package org.acme.db;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "passes")
public class Pass extends PanacheEntityBase {

    // We extend PanacheEntityBase, so we can customize ID, so we can use a linear sequence generator.
    // This makes testing easier, as our IDs don't jump around!
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "passes_seq")
    @SequenceGenerator(name = "passes_seq", sequenceName = "passes_SEQ", allocationSize = 1)
    public Long id;

    @Column(name = "name")
    public String name;

    @Column(name = "ascent")
    public int ascent;

    @Column(name = "latitude")
    public double latitude;

    @Column(name = "longitude")
    public double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    public Country country;
}
