package net.sf.markov4jmeter.m4jdslmodelgenerator;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import m4jdsl.M4jdslFactory;
import m4jdsl.Service;

/**
 * Repository class for handling all available services.
 *
 * @author   Eike Schulz (esc@informatik.uni-kiel.de)
 * @version  1.0
 */
public class ServiceRepository {

    /** Instance for creating M4J-DSL model elements. */
    private final M4jdslFactory m4jdslFactory;

    /** List of all registered services. */
    private final List<Service> services;


    /* ***************************  constructors  *************************** */


    /**
     * Constructor for a Service Repository.
     *
     * @param m4jdslFactory
     *     instance for creating M4J-DSL model elements.
     */
    public ServiceRepository (final M4jdslFactory m4jdslFactory) {

        this.m4jdslFactory = m4jdslFactory;
        this.services      = new LinkedList<Service>();
    }


    /* **************************  public methods  ************************** */


    /**
     * Returns all registered services.
     *
     * @return  a valid list of all registered services.
     */
    public List<Service> getServices () {

        return this.sortServices(this.services);
    }

    /**
     * Registers a service by name.
     *
     * @param name
     *     name of the service.
     *
     * @return
     *     a newly created service, if the service did not exist before;
     *     otherwise, the existing service will be returned.
     */
    public Service registerServiceByName (final String name) {

        Service service = this.findServiceByName(name);

        if (service != null) {

            return service;
        }

        service = this.m4jdslFactory.createService();
        service.setName(name);

        this.services.add(service);

        return service;
    }

    /**
     * Searches for a service of a given name.
     *
     * @param name
     *     name of the service to be searched for.
     *
     * @return
     *     a service of the given name if available;
     *     otherwise, <code>null</code> will be returned.
     */
    public Service findServiceByName (final String name) {

        for (final Service service : this.services) {

            if ( name.equals(service.getName()) ) {

                return service;
            }
        }

        return null;  // no match;
    }


    /* **************************  private methods  ************************* */


    /**
     * Sorts the list of services by name.
     *
     * @param services  List of services to be sorted.
     *
     * @return  the sorted list of services.
     */
    private List<Service> sortServices (final List<Service> services) {

        java.util.Collections.sort(services, new Comparator<Service>() {

            @Override
            public int compare (final Service s1, final Service s2) {

                return s1.getName().compareTo(s2.getName());
            }
        });

        return services;
    }
}
