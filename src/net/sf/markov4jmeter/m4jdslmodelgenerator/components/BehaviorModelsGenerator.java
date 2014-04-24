package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.util.LinkedList;

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
import net.sf.markov4jmeter.m4jdslmodelgenerator.util.IdGenerator;

public class BehaviorModelsGenerator {

    private final ServiceRepository serviceRepository;
    private final M4jdslFactory m4jdslFactory;
    private final IdGenerator idGenerator;


    public BehaviorModelsGenerator (
            final M4jdslFactory m4jdslFactory,
            final ServiceRepository serviceRepository,
            final IdGenerator idGenerator) {

        this.serviceRepository = serviceRepository;
        this.m4jdslFactory     = m4jdslFactory;
        this.idGenerator       = idGenerator;
    }


    public LinkedList<BehaviorModel> generateBehaviorModels () throws GeneratorException {

        final LinkedList<BehaviorModel> behaviorModels =
                new LinkedList<BehaviorModel>();

        final BehaviorModel behaviorModel = this.generateBehaviorModel();

        behaviorModels.add(behaviorModel);
        return behaviorModels;
    }

    public BehaviorModel generateBehaviorModel () throws GeneratorException {

        final BehaviorModel behaviorModel = this.createEmptyBehaviorModel();

        final BehaviorModelExitState behaviorModelExitState =
                behaviorModel.getExitState();

        // TODO: retrieve structure from Behavior Models;
        final String serviceName = FlowSessionLayerEFSMGenerator.INITIAL_STATE__SERVICE_NAME;
        final Service service = this.serviceRepository.findServiceByName(serviceName);

        if (service == null) {

            throw new GeneratorException(
                    "unknown service in Behavior Model detected: " + serviceName);
        }

        final MarkovState markovState = this.createMarkovState(service);

        final NormallyDistributedThinkTime normallyDistributedThinkTime =
                m4jdslFactory.createNormallyDistributedThinkTime();

        normallyDistributedThinkTime.setMean(5);
        normallyDistributedThinkTime.setDeviation(2);

        final Transition transition = this.createTransition(
                behaviorModelExitState, 1.0, normallyDistributedThinkTime);

        markovState.getOutgoingTransitions().add(transition);

        behaviorModel.getMarkovStates().add(markovState);
        behaviorModel.setInitialState(markovState);
        behaviorModel.setFilename("filename");
        behaviorModel.setName("name");

        return behaviorModel;
    }

    private MarkovState createMarkovState (final Service service) {

        final MarkovState markovState = this.m4jdslFactory.createMarkovState();

        markovState.setEId(this.idGenerator.newId());
        markovState.setService(service);

        return markovState;
    }

    private BehaviorModel createEmptyBehaviorModel () {

        final BehaviorModel behaviorModel =
                this.m4jdslFactory.createBehaviorModel();

        final BehaviorModelExitState behaviorModelExitState =
                this.createBehaviorModelExitState();

        behaviorModel.setExitState(behaviorModelExitState);

        return behaviorModel;
    }

    private BehaviorModelExitState createBehaviorModelExitState () {

        final BehaviorModelExitState behaviorModelExitState =
                this.m4jdslFactory.createBehaviorModelExitState();

        behaviorModelExitState.setEId(this.idGenerator.newId());

        return behaviorModelExitState;
    }

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
}
