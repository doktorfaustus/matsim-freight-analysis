package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPServiceStartEvent;

import java.util.HashMap;

public class FreightAnalysisServiceTracking {
	public HashMap<Id<CarrierService>, ServiceTracker> getTrackers() {
		return trackers;
	}

	private HashMap<Id<CarrierService>, ServiceTracker> trackers = new HashMap<>();

	public void addTracker(CarrierService service) {
	trackers.put(service.getId(), new ServiceTracker(service));
	}
	public void addTracker(CarrierService service, Id<Carrier> id) {
		this.addTracker(service);
		trackers.get(service.getId()).carrierId=id;
	}

	public void setCalculatedArrival(Id<CarrierService> serviceId, Double calculatedArrivalTime) {
		trackers.get(serviceId).calculatedArrival=calculatedArrivalTime;
	}

	public void trackServiceActivityStart(ActivityStartEvent activityStartEvent) {
		for (ServiceTracker service : trackers.values()){
			if (service.service.getLocationLinkId().equals(activityStartEvent.getLinkId())){
				if(service.driverId == null){
					if(service.service.getServiceStartTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=service.service.getServiceStartTimeWindow().getEnd()){
							service.driverIdGuess = activityStartEvent.getPersonId();
							service.arrivalTimeGuess=activityStartEvent.getTime();
					}
				} else if (service.driverId.toString().equals(activityStartEvent.getPersonId().toString())){
					service.startTime =activityStartEvent.getTime();
				}
			}
		}
	}

	public void setExpectedArrival(Id<CarrierService> serviceId, double expectedArrival) {
		trackers.get(serviceId).expectedArrival=expectedArrival;
	}

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
