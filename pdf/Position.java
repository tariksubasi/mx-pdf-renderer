package myfirstmodule.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Position model matching the React widget's Position structure
 */
public class Position {
    private Integer positionID;
    private String positionName;
    private String hierarchyCode;
    private String parentHierarchyCode;
    private Integer norm;
    private String titleCode;
    private List<Position> positions;

    public Position() {
        this.positions = new ArrayList<>();
        this.norm = 0;
    }

    public Integer getPositionID() {
        return positionID;
    }

    public void setPositionID(Integer positionID) {
        this.positionID = positionID;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getHierarchyCode() {
        return hierarchyCode;
    }

    public void setHierarchyCode(String hierarchyCode) {
        this.hierarchyCode = hierarchyCode;
    }

    public String getParentHierarchyCode() {
        return parentHierarchyCode;
    }

    public void setParentHierarchyCode(String parentHierarchyCode) {
        this.parentHierarchyCode = parentHierarchyCode;
    }

    public Integer getNorm() {
        return norm != null ? norm : 0;
    }

    public void setNorm(Integer norm) {
        this.norm = norm;
    }

    public String getTitleCode() {
        return titleCode;
    }

    public void setTitleCode(String titleCode) {
        this.titleCode = titleCode;
    }

    public List<Position> getPositions() {
        return positions != null ? positions : new ArrayList<>();
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
}


