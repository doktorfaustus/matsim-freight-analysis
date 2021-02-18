package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPServiceStartEvent;

import java.util.HashMap;

public class FreightAnalysisServiceTracking {

	private HashMap<Id<CarrierService>, ServiceTracker> trackers = new HashMap<>();
	public HashMap<Id<CarrierService>, ServiceTracker> getTrackers() { return trackers; }

	public void addTracker(CarrierService service) {
	trackers.put(service.getId(), new ServiceTracker(service));
	}

	public void addTracker(CarrierService service, Id<Carrier> id) {
		this.addTracker(service);
		trackers.get(service.getId()).carrierId=id;
	}

	// handle activityStartEvents to track the start of a service activity in case LSP Events are not thrown
	public void trackServiceActivityStart(ActivityStartEvent activityStartEvent) {
		for (ServiceTracker service : trackers.values()){
			if (service.service.getLocationLinkId().equals(activityStartEvent.getLinkId())){
				if(service.driverId == null){
					// if there is no driver, but there is a service which is to be performed at the moment at this place, we guess this could be the event for it.
					// (Does not work well obviously as soon as there are multiple services at a location that have generous time windows, like e.g. at stores).
					if(service.service.getServiceStartTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=service.service.getServiceStartTimeWindow().getEnd()){
							service.driverIdGuess = activityStartEvent.getPersonId();
							service.arrivalTimeGuess=activityStartEvent.getTime();
					}
					// if (unlikely) the driver is known for the service and this event is thrown at the location of that service, we assume this event is meant for this particular service.
					// (Doesnt work well either, because the driver is likely not known without LSP events, and then we dont' have to make guesses anyway.)
				} else if (service.driverId.toString().equals(activityStartEvent.getPersonId().toString())){
					service.startTime =activityStartEvent.getTime();
				}
			}
		}
	}

	public void setExpectedArrival(Id<CarrierService> serviceId, double expectedArrival) {
		trackers.get(serviceId).expectedArrival=expectedArrival;
	}

	public void setCalculatedArrival(Id<CarrierService> serviceId, Double calculatedArrivalTime) {
		trackers.get(serviceId).calculatedArrival=calculatedArrivalTime;
	}

	// UNTESTED handling of LSP Service events that provied reliable info about driver and timestamps.
	public void handleStartEvent(LSPServiceStartEvent event) {
		ServiceTracker service = trackers.get(event.getService().getId());
		service.driverId = event.getDriverId();
		service.startTime = event.getTime();
	}
	public void handleEndEvent(LSPServiceEndEvent event) {
		ServiceTracker service = trackers.get(event.getService().getId());
		service.endTime = event.getTime();
	}

}
