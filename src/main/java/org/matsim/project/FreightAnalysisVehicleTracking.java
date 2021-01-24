package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;

public class FreightAnalysisVehicleTracking {
	private HashMap<Id<Vehicle>, VehicleTracker> trackers = new HashMap<>();

	public Id<Vehicle> getDriver2VehicleId(Id<Person> driverId) {
		return driver2VehicleId.get(driverId);
	}

	private HashMap<Id<Person>, Id<Vehicle>> driver2VehicleId = new HashMap<>();

	public void addTracker(Vehicle vehicle) {
		trackers.putIfAbsent(vehicle.getId(), new VehicleTracker(vehicle));
	}

	public void addLeg(Id<Vehicle> vehId, Double travelTime, Double travelDistance, Boolean isEmpty) {
		if (!trackers.containsKey(vehId)) {
			return;
		}
		//hier könnte man noch was warnen

		VehicleTracker tracker = trackers.get(vehId);
		tracker.cost =
				tracker.cost
						+ tracker.travelDistance * tracker.vehicleType.getCostInformation().getCostsPerMeter()
						+ tracker.travelTime * tracker.vehicleType.getCostInformation().getCostsPerSecond();
		if (isEmpty) {
			tracker.emptyDistance = tracker.emptyDistance + travelDistance;
			tracker.emptyTime = tracker.emptyTime + (-travelTime);
		} else {
			tracker.travelDistance = tracker.travelDistance + travelDistance;
			tracker.travelTime = tracker.travelTime + (-travelTime);
		}
	}

	public HashMap<Id<Vehicle>, VehicleTracker> getTrackers() {
		return trackers;
	}

	public void addDriver2Vehicle(Id<Person> personId, Id<Vehicle> vehicleId, double time) {
		if (trackers.containsKey(vehicleId)) {
			if (trackers.get(vehicleId).currentDriverId != personId) { // In Case the Person wasn't using the vehicle before, a new tour of the vehicle is started.
				if (null != trackers.get(vehicleId).currentDriverId) {
					//TODO: Warning that driver changed unexpectedly as this wrongs the service duration because the end of the previous service is obviously not known.
				}
				trackers.get(vehicleId).currentDriverId = personId;
				trackers.get(vehicleId).serviceStartTime = time;
				driver2VehicleId.put(personId, vehicleId);
			}
		}
	}

	public void addCarrier2Vehicle(Id<Vehicle> vehicleId, Id<Carrier> carrierId) {
		if (trackers.containsKey(vehicleId)) {
			trackers.get(vehicleId).carrierId = carrierId;
		}
	}

	public void addServiceEnd(Id<Person> personId) {
		if (driver2VehicleId.containsKey(personId)){
			VehicleTracker tracker = trackers.get(driver2VehicleId.get(personId));
			tracker.serviceTime += tracker.lastExit- tracker.serviceStartTime;
			tracker.currentDriverId =null;
			tracker.lastDriverId=personId;
			tracker.driverHistory.add(personId);
		}
	}

	public void registerVehicleLeave(PersonLeavesVehicleEvent event) {
		if (trackers.containsKey(event.getVehicleId())){
			trackers.get(event.getVehicleId()).lastExit=event.getTime();
		}
	}
}
