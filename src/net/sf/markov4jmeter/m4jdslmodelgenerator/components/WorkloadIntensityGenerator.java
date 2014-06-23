package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.util.Properties;

import m4jdsl.ConstantWorkloadIntensity;
import m4jdsl.M4jdslFactory;
import m4jdsl.WorkloadIntensity;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;

/**
 * Generator class for creating M4J-DSL model components, which represent the
 * workload intensity.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class WorkloadIntensityGenerator {


    /* *****************************  constants  **************************** */


    /** Property key for workload intensity type. */
    private final static String PKEY_WORKLOAD_INTENSITY__TYPE =
            "workloadIntensity.type";

    /** Property key for workload intensity formula. */
    private final static String PKEY_WORKLOAD_INTENSITY__FORMULA =
            "workloadIntensity.formula";

    /** Workload intensity constant for "constant" workload intensity. */
    private final static String WORKLOAD_INTENSITY_TYPE__CONSTANT =
            "constant";

    /* --------------------------  error messages  -------------------------- */


    /** Error message for the case that a given workload intensity formula
     *  does not denote a constant workload intensity. */
    private final static String ERROR_NO_CONSTANT_WORKLOAD_INTENSITY =
            "formula \"%s\" does not denote a constant workload intensity";

    /** Error message for the case that a specified workload intensity type is
     *  unknown. */
    private final static String ERROR_UNKNOWN_WORKLOAD_INTENSITY_TYPE =
            "unknown workload intensity type \"%s\"";

    /** Error message for the case that no workload intensity formula is
     *  defined. */
    private final static String ERROR_WORKLOAD_INTENSITY_FORMULA_UNDEFINED =
            "workload intensity formula undefined";

    /** Error message for the case that no workload intensity type is
     *  defined. */
    private final static String ERROR_WORKLOAD_INTENSITY_TYPE_UNDEFINED =
            "workload intensity type undefined";


    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Workload Intensity Generator.
     *
     * @param m4jdslFactory  instance for creating M4J-DSL model elements.
     */
    public WorkloadIntensityGenerator (final M4jdslFactory m4jdslFactory) {

        this.m4jdslFactory = m4jdslFactory;
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates an M4J-DSL model component which represents a workload intensity.
     *
     * @param properties
     *     properties which provide the workload intensity information.
     *
     * @return
     *     the newly created M4J-DSL model component.
     *
     * @throws GeneratorException
     *     if the workload intensity information provided by the properties
     *     is insufficient or invalid.
     */
    public WorkloadIntensity generateWorkloadIntensity (
            final Properties properties) throws GeneratorException {

        final WorkloadIntensity workloadIntensity;  // to be returned;

        final String type = properties.getProperty(
                WorkloadIntensityGenerator.PKEY_WORKLOAD_INTENSITY__TYPE);

        final String formula = properties.getProperty(
                WorkloadIntensityGenerator.PKEY_WORKLOAD_INTENSITY__FORMULA);

        if (type == null) {

            throw new GeneratorException(
                    WorkloadIntensityGenerator.
                    ERROR_WORKLOAD_INTENSITY_TYPE_UNDEFINED);
        }

        if (formula == null) {

            throw new GeneratorException(
                    WorkloadIntensityGenerator.
                    ERROR_WORKLOAD_INTENSITY_FORMULA_UNDEFINED);
        }

        switch (type) {

            case WorkloadIntensityGenerator.WORKLOAD_INTENSITY_TYPE__CONSTANT :

                workloadIntensity =
                        this.generateConstantWorkloadIntensity(formula);

                break;

            default:

                final String message = String.format(
                        WorkloadIntensityGenerator.
                        ERROR_UNKNOWN_WORKLOAD_INTENSITY_TYPE,
                        type);

                throw new GeneratorException(message);
        }

        return workloadIntensity;
    }


    /* **************************  private methods  ************************* */


    /**
     * Creates an M4J-DSL model component which represents the constant number
     * of sessions indicated by a given formula.
     *
     * @param formula  formula which indicates a constant number of sessions.
     *
     * @return  the newly created M4J-DSL model component.
     *
     * @throws GeneratorException  if the formula does not denote a number.
     */
    private ConstantWorkloadIntensity generateConstantWorkloadIntensity (
            final String formula) throws GeneratorException {

        final ConstantWorkloadIntensity constantWorkloadIntensity =
                this.m4jdslFactory.createConstantWorkloadIntensity();

        // might throw a GeneratorException;
        final int numberOfSessions = this.getConstantNumberOfSessions(formula);

        constantWorkloadIntensity.setNumberOfSessions(numberOfSessions);
        constantWorkloadIntensity.setFormula(formula);

        return constantWorkloadIntensity;
    }

    /**
     * Returns the constant number of sessions indicated by a given formula.
     *
     * @param formula  formula which indicates a constant number of sessions.
     *
     * @return  the constant number of sessions indicated by the given formula.
     *
     * @throws GeneratorException  if the formula does not denote a number.
     */
    private int getConstantNumberOfSessions (final String formula)
            throws GeneratorException {

        try {

            return Integer.parseInt(formula);

        } catch (final NumberFormatException ex) {

            final String message = String.format(
                    WorkloadIntensityGenerator.
                    ERROR_NO_CONSTANT_WORKLOAD_INTENSITY,
                    formula);

            throw new GeneratorException(message);
        }
    }
}
