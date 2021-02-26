package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashMap;
import java.util.HashSet;

public class FreightAnalysisVehicleTracking {


	private HashMap<Id<Vehicle>, VehicleTracker> trackers = new HashMap<>();
	private HashMap<Id<Person>, Id<Vehicle>> driver2VehicleId = new HashMap<>();

	public Id<Vehicle> getDriver2VehicleId(Id<Person> driverId) {
		return driver2VehicleId.get(driverId);
	}

	// start tracking of a vehicle
	public void addTracker(Vehicle vehicle) {
		trackers.putIfAbsent(vehicle.getId(), new VehicleTracker(vehicle));
	}

	// register a leg vor a vehicle providing travel time and travelDistance
	public void addLeg(Id<Vehicle> vehId, Double travelTime, Double travelDistance, Boolean isEmpty) {
		if (!trackers.containsKey(vehId)) {
			return;
		}

		VehicleTracker tracker = trackers.get(vehId);
		// calculate cost for each leg individually
		tracker.cost =
				tracker.cost + calculateCost(tracker.vehicleType, travelDistance, travelTime);
		tracker.currentTripDistance += travelDistance;
		tracker.currentTripDuration += travelTime;

		if (isEmpty) {
			tracker.emptyDistance = tracker.emptyDistance + travelDistance;
			tracker.emptyTime = tracker.emptyTime + (-travelTime);
		} else {
			tracker.travelDistance = tracker.travelDistance + travelDistance;
			tracker.roadTime = tracker.roadTime + (-travelTime);
		}
	}

	public HashMap<Id<Vehicle>, VehicleTracker> getTrackers() {
		return trackers;
	}

	// register when a driver can be matched to a vehicle
	public void addDriver2Vehicle(Id<Person> personId, Id<Vehicle> vehicleId, double time) {
		if (trackers.containsKey(vehicleId)) {
			if (trackers.get(vehicleId).currentDriverId != personId) { // In Case the Person wasn't using the vehicle before, a new tour of the vehicle is started.
				if (null != trackers.get(vehicleId).currentDriverId) {
					//TODO: Warning that driver changed unexpectedly as this wrongs the service duration because the end of the previous service is obviously not known.
				}
				trackers.get(vehicleId).currentDriverId = personId;
				trackers.get(vehicleId).usageStartTime = time;
				driver2VehicleId.put(personId, vehicleId);
			}
		}
	}

	// register when a carrier can be matched to a vehicle
	public void addCarrier2Vehicle(Id<Vehicle> vehicleId, Id<Carrier> carrierId) {
		if (trackers.containsKey(vehicleId)) {
			trackers.get(vehicleId).carrierId = carrierId;
		}
	}

	// register a guess for a carrier of a vehicle.
	public void addCarrierGuess(Id<Vehicle> id, Id<Carrier> carrierGuess) {
		if (trackers.containsKey(id)) {
			trackers.get(id).carrierIdGuess = carrierGuess;
		}
	}

	// when a person that could've been a driver throws a "end" Event, it is assumed that they have stopped using the vehicle. The service time is updated, the currentDriver reset.
	public void endVehicleUsage(Id<Person> personId) {
		if (driver2VehicleId.containsKey(personId)){
			VehicleTracker tracker = trackers.get(driver2VehicleId.get(personId));
			tracker.usageTime += tracker.lastExit- tracker.usageStartTime;
			tracker.currentDriverId =null;
			tracker.lastDriverId=personId;
			tracker.driverHistory.add(personId);
		}
	}


	// when a person leaves a vehicle, a trip/leg (depending on view) has ended, so it is registered with length, duration and cost, also counters are reset.
	public void registerVehicleLeave(PersonLeavesVehicleEvent event) {
		if (trackers.containsKey(event.getVehicleId())){
			VehicleTracker tracker = trackers.get(event.getVehicleId());
			tracker.lastExit=event.getTime();
			tracker.tripHistory.add(new VehicleTracker.VehicleTrip(tracker.currentDriverId,tracker.currentTripDistance,tracker.currentTripDuration, calculateCost(tracker.vehicleType,tracker.currentTripDistance,tracker.currentTripDuration)));
			tracker.currentTripDistance=0.0;
			tracker.currentTripDuration=0.0;
		}
	}

	// helper function to calculate cost values based on the vehicle type cost info
	private double calculateCost(VehicleType vehicleType, Double distance, Double time){
		return(distance * vehicleType.getCostInformation().getCostsPerMeter()
				+  time * vehicleType.getCostInformation().getCostsPerSecond());
	}
}
class VehicleTracker {
	public double lastExit;
	public Id<Person> lastDriverId = Id.createPersonId(-1);
	public HashSet<Id<Person>> driverHistory=new HashSet<>();
	public HashSet<VehicleTrip> tripHistory= new HashSet<>();
	public double currentTripDuration;
	public double currentTripDistance;
	public Id<Carrier> carrierIdGuess;
	VehicleType vehicleType;
	Double roadTime = 0.0;
	Double usageTime = 0.0;
	Double travelDistance = 0.0;
	Double emptyTime = 0.0;
	Double emptyDistance = 0.0;
	Id<Carrier> carrierId;
	Double cost = 0.0;
	Id<Person> currentDriverId;
	Double usageStartTime = 0.0;

	public VehicleTracker(Vehicle vehicle) {
		this.vehicleType = vehicle.getType();
		// instantiate with fixed daily costs of vehicle:
		this.cost=vehicleType.getCostInformation().getFixedCosts();
	}

	// sub.class for storing  the info about single vehicle trips
	static class VehicleTrip {
		Id<Person> driverId ;
		Double travelTime;
		Double travelDistance;
		Double cost;

		public VehicleTrip(Id<Person> currentDriverId, double currentTripDistance, double currentTripDuration, double cost) {
			this.driverId = currentDriverId;
			this.travelDistance = currentTripDistance;
			this.travelTime = currentTripDuration;
			this.cost = cost;
		}
	}
}
