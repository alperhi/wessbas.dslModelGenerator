package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.util.HashMap;
import java.util.List;

import m4jdsl.BehaviorMix;
import m4jdsl.BehaviorModel;
import m4jdsl.M4jdslFactory;
import m4jdsl.RelativeFrequency;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;

public class BehaviorMixGenerator {

    private final M4jdslFactory m4jdslFactory;

    
    public BehaviorMixGenerator (final M4jdslFactory m4jdslFactory) {

        this.m4jdslFactory = m4jdslFactory;
    }
    
    
    public BehaviorMix generateBehaviorMix (
            final HashMap<String, Double> entries,
            final List<BehaviorModel> behaviorModels) throws GeneratorException {

        final BehaviorMix behaviorMix = m4jdslFactory.createBehaviorMix();

        for (final String name : entries.keySet()) {

            final BehaviorModel behaviorModel = this.findBehaviorModelByName(name, behaviorModels);

            if (behaviorModel == null) {

                throw new GeneratorException("no model named \"" + name + "\" available");
            }

            final RelativeFrequency relativeFrequency = m4jdslFactory.createRelativeFrequency();

            relativeFrequency.setBehaviorModel(behaviorModel);
            relativeFrequency.setValue(entries.get(name));

            behaviorMix.getRelativeFrequencies().add(relativeFrequency);
        }

        return behaviorMix;
    }
    
    private BehaviorModel findBehaviorModelByName (
            final String name,
            final List<BehaviorModel> behaviorModels) {

        for (final BehaviorModel behaviorModel : behaviorModels) {

            if (behaviorModel.getName().equals(name)) {

                return behaviorModel;
            }
        }
        
        return null;  // no match;
    }
}
