package net.sf.markov4jmeter.m4jdslmodelgenerator;

/**
 * Exception to be thrown if any error during the generation process occurs.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class GeneratorException extends Exception {

    /** Default serial version ID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with a specific error message.
     *
     * @param message  additional information about the error which occurred.
     */
    public GeneratorException (final String message) {

        super(message);
    }
}