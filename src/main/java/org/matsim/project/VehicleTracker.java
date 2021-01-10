package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;

public class VehicleTracker {
    String typeIdString = "N/A";
    Double travelTime = 0.0;
    Double travelDistance = 0.0;
    Double emptyTime = 0.0;
    Double emptyDistance = 0.0;
    String carrierId = "N/A";
    VehicleTracker(String type) {
        this.typeIdString = type;
    }

    public VehicleTracker(String vehicleTypeIdString, Id<Carrier> carrierId) {
        this.typeIdString = vehicleTypeIdString;
        this.carrierId = carrierId.toString();
    }
 }
