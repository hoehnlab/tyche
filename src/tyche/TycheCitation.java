package tyche;

/**
 * The ONE place where the TyCHE paper citation is written down. Every @Citation annotation in the package refers to these constants
 * (they are compile-time constants, which is what Java requires for annotation values).
 *
 * When the paper is published: edit the constants below (and nothing else in the Java code), then do a CLEAN build (`ant package` already starts with a clean).
 * Java copies constant values into every class that uses them, so an incremental compile of just this file would leave the old text in the other classes.
 */
public final class TycheCitation {
    private TycheCitation() {}

    public static final String AUTHORS = "Fielding, J. J., Wu, S., Melton, H. J., Wang, C. Z., Fisk, N., du Plessis, L., & Hoehn, K. B.";
    public static final int YEAR = 2026;
    public static final String TITLE = "TyCHE enables time-resolved lineage tracing of B cells and other heterogeneously-evolving populations.";
    public static final String VENUE = "Molecular Biology and Evolution, msag281";
    public static final String DOI = "10.1093/molbev/msag281";
    public static final String FIRST_AUTHOR = "Fielding";

    /** The full text BEAST prints (compile-time constant built from the parts above). */
    public static final String TEXT = AUTHORS + " (" + YEAR + ").\n" + TITLE + "\n" + VENUE + " (" + YEAR + ") doi:" + DOI + ".";
}
