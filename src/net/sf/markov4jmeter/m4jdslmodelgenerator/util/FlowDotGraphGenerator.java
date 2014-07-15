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
