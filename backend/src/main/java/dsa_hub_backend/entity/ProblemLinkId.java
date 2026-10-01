package dsa_hub_backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class ProblemLinkId implements Serializable {

    private String problemId;
    private String linkType;

    public ProblemLinkId() {
    }

    public ProblemLinkId(String problemId, String linkType) {
        this.problemId = problemId;
        this.linkType = linkType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProblemLinkId that)) return false;
        return Objects.equals(problemId, that.problemId) && Objects.equals(linkType, that.linkType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemId, linkType);
    }
}
