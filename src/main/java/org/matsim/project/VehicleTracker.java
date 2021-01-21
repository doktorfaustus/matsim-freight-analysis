package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.VehicleType;

public class VehicleTracker {
    String typeIdString = "N/A";
    VehicleType vehicleType = null;
    Double travelTime = 0.0;
    Double travelDistance = 0.0;
    Double emptyTime = 0.0;
    Double emptyDistance = 0.0;
    String carrierId = "N/A";
    Double cost = 0.0;
    VehicleTracker(String type) {
        this.typeIdString = type;
    }

    public VehicleTracker(VehicleType vehicleType, Id<Carrier> carrierId) {
        this.vehicleType = vehicleType;
        this.carrierId = carrierId.toString();
    }
    public VehicleTracker(VehicleType vehicleType, Id<Carrier> carrierId, Double baseCost) {
        this.vehicleType = vehicleType;
        this.carrierId = carrierId.toString();
        this.cost = baseCost;
    }
 }
