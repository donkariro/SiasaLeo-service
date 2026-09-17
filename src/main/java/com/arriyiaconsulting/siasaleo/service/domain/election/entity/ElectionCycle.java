package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import jakarta.persistence.*;

/** Reference data maintained by database migrations. */
@Entity
@Table(name = "election_cycle")
public class ElectionCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(name = "from_year", nullable = false)
    private int fromYear;
    @Column(name = "upto_year", nullable = false)
    private int uptoYear;
    @Column(nullable = false, length = 10)
    private String status;

    protected ElectionCycle() { }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getFromYear() { return fromYear; }
    public int getUptoYear() { return uptoYear; }
    public String getStatus() { return status; }
}
