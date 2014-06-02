package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolLayerEFSMState;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class AbstractProtocolLayerEFSMGenerator {

    protected final M4jdslFactory m4jdslFactory;
    protected final IdGenerator idGenerator;
    protected final IdGenerator requestIdGenerator;


    public AbstractProtocolLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final IdGenerator requestIdGenerator) {

        this.m4jdslFactory     = m4jdslFactory;
        this.idGenerator        = idGenerator;
        this.requestIdGenerator = requestIdGenerator;
    }


    protected ProtocolLayerEFSM createEmptyProtocolLayerEFSM () {

        final ProtocolLayerEFSM protocolLayerEFSM =
                this.m4jdslFactory.createProtocolLayerEFSM();

        final ProtocolExitState protocolExitState =
                this.createProtocolExitState();

        protocolLayerEFSM.setExitState(protocolExitState);

        return protocolLayerEFSM;
    }

    protected ProtocolState createProtocolState (final Request request) {

        final ProtocolState protocolState =
                this.m4jdslFactory.createProtocolState();

        protocolState.setEId(this.idGenerator.newId());
        protocolState.setRequest(request);

        return protocolState;
    }

    protected ProtocolExitState createProtocolExitState () {

        final ProtocolExitState protocolExitState =
                this.m4jdslFactory.createProtocolExitState();

        protocolExitState.setEId(this.idGenerator.newId());

        return protocolExitState;
    }

    protected ProtocolTransition createProtocolTransition (
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
