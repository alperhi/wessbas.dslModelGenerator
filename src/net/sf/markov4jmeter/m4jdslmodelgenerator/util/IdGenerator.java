package net.sf.markov4jmeter.m4jdslmodelgenerator.util;

public class IdGenerator {

    private final static long START_ID = 1;
    private final static String DEFAULT_PREFIX = "id";

    private long idCounter;
    private String prefix;

    
    public IdGenerator (final String prefix) {

        this.prefix    = prefix;
        this.idCounter = IdGenerator.START_ID;        
    }
    
    public IdGenerator() {

        this(IdGenerator.DEFAULT_PREFIX);
    }

    public String getPrefix () {

        return this.prefix;
    }
    
    public void reset () {

        this.idCounter = IdGenerator.START_ID;
    }
    
    public String newId () {

        return this.prefix + this.idCounter++;
    }
}
