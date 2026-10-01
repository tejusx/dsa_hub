package dsa_hub_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @Column(name = "problem_id")
    private String problemId;

    @Column(nullable = false)
    private String title;

    @Column(name = "leetcode_id", unique = true)
    private String leetcodeId;

    @Column(name = "leetcode_slug")
    private String leetcodeSlug;

    private String difficulty;

    @Column(name = "leetcode_paid_only")
    private Boolean leetcodePaidOnly;

    @Column(name = "leetcode_has_solution")
    private Boolean leetcodeHasSolution;

    @Column(name = "leetcode_has_video_solution")
    private Boolean leetcodeHasVideoSolution;

    public Problem() {
    }

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLeetcodeId() {
        return leetcodeId;
    }

    public void setLeetcodeId(String leetcodeId) {
        this.leetcodeId = leetcodeId;
    }

    public String getLeetcodeSlug() {
        return leetcodeSlug;
    }

    public void setLeetcodeSlug(String leetcodeSlug) {
        this.leetcodeSlug = leetcodeSlug;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getLeetcodePaidOnly() {
        return leetcodePaidOnly;
    }

    public void setLeetcodePaidOnly(Boolean leetcodePaidOnly) {
        this.leetcodePaidOnly = leetcodePaidOnly;
    }

    public Boolean getLeetcodeHasSolution() {
        return leetcodeHasSolution;
    }

    public void setLeetcodeHasSolution(Boolean leetcodeHasSolution) {
        this.leetcodeHasSolution = leetcodeHasSolution;
    }

    public Boolean getLeetcodeHasVideoSolution() {
        return leetcodeHasVideoSolution;
    }

    public void setLeetcodeHasVideoSolution(Boolean leetcodeHasVideoSolution) {
        this.leetcodeHasVideoSolution = leetcodeHasVideoSolution;
    }
}