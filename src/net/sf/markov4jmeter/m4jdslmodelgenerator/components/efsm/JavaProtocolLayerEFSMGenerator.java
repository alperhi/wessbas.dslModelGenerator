package net.sf.markov4jmeter.m4jdslmodelgenerator.components.efsm;

import m4jdsl.M4jdslFactory;
import m4jdsl.ProtocolExitState;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolState;
import m4jdsl.ProtocolTransition;
import m4jdsl.Request;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

/**
 * Class for building Protocol Layer EFSMs based on Java requests.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class JavaProtocolLayerEFSMGenerator
extends AbstractProtocolLayerEFSMGenerator {


    /* ***************************  constructors  *************************** */

    /**
     * Constructor for a Protocol Layer EFSM with Java requests.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     * @param idGenerator
     *     instance for creating unique Protocol State IDs.
     * @param requestIdGenerator
     *     instance for creating unique request IDs.
     */
    public JavaProtocolLayerEFSMGenerator (
            final M4jdslFactory m4jdslFactory,
            final IdGenerator idGenerator,
            final IdGenerator requestIdGenerator) {

        super(m4jdslFactory, idGenerator, requestIdGenerator);
    }


    /* **************************  public methods  ************************** */


    @Override
    public ProtocolLayerEFSM generateProtocolLayerEFSM (
            final String serviceName) throws GeneratorException {

        final ProtocolLayerEFSM protocolLayerEFSM =
                this.createEmptyProtocolLayerEFSM();

        final ProtocolExitState protocolExitState =
                protocolLayerEFSM.getExitState();

        // TODO: more information required for building SUT-specific requests and transitions;

        // might throw a GeneratorException;
        final Request request = this.createRequest(
                AbstractProtocolLayerEFSMGenerator.REQUEST_TYPE_JAVA);

        final ProtocolState protocolState = this.createProtocolState(request);

        final String guard;  // no SUT-specific guard available yet ...
        final String action;  // no SUT-specific action available yet ...

        final ProtocolTransition protocolTransition =
                this.createProtocolTransition(
                        protocolExitState,
                        "<guard>",
                        "<action>");

        protocolState.getOutgoingTransitions().add(protocolTransition);

        protocolLayerEFSM.getProtocolStates().add(protocolState);
        protocolLayerEFSM.setInitialState(protocolState);

        return protocolLayerEFSM;
    }
}
