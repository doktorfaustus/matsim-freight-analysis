package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;

public class ServiceTracker {
	public double expectedArrival;
	public Id<Link> linkId;
	public Double calculatedArrival;
	Double scheduledStartTime;
Boolean onTimeArrival;
Boolean onTimeDelivery;
	public ServiceTracker(CarrierService service) {
		service.getId();
		service.getServiceStartTimeWindow();
		service.getServiceDuration();
		service.getType();
	}
}
