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

public abstract class AbstractSessionLayerEFSMGenerator {

    protected final ServiceRepository serviceRepository;
    protected final M4jdslFactory m4jdslFactory;

    // TODO: protocolLayerEFSMGenerator is never used in this class;
    protected final JavaProtocolLayerEFSMGenerator protocolLayerEFSMGenerator;
    protected final IdGenerator idGenerator;


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

    public abstract SessionLayerEFSM generateSessionLayerEFSM () throws GeneratorException;


    protected Service createService (final String name) {

        final Service service =
                this.serviceRepository.registerServiceByName(name);

        return service;
    }

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

    protected ApplicationState createApplicationState (
            final Service service,
            final ProtocolLayerEFSM protocolLayerEFSM) {

        final ApplicationState applicationState =
                this.m4jdslFactory.createApplicationState();

//        applicationState.setEId(this.idGenerator.newId());
        applicationState.setEId(this.idGenerator.getPrefix() + "_" + service.getName());  // use service name as ID for better readability;
        applicationState.setService(service);
        applicationState.setProtocolDetails(protocolLayerEFSM);

        return applicationState;
    }

    protected ApplicationExitState createApplicationExitState () {

        final ApplicationExitState applicationExitState =
                this.m4jdslFactory.createApplicationExitState();

        applicationExitState.setEId(this.idGenerator.newId());

        return applicationExitState;
    }

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