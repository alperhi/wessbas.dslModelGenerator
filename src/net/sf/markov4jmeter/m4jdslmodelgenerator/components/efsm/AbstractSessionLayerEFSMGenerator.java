package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import java.io.IOException;

import org.eclipse.emf.common.util.EList;

import m4jdsl.Action;
import m4jdsl.ApplicationExitState;
import m4jdsl.ApplicationState;
import m4jdsl.ApplicationTransition;
import m4jdsl.Guard;
import m4jdsl.GuardActionParameterList;
import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.SessionLayerEFSMState;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.ServiceRepository;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.DotGraphGenerator;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

/**
 * Abstract base class for all Session Layer EFSM Generators. This class
 * provides methods for creating model elements, such as states and transitions.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public abstract class AbstractSessionLayerEFSMGenerator {


    /* *****************************  constants  **************************** */


    /** Warning message for the case that the graph output file could not be
     *  written successfully. */
    private final static String WARNING_GRAPH_OUTPUT_FILE_COULD_NOT_BE_WRITTEN =
            "graph output file could not be written to \"%s\"";


    /* *************************  global variables  ************************* */


    /** Instance for storing the <code>Service</code> instances which are
     *  included in the Session Layer EFSM. */
    protected final ServiceRepository serviceRepository;

    /** Instance for creating M4J-DSL model elements. */
    protected final M4jdslFactory m4jdslFactory;

    /** Instance for creating Protocol Layer EFSMs. */
    // TODO: protocolLayerEFSMGenerator is never used in this class (yet);
    protected final AbstractProtocolLayerEFSMGenerator protocolLayerEFSMGenerator;

    /** Instance for creating unique Application State IDs. */
    protected final IdGenerator idGenerator;

    /** Instance for generating DOT graph output. */
    protected final DotGraphGenerator dotGraphGenerator;

    /** Path of the DOT graph output file; might be <code>null</code>, if no
     *  graph shall be generated. */
    private final String graphFilePath;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Session Layer EFSM Generator.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param serviceRepository
     *     instance for storing the <code>Service</code> instances which are
     *     included in the Session Layer EFSM.
     * @param protocolLayerEFSMGenerator
     *     instance for creating Protocol Layer EFSMs.
     * @param idGenerator
     *     instance for creating unique Application State IDs.
     * @param graphFilePath
     *     Path of the DOT graph output file; might be <code>null</code>, if no
     *     graph shall be generated.
     */
    public AbstractSessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final AbstractProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator,
            final String graphFilePath,
            final DotGraphGenerator dotGraphGenerator) {

        this.m4jdslFactory              = m4jdslFactory;
        this.serviceRepository          = serviceRepository;
        this.protocolLayerEFSMGenerator = protocolLayerEFSMGenerator;
        this.idGenerator                = idGenerator;
        this.graphFilePath              = graphFilePath;
        this.dotGraphGenerator          = dotGraphGenerator;
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates a Session Layer EFSM and writes its DOT graph.
     *
     * @return
     *     the newly created Session Layer EFSM.
     *
     * @throws GeneratorException
     *     in case the Session Layer EFSM cannot be created for any reason.
     */
    public SessionLayerEFSM generateSessionLayerEFSMAndWriteDotGraph ()
            throws GeneratorException {

        this.flushDotGraph();

        final SessionLayerEFSM sessionLayerEFSM =
                this.generateSessionLayerEFSM();

        this.writeDotGraph(this.graphFilePath);

        return sessionLayerEFSM;
    }

    /**
     * Creates a Session Layer EFSM.
     *
     * @return
     *     the newly created Session Layer EFSM.
     *
     * @throws GeneratorException
     *     in case the Session Layer EFSM cannot be created for any reason.
     */
    public abstract SessionLayerEFSM generateSessionLayerEFSM ()
            throws GeneratorException;


    /* *************************  protected methods  ************************ */


    /**
     * Creates a <code>Service</code> instance and registers it in the
     * repository; in case the service already exists, the available instance
     * will be returned.
     *
     * @param name  name of the service.
     *
     * @return  the newly created Service.
     */
    protected Service createService (final String name) {

        final Service service =
                this.serviceRepository.registerServiceByName(name);

        return service;
    }
    
    /**
     * Creates an empty Session Layer EFSM, which is an instance that only
     * includes an exit state.
     *
     * @param exitStateId  ID of the exit state.
     *
     * @return  the newly created Session Layer EFSM.
     */
    protected SessionLayerEFSM createEmptySessionLayerEFSM (
            final String exitStateId) {

        final SessionLayerEFSM sessionLayerEFSM =
                this.m4jdslFactory.createSessionLayerEFSM();

        final ApplicationExitState applicationExitState =
                this.createApplicationExitState();

        applicationExitState.setEId(exitStateId);
        sessionLayerEFSM.setExitState(applicationExitState);

        return sessionLayerEFSM;
    }
       
    /**
     * Creates an Application State.
     *
     * @param service
     *     service to be associated with the state.
     * @param protocolLayerEFSM
     *     Protocol Layer EFSM to be associated with the state.
     *
     * @return  the newly created Application State.
     */
    protected ApplicationState createApplicationState (
            final Service service,
            final ProtocolLayerEFSM protocolLayerEFSM) {

        final ApplicationState applicationState =
                this.m4jdslFactory.createApplicationState();

        // use the service name as a part of the ID for better readability;
        final String id = this.idGenerator.newId() + "_" + service.getName();

        // final String id = this.idGenerator.newId();

        applicationState.setEId(id);
        applicationState.setService(service);
        applicationState.setProtocolDetails(protocolLayerEFSM);

        return applicationState;
    }

    /**
     * Creates an Application Exit State, which is an Application State that
     * represents the exit state of an EFSM.
     *
     * @return  the newly created Application Exit State.
     */
    protected ApplicationExitState createApplicationExitState () {

        final ApplicationExitState applicationExitState =
                this.m4jdslFactory.createApplicationExitState();

        applicationExitState.setEId(this.idGenerator.newId());

        return applicationExitState;
    }

    /**
     * Creates a new GuardActionParameterList
     * 
     * @return GuardActionParameterList
     */
    protected GuardActionParameterList createGuardActionParamterList() {
    	final GuardActionParameterList guardActionParameterList = 
    			this.m4jdslFactory.createGuardActionParameterList();
    	return guardActionParameterList;
    }
    
    /**
     * Creates an Application Transition, including guard and action.
     *
     * @param targetState  target state of the transition.
     * @param guard        guard of the transition.
     * @param action       action of the transition.
     *
     * @return  the newly created Application Transition.
     */
    protected ApplicationTransition createApplicationTransition (
            final SessionLayerEFSMState targetState,
            final EList<Guard> guards,
            final EList<Action> actions) {

        final ApplicationTransition applicationTransition =
                this.m4jdslFactory.createApplicationTransition();

        applicationTransition.setTargetState(targetState);
        applicationTransition.getGuard().addAll(guards);
        applicationTransition.getAction().addAll(actions);

        return applicationTransition;
    }


    /**
     * Registers a state to be generated.
     *
     * @param name
     *     name of the state for identification purposes.
     * @param shape
     *     shape of the state, must be one of the <code>SHAPE</code> constants
     *     of class {@link DotGraphGenerator}.
     */
    protected void addDotState (final String name, final String shape) {

        if (this.dotGraphGenerator != null) {

            this.dotGraphGenerator.addState(name, shape);
        }
    }

    /**
     * Registers a transition to be generated.
     *
     * @param source
     *     name of the source state.
     * @param target
     *     name of the target state.
     * @param style
     *     style of the transition, must be one of the <code>STYLE</code>
     *     constants of class {@link DotGraphGenerator}.
     * @param label
     *     label of the transition for representation purposes.
     */
    protected void addDotTransition (
            final String source,
            final String target,
            final String style,
            final String label) {

        if (this.dotGraphGenerator != null) {

            this.dotGraphGenerator.addTransition(
                    source,
                    target,
                    style,
                    label);
        }
    }

    /**
     * Registers a transition to be generated, without a label.
     *
     * @param source
     *     name of the source state.
     * @param target
     *     name of the target state.
     * @param style
     *     style of the transition, must be one of the <code>STYLE</code>
     *     constants of class {@link DotGraphGenerator}.
     */
    protected void addDotTransition (
            final String source,
            final String target,
            final String style) {

        if (this.dotGraphGenerator != null) {

            this.dotGraphGenerator.addTransition(
                    source,
                    target,
                    style);
        }
    }


    /* **************************  private methods  ************************* */


    /**
     * Flushes all registered states and transitions of the current DOT graph.
     */
    private void flushDotGraph () {

        if (this.dotGraphGenerator != null) {

            this.dotGraphGenerator.flush();
        }
    }

    /**
     * Writes the DOT graph to a specific output file; in case the file cannot
     * be written, a warning will be given on standard output.
     *
     * @param filePath  path to the output file.
     */
    private void writeDotGraph (final String filePath) {

        if (this.dotGraphGenerator != null) {

            try {

                // might throw a Security- or IOException; throws a
                // NullPointerException, if "filePath" is null;
                this.dotGraphGenerator.writeGraphToFile(filePath);

            } catch (final SecurityException
                         | IOException
                         | NullPointerException ex) {

                final String message = String.format(
                        AbstractSessionLayerEFSMGenerator.
                        WARNING_GRAPH_OUTPUT_FILE_COULD_NOT_BE_WRITTEN,
                        filePath);

                System.out.println(message);
            }
        }
    }
}