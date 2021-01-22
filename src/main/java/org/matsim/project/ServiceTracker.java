package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

public class ServiceTracker {
	public Id<CarrierService> serviceId;
	public double expectedArrival;
	public Id<Link> linkId=Id.createLinkId(-1);
	public Double calculatedArrival;
	public Id<Carrier> carrierId = Id.create(-1,Carrier.class);
	public Id<Person> driverId = Id.createPersonId(-1);
	public Double arrivalTime;
	Double scheduledStartTime;
Boolean onTimeArrival;
Boolean onTimeDelivery;
	public ServiceTracker(CarrierService service) {
		this.serviceId=service.getId();
		service.getServiceStartTimeWindow();
		service.getServiceDuration();
		service.getType();
	}
}
