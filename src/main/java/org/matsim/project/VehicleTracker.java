package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashSet;

public class VehicleTracker {
	public double lastExit;
	public Id<Person> lastDriverId;
	public HashSet<Id<Person>> driverHistory=new HashSet<>();
	VehicleType vehicleType;
	Double travelTime = 0.0;
	Double serviceTime = 0.0;
	Double travelDistance = 0.0;
	Double emptyTime = 0.0;
	Double emptyDistance = 0.0;
	Id<Carrier> carrierId;
	Double cost = 0.0;
	Id<Person> currentDriverId = null;
	Double serviceStartTime = 0.0;

	public VehicleTracker(Vehicle vehicle) {
		this.vehicleType = vehicle.getType();
	}

	public String toTSV() {
		return (this.vehicleType.getId().toString() + "	" + this.carrierId.toString() + "	" + this.lastDriverId.toString() + "	" + this.serviceTime.toString() + "	" + this.travelTime.toString() + "	" + this.travelDistance.toString() + "	" + this.cost.toString());
	}
}
