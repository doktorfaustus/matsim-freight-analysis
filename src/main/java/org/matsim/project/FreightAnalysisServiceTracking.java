package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPServiceStartEvent;

import java.util.HashMap;

public class FreightAnalysisServiceTracking {

	private HashMap<Id<Carrier>, ServiceTracker.CarrierServiceTracker> carrierServiceTrackers;

	{
		carrierServiceTrackers = new HashMap<>();
	}


	public HashMap<Id<CarrierService>, ServiceTracker> getTrackers(Id<Carrier> carrierId) { return carrierServiceTrackers.get(carrierId).serviceTrackers;}
	public HashMap<Id<Carrier>, ServiceTracker.CarrierServiceTracker> getCarrierServiceTrackers(){return carrierServiceTrackers;}

	public void addTracker(CarrierService service, Id<Carrier> id) {
		ServiceTracker st = new ServiceTracker(service);
		if(carrierServiceTrackers.containsKey(id)){
			carrierServiceTrackers.get(id).serviceTrackers.put(service.getId(), st);
		} else {
			carrierServiceTrackers.put(id, new ServiceTracker.CarrierServiceTracker(id, service));
		}
	}

	// handle activityStartEvents to track the start of a service activity in case LSP Events are not thrown
	public void trackServiceActivityStart(ActivityStartEvent activityStartEvent) {
		for (ServiceTracker.CarrierServiceTracker cst: carrierServiceTrackers.values()) {
			for (ServiceTracker service : cst.serviceTrackers.values()) {
				if (service.service.getLocationLinkId().equals(activityStartEvent.getLinkId())) {
					if (service.driverId == null) {
						// if there is no driver, but there is a service which is to be performed at the moment at this place, we guess this could be the event for it.
						// (Does not work well obviously as soon as there are multiple services at a location that have generous time windows, like e.g. at stores).
						if (service.service.getServiceStartTimeWindow().getStart() <= activityStartEvent.getTime() && activityStartEvent.getTime() <= service.service.getServiceStartTimeWindow().getEnd()) {
							service.driverIdGuess = activityStartEvent.getPersonId();
							service.arrivalTimeGuess = activityStartEvent.getTime();
						}
						// if (unlikely) the driver is known for the service and this event is thrown at the location of that service, we assume this event is meant for this particular service.
						// (doesn't work well either, because the driver is likely not known without LSP events, and then we don't have to make guesses anyway.)
					} else if (service.driverId.toString().equals(activityStartEvent.getPersonId().toString())) {
						service.startTime = activityStartEvent.getTime();
					}
				}
			}
		}
	}

	public void setExpectedArrival(Id<Carrier> carrierId, Id<CarrierService> serviceId, double expectedArrival) {
	if (carrierServiceTrackers.containsKey(carrierId)){
		if (carrierServiceTrackers.get(carrierId).serviceTrackers.containsKey(serviceId)){
			carrierServiceTrackers.get(carrierId).serviceTrackers.get(serviceId).expectedArrival=expectedArrival;
		}
	}
	}

	public void setCalculatedArrival(Id<Carrier> carrierId, Id<CarrierService> serviceId, Double calculatedArrival) {
		if (carrierServiceTrackers.containsKey(carrierId)){
			if (carrierServiceTrackers.get(carrierId).serviceTrackers.containsKey(serviceId)){
				carrierServiceTrackers.get(carrierId).serviceTrackers.get(serviceId).calculatedArrival=calculatedArrival;
			}
		}
	}

	// UNTESTED handling of LSP Service events that provided reliable info about driver and timestamps.
	public void handleStartEvent(LSPServiceStartEvent event) {
		if (carrierServiceTrackers.containsKey(event.getCarrierId())){
			if (carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.containsKey(event.getService().getId())){
				ServiceTracker service = carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.get(event.getService().getId());
				service.driverId = event.getDriverId();
				service.startTime = event.getTime();
			}
		}
	}
	public void handleEndEvent(LSPServiceEndEvent event) {
		if (carrierServiceTrackers.containsKey(event.getCarrierId())){
			if (carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.containsKey(event.getService().getId())){
				ServiceTracker service = carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.get(event.getService().getId());
				service.endTime = event.getTime();
			}
		}
	}

}

class ServiceTracker {
	public CarrierService service;
	public Double calculatedArrival =0.0;
	public Id<Carrier> carrierId ;
	public Id<Person> driverId ;
	public Double startTime =0.0;
	public Id<Person> driverIdGuess;
	public Double arrivalTimeGuess = 0.0;
	public double expectedArrival = 0.0;
	public double endTime;

	public ServiceTracker(CarrierService service) {
		this.service=service;
	}
	static class CarrierServiceTracker{
		Id<Carrier> carrierId;
		HashMap<Id<CarrierService>, ServiceTracker> serviceTrackers= new HashMap<>();

		public CarrierServiceTracker(Id<Carrier> id, CarrierService service) {
			this.serviceTrackers.put(service.getId(), new ServiceTracker(service));
			this.carrierId=id;
		}
	}
}
