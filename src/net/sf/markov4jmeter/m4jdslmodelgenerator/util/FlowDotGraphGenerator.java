package net.sf.markov4jmeter.m4jdslmodelgenerator.util;

import org.apache.commons.lang.StringEscapeUtils;

public class FlowDotGraphGenerator extends DotGraphGenerator {


    /* ***************************  constructors  *************************** */


    public FlowDotGraphGenerator () {

        super();
    }


    /* **************************  public methods  ************************** */


    public void addTransition(
            final String source,
            final String target,
            final String style,
            final String guard,
            final String action) {

        if ( !"".equals(guard) || !"".equals(action) ) {

            // note that the quotes of guards and action must be escaped;
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
