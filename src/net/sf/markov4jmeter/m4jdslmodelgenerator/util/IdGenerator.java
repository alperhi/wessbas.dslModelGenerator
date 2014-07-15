package net.sf.markov4jmeter.m4jdslmodelgenerator.util;

/**
 * Helper class for generating unique identifiers. Each identifier consists of
 * a common prefix and a counter ID.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class IdGenerator {


    /* *****************************  constants  **************************** */

    /** Start value of the ID counter. */
    private final static long START_ID = 1;

    /** Default prefix of each identifier. */
    private final static String DEFAULT_PREFIX = "id";


    /* *************************  global variables  ************************* */


    /** ID counter for generating unique values. */
    private long idCounter;

    /** Prefix of each generated identifier. */
    private final String prefix;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for an ID Generator, which uses a specific prefix for each
     * generated identifier.
     *
     * @param prefix  prefix of each generated identifier.
     */
    public IdGenerator (final String prefix) {

        this.prefix    = prefix;
        this.idCounter = IdGenerator.START_ID;
    }

    /**
     * Constructor for an ID Generator, which uses the default prefix "id" for
     * each generated identifier.
     */
    public IdGenerator() {

        this(IdGenerator.DEFAULT_PREFIX);
    }


    /* **************************  public methods  ************************** */


    /**
     * Returns the prefix which is added to each generated identifier.
     *
     * @return  prefix of each generated identifier.
     */
    public String getPrefix () {

        return this.prefix;
    }

    /**
     * Resets the ID counter to its start value.
     */
    public void reset () {

        this.idCounter = IdGenerator.START_ID;
    }

    /**
     * Generates a new identifier.
     *
     * @return  an identifier which consists of a prefix and a counter ID.
     */
    public String newId () {

        return this.prefix + this.idCounter++;
    }
}