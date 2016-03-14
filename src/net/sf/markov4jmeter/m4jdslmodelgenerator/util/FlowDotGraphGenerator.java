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


package net.sf.markov4jmeter.m4jdslmodelgenerator.util;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Class for generating DOT graphs which consist of states and transitions.
 * Transition labels are formatted uniformly, consisting of guards and actions.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class FlowDotGraphGenerator extends DotGraphGenerator {


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Flow DOT Graph Generator
     */
    public FlowDotGraphGenerator () {

        super();
    }


    /* **************************  public methods  ************************** */


    /**
     * Registers a transition to be generated; the transition label will be
     * built of a guard and an action, formatted as
     * [<i>guard</i>]/[<i>action</i>].
     *
     * @param source
     *     name of the source state.
     * @param target
     *     name of the target state.
     * @param style
     *     style of the transition, must be one of the <code>STYLE</code>
     *     constants.
     * @param guard
     *     guard of the transition.
     * @param action
     *     action of the transition.
     */
    public void addTransition(
            final String source,
            final String target,
            final String style,
            final String guard,
            final String action) {

        if ( !"".equals(guard) || !"".equals(action) ) {

            // note that the quotes of guards and actions must be escaped;
            final String label = "["
                    + StringEscapeUtils.escapeJava(guard)
                    + "] / ["
                    + StringEscapeUtils.escapeJava(action)
                    + "]";

            this.addTransition(source, target, style, label);

        } else {

            this.addTransition(source, target, style);
        }
    }
}
