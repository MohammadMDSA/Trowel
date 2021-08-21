package ir.sami.trowel.data;


import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class ModelBuildConfig {

    private String projectName;
    private boolean computeFeatureUpRight;
    private int maxImageDimension;
    private String featureDescriberMethod;
    private String featureDescriberPreset;
    private MatchGeometricModel matchGeometricModel;
    private String matchRatio;
    private String matchMethod;
    private ReconstructionRefinement reconstructionRefinement;
    private boolean fixPoseBundleAdjustment;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String isComputeFeatureUpRight() {
        return computeFeatureUpRight ? "1" : "0";
    }

    public void setComputeFeatureUpRight(boolean computeFeatureUpRight) {
        this.computeFeatureUpRight = computeFeatureUpRight;
    }

    public int getMaxImageDimension() {
        return maxImageDimension;
    }

    public void setMaxImageDimension(int maxImageDimension) {
        this.maxImageDimension = maxImageDimension;
    }

    public String getFeatureDescriberMethod() {
        return featureDescriberMethod;
    }

    public void setFeatureDescriberMethod(String featureDescriberMethod) {
        this.featureDescriberMethod = featureDescriberMethod;
    }

    public String getFeatureDescriberPreset() {
        return featureDescriberPreset;
    }

    public void setFeatureDescriberPreset(String featureDescriberPreset) {
        this.featureDescriberPreset = featureDescriberPreset;
    }

    public String getMatchGeometricModel() {
        return matchGeometricModel.getValue();
    }

    public void setMatchGeometricModel(MatchGeometricModel matchGeometricModel) {
        this.matchGeometricModel = matchGeometricModel;
    }

    public String getMatchRatio() {
        return matchRatio;
    }

    public void setMatchRatio(String matchRatio) {
        this.matchRatio = matchRatio;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public void setMatchMethod(String matchMethod) {
        this.matchMethod = matchMethod;
    }

    public String getReconstructionRefinement() {
        return reconstructionRefinement.getValue();
    }

    public void setReconstructionRefinement(ReconstructionRefinement reconstructionRefinement) {
        this.reconstructionRefinement = reconstructionRefinement;
    }

    public String isFixPoseBundleAdjustment() {
        return fixPoseBundleAdjustment ? "1" : "0";
    }

    public void setFixPoseBundleAdjustment(boolean fixPoseBundleAdjustment) {
        this.fixPoseBundleAdjustment = fixPoseBundleAdjustment;
    }

    public enum MatchGeometricModel {
        FundamentalMatrixFiltering("f"),
        EssentialMatrixFiltering("e"),
        HomographyMatrixFiltering("h");

        private String value;

        MatchGeometricModel(String val) {
            this.value = val;
        }

        public String getValue() {
            return value;
        }
    }

    public static class ReconstructionRefinement {
        private boolean focalLength;
        private boolean principalPoint;
        private boolean distortion;

        public boolean isFocalLength() {
            return focalLength;
        }

        public boolean isPrincipalPoint() {
            return principalPoint;
        }

        public boolean isDistortion() {
            return distortion;
        }

        public void setDistortion(boolean distortion) {
            this.distortion = distortion;
        }

        public void setFocalLength(boolean focalLength) {
            this.focalLength = focalLength;
        }

        public void setPrincipalPoint(boolean principalPoint) {
            this.principalPoint = principalPoint;
        }

        public String getValue() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (focalLength && principalPoint && distortion)
                    return "ADJUST_ALL";
                if (!focalLength && !principalPoint && !distortion)
                    return "NONE";
                List<String> enabled = new ArrayList<>();
                if (focalLength)
                    enabled.add("ADJUST_FOCAL_LENGTH");
                if (principalPoint)
                    enabled.add("ADJUST_PRINCIPAL_POINT");
                if (distortion)
                    enabled.add("ADJUST_DISTORTION");
                return String.join("|", enabled);
            }
            return "NONE";
        }
    }
}
