package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "problem_links")
@IdClass(ProblemLinkId.class)
public class ProblemLink {

    @Id
    @Column(name = "problem_id", nullable = false)
    private String problemId;

    @Id
    @Column(name = "link_type", nullable = false)
    private String linkType;

    @Column(nullable = false)
    private String url;

    public ProblemLink() {
    }

    public String getProblemId() {
        return problemId;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getUrl() {
        return url;
    }
}
