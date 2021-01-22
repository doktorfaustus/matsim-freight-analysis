package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;

import java.util.HashMap;

public class FreightAnalysisServiceTracking {
	HashMap<Id<CarrierService>, ServiceTracker> trackers = new HashMap<>();
	public void addTracker(CarrierService service) {
	trackers.put(service.getId(), new ServiceTracker(service));
	}
}
