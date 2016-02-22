/***************************************************************************
 * Copyright (c) 2016 the WESSBAS project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/


package net.sf.markov4jmeter.m4jdslmodelgenerator.components;

import java.util.HashMap;
import java.util.List;

import m4jdsl.BehaviorMix;
import m4jdsl.BehaviorModel;
import m4jdsl.M4jdslFactory;
import m4jdsl.RelativeFrequency;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;

/**
 * Generator class for creating M4J-DSL model components, which represent the
 * Behavior Mix.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class BehaviorMixGenerator {


    /* *****************************  constants  **************************** */


    /** Error message for the case that a Behavior Model of a certain name is
     *  unavailable. */
    private final static String ERROR_MODEL_UNAVAILABLE =
            "no model named \"%s\" available";


    /* *************************  global variables  ************************* */


    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;


    /* ***************************  constructors  *************************** */

    /**
     * Constructor for a Behavior Mix Generator.
     *
     * @param m4jdslFactory  instance for creating M4J-DSL model elements.
     */
    public BehaviorMixGenerator (final M4jdslFactory m4jdslFactory) {

        this.m4jdslFactory = m4jdslFactory;
    }


    /* **************************  public methods  ************************** */


    /**
     * Creates an M4J-DSL model component which represents a Behavior Mix.
     *
     * @param entries
     *     hash map which maps Behavior Model names (as keys) to their relative
     *     frequencies (as values).
     * @param behaviorModels
     *     M4J-DSL model components which represent the Behavior Models.
     *
     * @return
     *     the newly created M4J-DSL model component.
     *
     * @throws GeneratorException
     *     if any Behavior Model entry cannot be installed to the Behavior Mix.
     */
    public BehaviorMix generateBehaviorMix (
            final HashMap<String, Double> entries,
            final List<BehaviorModel> behaviorModels)
                    throws GeneratorException {

        final BehaviorMix behaviorMix = m4jdslFactory.createBehaviorMix();

        for (final String name : entries.keySet()) {

            final BehaviorModel behaviorModel =
                    this.findBehaviorModelByName(name, behaviorModels);

            if (behaviorModel == null) {

                final String message = String.format(
                        BehaviorMixGenerator.ERROR_MODEL_UNAVAILABLE,
                        name);

                throw new GeneratorException(message);
            }

            final RelativeFrequency relativeFrequency =
                    this.createRelativeFrequency(
                            behaviorModel,
                            entries.get(name));

            behaviorMix.getRelativeFrequencies().add(relativeFrequency);
        }

        return behaviorMix;
    }


    /* **************************  private methods  ************************* */


    /**
     * Creates an M4J-DSL model component which represents a relative frequency.
     *
     * @param behaviorModel
     *     M4J-DSL model component which represents a Behavior Model.
     * @param frequency
     *     frequency of the given Behavior Model.
     *
     * @return
     *     the newly created M4J-DSL model component.
     */
    private RelativeFrequency createRelativeFrequency (
            final BehaviorModel behaviorModel,
            final double frequency) {

        final RelativeFrequency relativeFrequency =
                this.m4jdslFactory.createRelativeFrequency();

        relativeFrequency.setBehaviorModel(behaviorModel);
        relativeFrequency.setValue(frequency);

        return relativeFrequency;
    }

    /**
     * Searches for a Behavior Model by name.
     *
     * @param name
     *     name of the Behavior Model to be searched for.
     * @param behaviorModels
     *     list of Behavior Models to be searched through.
     *
     * @return
     *     a matching Behavior Model, or <code>null</code> if such model does
     *     not exist.
     */
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
