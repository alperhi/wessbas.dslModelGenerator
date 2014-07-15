package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import m4jdsl.BehaviorModel;
import m4jdsl.BehaviorModelExitState;
import m4jdsl.BehaviorModelState;
import m4jdsl.M4jdslFactory;
import m4jdsl.MarkovState;
import m4jdsl.NormallyDistributedThinkTime;
import m4jdsl.Service;
import m4jdsl.ThinkTime;
import m4jdsl.Transition;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.ServiceRepository;
import net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm.FlowSessionLayerEFSMGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.CSVHandler;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

/**
 * Generator class for creating M4J-DSL model components, which represent the
 * Behavior Models.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class BehaviorModelsGenerator {


    /* *****************************  constants  **************************** */


    /** Suffix of behavior information files. */
    private final static String BEHAVIOR_FILE_SUFFIX = ".csv";


    /* ----------------------  error/warning messages  ---------------------- */


    /** Error message for the case that the initial state is not included in a
     *  Behavior Model. */
    private final static String ERROR_UNDEFINED_INITIAL_SERVICE =
            "initial service \"%s\" is not included in Behavior Model \"%s\"";

    /** Warning message for the case that an unknown target state has been
     *  detected for a transition in a Behavior Model. */
    private final static String WARNING_UNKNOWN_TARGETSTATE =
            "unknown target state \"%s\" detected, will be ignored";

    /** Warning message for the case that a behavior information file could
     *  not be loaded for a Behavior Model. */
    private final static String WARNING_BEHAVIOR_FILE_LOADING_FAILED =
            "behavior information file \"%s\" could not be loaded for "
            + "Behavior Model \"%s\"; will not install any behavior "
            + "information into that model";

    /** Warning message for the case that a negative probability has been
     *  detected. */
    private final static String WARNING_NEGATIVE_PROBABILITY =
            "negative probability value detected, will use 0.0 instead of %f";

    /** Warning message for the case that a probability value could not be
     *  parsed. */
    private final static String WARNING_PROBABILITY_PARSING_FAILED =
            "could not parse probability value \"%s\", will use 0.0 instead";


    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;

    /** Instance for creating unique Markov State IDs. */
    private final IdGenerator idGenerator;

    /** Instance for handling all available services. */
    private final ServiceRepository serviceRepository;

    /** Instance for reading/writing CSV files. */
    private final CSVHandler csvHandler;

    /** Instance for parsing think time definitions. */
    private final ThinkTimeParser thinkTimeParser;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Behavior Models Generator.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param idGenerator
     *     instance for creating unique Markov State IDs.
     * @param serviceRepository
     *     instance for handling all available services.
     */
    public BehaviorModelsGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final ServiceRepository serviceRepository) {

        this.m4jdslFactory     = m4jdslFactory;
        this.idGenerator       = idGenerator;
        this.serviceRepository = serviceRepository;

        this.csvHandler      = new CSVHandler();
        this.thinkTimeParser = new ThinkTimeParser(m4jdslFactory);
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates a set of Behavior Models, based on name/filename/behavior
     * information which needs to provided as separate arrays; each of those
     * arrays must be of same length, providing information in a common order.
     *
     * @param names
     *     names of the Behavior Models.
     * @param filenames
     *     filenames of the Behavior Models.
     * @param behaviorFiles
     *     CSV files which provide the behavior information as matrices, to be
     *     included into the Behavior Models.
     *
     * @return
     *     the newly created Behavior Models.
     *
     * @throws GeneratorException
     *     if any error occurs during the generation process.
     */
    public LinkedList<BehaviorModel> generateBehaviorModels (
            final String[] names,
            final String[] filenames,
            final File[] behaviorFiles) throws GeneratorException {

        final LinkedList<BehaviorModel> behaviorModels =
                new LinkedList<BehaviorModel>();

        for (int i = 0, n = behaviorFiles.length; i < n; i++) {

            final String name         = names[i];
            final String filename     = filenames[i];
            final File   behaviorFile = behaviorFiles[i];

            final BehaviorModel behaviorModel = this.generateBehaviorModel(
                    name,
                    filename,
                    behaviorFile);

            behaviorModels.add(behaviorModel);
        }

        return behaviorModels;
    }


    /* **************************  private methods  ************************* */


    /**
     * Creates a Behavior Model, based on name/filename/behavior information.
     *
     * @param name
     *     name of the Behavior Model.
     * @param filename
     *     filename of the Behavior Model.
     * @param behaviorFile
     *     CSV file which provides the behavior information as matrix, to be
     *     included into the Behavior Model.
     *
     * @return
     *     the newly created Behavior Model.
     *
     * @throws GeneratorException
     *     if any error occurs during the generation process.
     */
    private BehaviorModel generateBehaviorModel (
            final String name,
            final String filename,
            final File   behaviorFile) throws GeneratorException {

        final String initServiceName =
                FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME;

        final BehaviorModel behaviorModel =
                this.createBehaviorModel(name, filename);

        boolean installedInitialState = false;

        for (final Service service : this.serviceRepository.getServices()) {

            final MarkovState markovState = this.createMarkovState(service);

            // add the newly created Markov State to the Behavior Model;

            behaviorModel.getMarkovStates().add(markovState);

            if ( service.getName().equals(initServiceName) ) {

                behaviorModel.setInitialState(markovState);
                installedInitialState = true;
            }
        }

        if ( !installedInitialState ) {

            final String message = String.format(
                    BehaviorModelsGenerator.ERROR_UNDEFINED_INITIAL_SERVICE,
                    initServiceName,
                    name);

            throw new GeneratorException(message);
        }

        // all Markov States have been created, and -all- services have been
        // registered in the service repository; now, add the transitions
        // between the states, with the use of the repository;

        // behaviorInformation is initialized with null, in case reading fails;
        final String[][] behaviorInformation =
                this.readBehaviorInformation( behaviorFile.getAbsolutePath() );

        if (behaviorInformation != null) {

            final List<MarkovState> markovStates =
                    behaviorModel.getMarkovStates();

            for (final MarkovState markovState : markovStates) {

                this.setMarkovStateTransitions(
                        markovState,
                        markovStates,
                        behaviorModel.getExitState(),
                        behaviorInformation);
            }

        } else {

            this.warn(
                    BehaviorModelsGenerator.
                    WARNING_BEHAVIOR_FILE_LOADING_FAILED,
                    behaviorFile,
                    name);
        }

        return behaviorModel;
    }

    /**
     * Reads the behavior information from a given CSV file.
     *
     * @param filename
     *     CSV file which provides the behavior information as matrix.
     *
     * @return
     *     the matrix which has been read from the given CSV file, or
     *     <code>null</code> if the file could not be read.
     */
    private String[][] readBehaviorInformation (final String filename) {

        String[][] information;  // to be returned;

        try {

            information = this.csvHandler.readValues(filename);

        } catch (final Exception ex) {

            information = null;  // null indicates an error;
        }

        return information;
    }

    /**
     * Prints a warning message on the standard output stream.
     *
     * @param template
     *     template of the message to be written.
     * @param args
     *     arguments to be inserted into the template.
     */
    private void warn (final String template, final Object... args) {

        final String message = String.format(template, args);

        System.out.println("WARNING: " + message);
    }

    /**
     * Installs the outgoing transitions of a Markov State, according to the
     * given behavior information.
     *
     * @param markovState
     *     Markov State whose outgoing transitions shall be installed.
     * @param markovStates
     *     all Markov States of the related Behavior Model, including possible
     *     target states.
     * @param behaviorModelExitState
     *     exit state of the related Behavior Model, possibly being a target
     *     state.
     * @param behaviorInformation
     *     behavior information which indicates the transitions to be installed.
     */
    private void setMarkovStateTransitions (
            final MarkovState markovState,
            final List<MarkovState> markovStates,
            final BehaviorModelExitState behaviorModelExitState,
            final String[][] behaviorInformation) {

        final String[] row = this.findRowByStateName(
                markovState.getService().getName(),
                behaviorInformation);

        if (row != null) {  // behavior information available?

            final String[] headerRow = behaviorInformation[0];

            // ignore first (header) column  -->  start with index 1;
            for (int i = 1, n = headerRow.length; i < n; i++) {

                final String targetServiceName = headerRow[i];

                final BehaviorModelState targetState;
                final double probability;
                final ThinkTime thinkTime;

                if ( "$".equals(targetServiceName) ) {

                    targetState = behaviorModelExitState;

                } else {

                    targetState = this.findMarkovStateByServiceName(
                            targetServiceName,
                            markovStates);

                    if (targetState == null) {

                        this.warn(BehaviorModelsGenerator.
                                WARNING_UNKNOWN_TARGETSTATE,
                                targetServiceName);

                        continue;
                    }
                }

                final String entry = row[i];

                probability = this.extractProbability(entry);

                // do not add any transition, if probability equals 0;
                if (probability > 0) {

                    thinkTime = this.extractThinkTime(entry);

                    final Transition transition = this.createTransition(
                            targetState,
                            probability,
                            thinkTime);

                    markovState.getOutgoingTransitions().add(transition);
                }
            }
        }
    }

    /**
     * Searches for a Markov State which is associated with a specific service,
     * specified by its name.
     *
     * @param serviceName
     *     name of the service which is associated with the Markov State.
     * @param markovStates
     *     Markov States to be searched through.
     *
     * @return
     *     a matching Markov State, or <code>null</code> if no matching state
     *     exists.
     */
    private MarkovState findMarkovStateByServiceName(
            final String serviceName,
            final List<MarkovState> markovStates) {

        for (final MarkovState markovState : markovStates) {

            if ( serviceName.equals(markovState.getService().getName()) ) {

                return markovState;
            }
        }

        return null;  // no matching state for service name;
    }

    /**
     * Extracts the probability from a given behavior matrix entry; in case any
     * extraction error occurs, a warning will be given, and a default value
     * will be returned.
     *
     * @param str
     *     behavior matrix entry which consists of probability and think time
     *     definitions, both separated by semicolon.
     *
     * @return
     *     the (extracted) probability value, which might be 0.0 by default, if
     *     extraction fails.
     */
    private double extractProbability (final String str) {

        final String valueStr = str.substring(0, str.indexOf(';'));

        double probability;

        try {

            // might throw a NullPointer- or NumberFormatException;
            // (NullPointerException should be never thrown here);
            probability = Double.parseDouble(valueStr);

            if (probability < 0) {

                this.warn(
                        BehaviorModelsGenerator.WARNING_NEGATIVE_PROBABILITY,
                        probability);

                probability = 0.0d;
            }

        } catch (final NumberFormatException ex) {

            this.warn(
                    BehaviorModelsGenerator.WARNING_PROBABILITY_PARSING_FAILED,
                    valueStr);

            probability = 0.0d;
        }

        return probability;
    }

    /**
     * Extracts the think time from a given behavior matrix entry.
     *
     * @param str
     *     behavior matrix entry which consists of probability and think time
     *     definitions, both separated by semicolon.
     *
     * @return
     *     the extracted think time, or <code>null</code> if extraction fails.
     */
    private ThinkTime extractThinkTime (final String str) {

        final String thinkTimeStr =
                str.substring(str.indexOf(';') + 1, str.length()).trim();

        // parse() returns null, if parsing fails;
        return this.thinkTimeParser.parse(thinkTimeStr);
    }

    /**
     * Searches for a behavior matrix row which is associated with a specific
     * state name
     *
     * @param stateName
     *     state name whose associated row shall be found.
     * @param behaviorInformation
     *     matrix to be searched through.
     *
     * @return
     *     a matching row, or <code>null</code> if no matching row exists.
     */
    private String[] findRowByStateName (
            final String stateName,
            final String[][] behaviorInformation) {

        for (final String[] row : behaviorInformation) {

            if ( stateName.equals(row[0]) ) {

                return row;
            }
        }

        return null;  // no matching row for state name;
    }

    /**
     * Creates an M4J-DSL model component which represents a Behavior Model.
     *
     * @param name
     *     name of the Behavior Model.
     * @param filename
     *     filename of the Behavior Model; if the filename suffix does not
     *     indicate a CSV file, a ".csv" suffix will be added to the filename.
     *
     * @return
     *     the newly created M4J-DSL model component.
     */
    private BehaviorModel createBehaviorModel (
            final String name,
            final String filename) {

        final BehaviorModel behaviorModel = this.createEmptyBehaviorModel();

        behaviorModel.setFilename( this.csvFilename(filename) );
        behaviorModel.setName(name);

        return behaviorModel;
    }

    /**
     * Returns a CSV-file-indicating variant of a filename.
     *
     * @param filename
     *     filename which is assumed to indicate a CSV file by its suffix;
     *     otherwise, a ".csv" suffix will be added.
     *
     * @return
     *     the given filename, if it indicates a CSV file; otherwise, the
     *     filename with an added ".csv" suffix will be returned.
     */
    private String csvFilename (final String filename) {

        final String csvSuffix = BehaviorModelsGenerator.BEHAVIOR_FILE_SUFFIX;

        return filename.toLowerCase().endsWith(csvSuffix) ?
                filename : (filename + csvSuffix);
    }

    /**
     * Creates an M4J-DSL model component which represents a Markov State.
     *
     * @param service
     *     the service which is associated with the Markov State.
     *
     * @return
     *     the newly created M4J-DSL model component.
     */
    private MarkovState createMarkovState (final Service service) {

        final MarkovState markovState = this.m4jdslFactory.createMarkovState();

        // use the service name as a part of the ID for better readability;
        markovState.setEId(this.idGenerator.newId() + "_" + service.getName());

        markovState.setService(service);

        return markovState;
    }

    /**
     * Creates an M4J-DSL model component which represents an empty Behavior
     * Model, that is a model which only includes an exit state.
     *
     * @return
     *     the newly created M4J-DSL model component.
     */
    private BehaviorModel createEmptyBehaviorModel () {

        final BehaviorModel behaviorModel =
                this.m4jdslFactory.createBehaviorModel();

        final BehaviorModelExitState behaviorModelExitState =
                this.createBehaviorModelExitState();

        behaviorModel.setExitState(behaviorModelExitState);

        return behaviorModel;
    }

    /**
     * Creates an M4J-DSL model component which represents the exit state of
     * a Behavior Model.
     *
     * @return
     *     the newly created M4J-DSL model component.
     */
    private BehaviorModelExitState createBehaviorModelExitState () {

        final BehaviorModelExitState behaviorModelExitState =
                this.m4jdslFactory.createBehaviorModelExitState();

        behaviorModelExitState.setEId(this.idGenerator.newId());

        return behaviorModelExitState;
    }

    /**
     * Creates an M4J-DSL model component which represents a Behavior Model
     * transition.
     *
     * @param targetState
     *     target state of the transition.
     * @param probability
     *     probability of the transition.
     * @param thinkTime
     *     think time of the transition.
     *
     * @return
     *     the newly created M4J-DSL model component.
     */
    private Transition createTransition (
            final BehaviorModelState targetState,
            final double probability,
            final ThinkTime thinkTime) {

        final Transition transition = this.m4jdslFactory.createTransition();

        transition.setTargetState(targetState);
        transition.setProbability(probability);
        transition.setThinkTime(thinkTime);

        return transition;
    }


    /* *************************  internal classes  ************************* */


    /**
     * Class for parsing a token of a CSV-file which denotes a user think time.
     * A token has to be formatted as
     * <blockquote>
     *   <i>&#060;functionDescriptor&#062;</i>(<i>&#060;parameter1&#062; &#060;parameter2&#062; ... </i> )
     * </blockquote>
     * whereas <i>&#060;functionDescriptor&#062;</i> indicates the used
     * distribution function. The number of parameters depends on that
     * distribution.
     *
     * @author Eike Schulz (esc@informatik.uni-kiel.de)
     *
     * @version 1.0
     */
    private class ThinkTimeParser {

        /** Regular expression specifying the separator for parameters. */
        private final static String PARAMETER_SEPARATOR = "\\s+";

        /** Function descriptor for a normally distributed think time. */
        private final static String FDESCR__NORMALLY_DISTRIBUTED_TT = "n";

        /** Instance for creating M4J-DSL model elements. */
        private final M4jdslFactory m4jdslFactory;


        /**
         * Constructor for a think time parser.
         *
         * @param m4jdslFactory  instance for creating M4J-DSL model elements.
         */
        public ThinkTimeParser (final M4jdslFactory m4jdslFactory) {

            this.m4jdslFactory = m4jdslFactory;
        }


        /**
         * Parses a given <code>String</code> which specifies a think time
         * according to a certain distribution.
         *
         * @param str
         *     <code>String</code> which provides a distribution function
         *     descriptor and the required parameters as well. The leading
         *     function descriptor indicates the distribution type and therewith
         *     the type of {@link ThinkTime} to be returned. The passed
         *     <code>String</code> might be wrapped into whitespace, which will
         *     be removed before parsing starts.
         *
         * @return
         *     a valid instance of a {@link ThinkTime} sub-class, or
         *     <code>null</code> if parsing fails for any reason.
         */
        public ThinkTime parse (String str) {

            if (str != null) {

                str = str.trim();

                // ensure that at least one leading character exists prior the
                // first opening bracket; closing bracket must be at last
                // position;
                if ( str.indexOf('(') > 0 &&
                     str.lastIndexOf(')') == str.length() - 1) {

                    final String functionDescriptor =
                            this.extractFunctionDescriptor(str);

                    final String[] parameters =
                            this.extractParameters(str);

                    return this.createThinkTime(
                            functionDescriptor,
                            parameters);
                }
            }

            return null;  // invalid function String;
        }

        /**
         * Extracts the function descriptor which indicates the distribution
         * type.
         *
         * @param str
         *     <code>String</code> which specifies the distribution type as well
         *     as required parameters; it must contain at least an opening
         *     bracket.
         *
         * @return
         *     a valid <code>String</code> instance, or <code>null</code> if no
         *     function descriptor can be found.
         */
        private String extractFunctionDescriptor (final String str) {

            return str.split("\\(")[0].trim();
        }

        /**
         * Extracts the parameters which are required by the regarding
         * distribution type. The parameters will be returned as (unparsed)
         * <code>String</code>s.
         *
         * @param function
         *     <code>String</code> which specifies the distribution type as well
         *     as required parameters.
         *
         * @return
         *     a valid array of <code>String</code> instances; each entry
         *     denotes an unparsed parameter.
         */
        private String[] extractParameters (final String function) {

            final String str = function.substring(
                    function.indexOf('(') + 1,
                    function.length() - 1).trim();

            return str.split(ThinkTimeParser.PARAMETER_SEPARATOR);
        }

        /**
         * Creates an instance of a {@link ThinkTime} sub-class, depending on
         * the given function descriptor.
         *
         * @param functionDescriptor
         *     a <code>String</code> which indicates the think time distribution
         *     type.
         * @param parameters
         *     the (unparsed) function parameters required by the think time
         *     distribution type.
         *
         * @return
         *     a valid instance of a {@link ThinkTime} sub-class; if the given
         *     function descriptor is unknown or parameters are invalid,
         *     <code>null</code> will be returned.
         */
        private ThinkTime createThinkTime (
                final String functionDescriptor,
                final String[] parameters) {

            if (ThinkTimeParser.FDESCR__NORMALLY_DISTRIBUTED_TT.
                    equalsIgnoreCase(functionDescriptor)) {

                return this.createNormalDistributionThinkTime(parameters);
            }

            // more cases might be added for further distribution types;

            return null;  // null ~ unknown function descriptor;
        }

        /**
         * Creates a {@link NormallyDistributedThinkTime} instance, which will
         * be initialized with the parsed values of the given parameters.
         *
         * @param parameters
         *     the function parameters required by the think time distribution
         *     type.
         *
         * @return
         *     a valid instance of {@link NormallyDistributedThinkTime}, or
         *     <code>null</code> if any parameter parsing fails.
         */
        private NormallyDistributedThinkTime
        createNormalDistributionThinkTime (final String[] parameters) {

            NormallyDistributedThinkTime thinkTime = null;

            if (parameters.length == 2) {

                try {

                    // parseDouble() might throw a NumberFormatException;
                    final double mean      = Double.parseDouble(parameters[0]);
                    final double deviation = Double.parseDouble(parameters[1]);

                    thinkTime = this.createNormallyDistributedThinkTime(
                            mean,
                            deviation);

                } catch (final NumberFormatException ex) {

                    // keep thinkTime being null for indicating an error;
                }
            }

            return thinkTime;
        }

        /**
         * Creates a {@link NormallyDistributedThinkTime} instance, which will
         * be initialized with the given mean and deviation values.
         *
         * @param mean       mean value of the think time.
         * @param deviation  deviation value of the think time.
         *
         * @return  a valid instance of {@link NormallyDistributedThinkTime}.
         */
        private NormallyDistributedThinkTime createNormallyDistributedThinkTime(
                final double mean,
                final double deviation) {

            final NormallyDistributedThinkTime normallyDistributedThinkTime =
                    this.m4jdslFactory.createNormallyDistributedThinkTime();

            normallyDistributedThinkTime.setMean(mean);
            normallyDistributedThinkTime.setDeviation(deviation);

            return normallyDistributedThinkTime;
        }

    }  // internal class;
}