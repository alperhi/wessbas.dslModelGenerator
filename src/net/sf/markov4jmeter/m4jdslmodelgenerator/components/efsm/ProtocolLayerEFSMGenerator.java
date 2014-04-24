package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolLayerEFSMState;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class ProtocolLayerEFSMGenerator {

    private final M4jdslFactory m4jdslFactory;
    private final IdGenerator idGenerator;
    private final IdGenerator requestIdGenerator;
    

    public ProtocolLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final IdGenerator requestIdGenerator) {

        this.m4jdslFactory     = m4jdslFactory;
        this.idGenerator        = idGenerator;
        this.requestIdGenerator = requestIdGenerator;
    }

    public ProtocolLayerEFSM generateProtocolLayerEFSM () {

        final ProtocolLayerEFSM protocolLayerEFSM = this.createEmptyProtocolLayerEFSM();
        final ProtocolExitState protocolExitState = protocolLayerEFSM.getExitState();

        final Request request = this.createRequest(0);  // TODO: provide more information;
        final ProtocolState protocolState = this.createProtocolState(request);

        final ProtocolTransition protocolTransition =
                this.createProtocolTransition(protocolExitState, "<guard>", "<action>");

        protocolState.getOutgoingTransitions().add(protocolTransition);

        protocolLayerEFSM.getProtocolStates().add(protocolState);
        protocolLayerEFSM.setInitialState(protocolState);

        return protocolLayerEFSM;
    }

    private Request createRequest (int type) {

        // TODO: retrieve request from Gear, support further types;
        final Request request = this.m4jdslFactory.createJavaRequest();

        request.setEId(this.requestIdGenerator.newId());
        return request;
    }

    private ProtocolLayerEFSM createEmptyProtocolLayerEFSM () {

        final ProtocolLayerEFSM protocolLayerEFSM =
                this.m4jdslFactory.createProtocolLayerEFSM();

        final ProtocolExitState protocolExitState =
                this.createProtocolExitState();

        protocolLayerEFSM.setExitState(protocolExitState);

        return protocolLayerEFSM;
    }

    private ProtocolState createProtocolState (final Request request) {

        final ProtocolState protocolState =
                this.m4jdslFactory.createProtocolState();

        protocolState.setEId(this.idGenerator.newId());
        protocolState.setRequest(request);

        return protocolState;
    }

    private ProtocolExitState createProtocolExitState () {

        final ProtocolExitState protocolExitState =
                this.m4jdslFactory.createProtocolExitState();

        protocolExitState.setEId(this.idGenerator.newId());

        return protocolExitState;
    }

    private ProtocolTransition createProtocolTransition (
            final ProtocolLayerEFSMState targetState,
            final String guard,
            final String action) {

        final ProtocolTransition protocolTransition =
                this.m4jdslFactory.createProtocolTransition();

        protocolTransition.setTargetState(targetState);
        protocolTransition.setGuard(guard);
        protocolTransition.setAction(action);

        return protocolTransition;
    }
}
