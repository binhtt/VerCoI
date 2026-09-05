package vercoi.model;

public record Metrics(int tp, int fp, int fn, int tn) {
    public double precision() { return div(tp, tp + fp); }
    public double recall() { return div(tp, tp + fn); }
    public double f1() { double p=precision(), r=recall(); return p+r==0?0:2*p*r/(p+r); }
    public double specificity() { return div(tn, tn + fp); }
    public double accuracy() { return div(tp+tn, tp+fp+fn+tn); }
    private static double div(double a,double b){ return b==0?0:a/b; }
}
