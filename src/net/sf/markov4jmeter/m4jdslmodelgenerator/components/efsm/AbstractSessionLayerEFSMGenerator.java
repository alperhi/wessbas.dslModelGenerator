package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import m4jdsl.ApplicationExitState;
import m4jdsl.ApplicationState;
import m4jdsl.ApplicationTransition;
import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.SessionLayerEFSMState;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.ServiceRepository;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

/**
 * Abstract base class for all Session Layer EFSM Generators. This class
 * provides methods for creating model elements, such as states and transitions.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public abstract class AbstractSessionLayerEFSMGenerator {

    /** Instance for storing the <code>Service</code> instances which are
     *  included in the Session Layer EFSM. */
    protected final ServiceRepository serviceRepository;

    /** Instance for creating M4J-DSL model elements. */
    protected final M4jdslFactory m4jdslFactory;

    /** Instance for creating Protocol Layer EFSMs. */
    // TODO: protocolLayerEFSMGenerator is never used in this class (yet);
    protected final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator;

    /** Instance for creating unique Application State IDs. */
    protected final IdGenerator idGenerator;


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
     */
    public AbstractSessionLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator,
            final IdGenerator idGenerator) {

        this.m4jdslFactory              = m4jdslFactory;
        this.serviceRepository          = serviceRepository;
        this.protocolLayerEFSMGenerator = protocolLayerEFSMGenerator;
        this.idGenerator                = idGenerator;
    }


    /* **************************  public methods  ************************** */


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
            final String guard,
            final String action) {

        final ApplicationTransition applicationTransition =
                this.m4jdslFactory.createApplicationTransition();

        applicationTransition.setTargetState(targetState);
        applicationTransition.setGuard(guard);
        applicationTransition.setAction(action);

        return applicationTransition;
    }
}