package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class JavaProtocolLayerEFSMGenerator extends AbstractProtocolLayerEFSMGenerator {

    public JavaProtocolLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final IdGenerator requestIdGenerator) {

        super(m4jdslFactory, idGenerator, requestIdGenerator);
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

        // TODO: build request Gear-specific, support further types;
        final Request request = this.m4jdslFactory.createJavaRequest();

        request.setEId(this.requestIdGenerator.newId());
        return request;
    }
}
