package org.touge.osgiutil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.touge.osgiutil.OSGiUtil;
import org.touge.osgiutil.OSGiUtil.ServiceFollower;
import org.touge.osgiutil.OSGiUtil.ServiceVisitor;
import org.touge.osgiutil.OSGiUtil.TrackingCollection;

//# OSGiUtil Examples
public class Examples {

	public static void main(String[] args) {
		
		BundleContext context = null;
		
		//## Service binding patterns.
		
		//### You want to get a single reference to a specific service.  
		//You expect only one and you want the operation to fail if the service is not available for some reason.
				
		Service1 service = 
				(Service1) OSGiUtil.getServiceInstance(context, Service1.class.getName());
		
		if (service == null)
			System.out.println("Service was not available.  Panic.");
		
		
		//### You want to do something once a set of services is available.  
		// You only want to know when the complete set exists.
		// First define the services you want
		String [] services = { 
				Service1.class.getName(),
				Service2.class.getName(),
				Service3.class.getName()};
		
		//Then call .withServices() and define a ServiceFollower.
		OSGiUtil.withServices(context, Arrays.asList(services) , new ServiceFollower() {
			
			@Override
			public void unavailable(Object service) {
				// Shutdown whatever needs to be shutdown, complete set of services no longer available.
				
			}
			
			@Override
			public void allAvailable(Map<String, Object> services, Map<String, ServiceReference<?>> references) {
				// The services map contains the complete set of services.  The key is the service name, the value is a reference to the service.
				// The map of references can be used to access the properties associated with the services.
			}
		});
		
		// ### You want to act upon a set of instances that all implement the same service interface.  
		// This is common with whiteboard-style services.
		OSGiUtil.onServices(context, Service2.class.getName(), null, new ServiceVisitor<Service2>() {

			@Override
			public void apply(ServiceReference<?> sr, Service2 service) {
				//This method is called on all instances of Service2 in the registry at the time of call.
			}
			
		});
		
		// ### You want to maintain a collection of services that may be referenced at any time.  
		// The collection also allows to isolate dependent code from the OSGi APIs.
		TrackingCollection<Service3> serviceCollection = OSGiUtil.trackingCollection(context, Service3.class.getName());
		
		//A TrackingCollection adds close() to the Collection interface to stop the collection from actively tracking services.
		if (!serviceCollection.isEmpty()) {
			// Do something with the services.
		}
		
		//Finished with the services
		serviceCollection.close();
		
		//Similar to the TrackingCollection but allows the client to define the collection to be populated.
		Collection<Service3> svcs = new ArrayList<Service3>();
		ServiceTracker tracker = OSGiUtil.collectServices(context, Service3.class.getName(), svcs);
		
		//Do something with the services
		for (Service3 s : svcs)
			System.out.println(s.toString());
			
		//Close the service tracker
		tracker.close();
	}
	
	//Example service 1
	interface Service1 {
		
	}
	//Example service 2
	interface Service2 {
		
	}
	
	//Example service 3
	interface Service3 {
		
	}
}
