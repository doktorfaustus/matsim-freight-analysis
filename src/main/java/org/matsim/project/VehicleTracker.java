package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashSet;

public class VehicleTracker {
	public double lastExit;
	public Id<Person> lastDriverId = Id.createPersonId(-1);
	public HashSet<Id<Person>> driverHistory=new HashSet<>();
	public HashSet<VehicleTrip> tripHistory= new HashSet<>();
	public double currentTripDuration;
	public double currentTripDistance;
	VehicleType vehicleType;
	Double travelTime = 0.0;
	Double serviceTime = 0.0;
	Double travelDistance = 0.0;
	Double emptyTime = 0.0;
	Double emptyDistance = 0.0;
	Id<Carrier> carrierId = Id.create(-1,Carrier.class);
	Double cost = 0.0;
	Id<Person> currentDriverId = Id.createPersonId(-1);
	Double usageStartStime = 0.0;

	public VehicleTracker(Vehicle vehicle) {
		this.vehicleType = vehicle.getType();
	}

	static class VehicleTrip {
		Id<Person> driverId = Id.createPersonId(-1);
		Double travelTime = 0.0;
		Double travelDistance = 0.0;
		Id<Carrier> carrierId = Id.create(-1, Carrier.class);
		Double cost=0.0;

		public VehicleTrip(Id<Person> currentDriverId, double currentTripDistance, double currentTripDuration, double cost) {
			this.driverId = currentDriverId;
			this.travelDistance = currentTripDistance;
			this.travelTime = currentTripDuration;
			this.cost = cost;
		}
	}
}
