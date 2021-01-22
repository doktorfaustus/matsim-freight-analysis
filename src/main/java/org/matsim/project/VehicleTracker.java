package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.VehicleType;

public class VehicleTracker {
	String typeIdString = "N/A";
	VehicleType vehicleType;
	Double travelTime = 0.0;
	Double travelDistance = 0.0;
	Double emptyTime = 0.0;
	Double emptyDistance = 0.0;
	Id<Carrier> carrierId;
	Double cost = 0.0;
	Id<Person> driverId = null;

	VehicleTracker(String type) {
		this.typeIdString = type;
	}

	public VehicleTracker(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public String toTSV() {
		return (this.vehicleType.getId().toString() + "	" + this.carrierId.toString() + "	" + this.driverId.toString() + "	" + this.travelTime.toString() + "	" + this.travelDistance.toString() + "	" + this.cost.toString());
	}
}
