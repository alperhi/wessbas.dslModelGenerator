package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.util.Properties;

import m4jdsl.ConstantWorkloadIntensity;
import m4jdsl.M4jdslFactory;
import m4jdsl.WorkloadIntensity;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;

public class WorkloadIntensityGenerator {

    private final static String PKEY_WORKLOAD_INTENSITY__TYPE    = "workloadIntensity.type";
    private final static String PKEY_WORKLOAD_INTENSITY__FORMULA = "workloadIntensity.formula";

    private final static String WORKLOAD_INTENSITY_TYPE__CONSTANT = "constant";

    private final static String ERROR_NO_CONSTANT_WORKLOAD_INTENSITY =
            "formula \"%s\" does not denote a constant workload intensity";

    private final static String ERROR_UNKNOWN_WORKLOAD_INTENSITY_TYPE =
            "unknown workload intensity type \"%s\"";
    
    private final static String WORKLOAD_INTENSITY_FORMULA_UNDEFINED =
            "workload intensity formula undefined";

    private final static String WORKLOAD_INTENSITY_TYPE_UNDEFINED =
            "workload intensity type undefined";

    private final M4jdslFactory m4jdslFactory;


    public WorkloadIntensityGenerator (final M4jdslFactory m4jdslFactory) {

        this.m4jdslFactory = m4jdslFactory;
    }


    public WorkloadIntensity generateWorkloadIntensity (
            final Properties properties) throws GeneratorException {

        final WorkloadIntensity workloadIntensity;  // to be returned;

        final String type = properties.getProperty(
                WorkloadIntensityGenerator.PKEY_WORKLOAD_INTENSITY__TYPE);
        
        final String formula = properties.getProperty(
                WorkloadIntensityGenerator.PKEY_WORKLOAD_INTENSITY__FORMULA);

        if (type == null) {

            throw new GeneratorException(
                    WorkloadIntensityGenerator.WORKLOAD_INTENSITY_TYPE_UNDEFINED);
                    
        }

        if (formula == null) {

            throw new GeneratorException(
                    WorkloadIntensityGenerator.WORKLOAD_INTENSITY_FORMULA_UNDEFINED);
        }

        switch (type) {

            case WorkloadIntensityGenerator.WORKLOAD_INTENSITY_TYPE__CONSTANT :

                workloadIntensity = this.generateConstantWorkloadIntensity(formula);
                break;

            default:

                final String message = String.format(
                        WorkloadIntensityGenerator.ERROR_UNKNOWN_WORKLOAD_INTENSITY_TYPE,
                        type);

                throw new GeneratorException(message);
        }

        return workloadIntensity;
    }
    
    private ConstantWorkloadIntensity generateConstantWorkloadIntensity (
            final String formula) throws GeneratorException {

        final ConstantWorkloadIntensity constantWorkloadIntensity =
                this.m4jdslFactory.createConstantWorkloadIntensity();

        final int numberOfSessions = this.getConstantNumberOfSessions(formula);

        constantWorkloadIntensity.setNumberOfSessions(numberOfSessions);
        constantWorkloadIntensity.setFormula(formula);

        return constantWorkloadIntensity;
    }
    
    private int getConstantNumberOfSessions (final String formula) throws GeneratorException {
        
        try {

            return Integer.parseInt(formula);

        } catch (final NumberFormatException ex) {

            final String message = String.format(
                    WorkloadIntensityGenerator.ERROR_NO_CONSTANT_WORKLOAD_INTENSITY,
                    formula);
                    
            throw new GeneratorException(message);
        }
    }
}
