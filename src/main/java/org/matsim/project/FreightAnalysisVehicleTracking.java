package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashMap;

public class FreightAnalysisVehicleTracking {
    HashMap<Id<Vehicle>, VehicleTracker> trackers = new HashMap<>();
    HashMap<Id<Person>, Id<Vehicle>> driver2VehicleId = new HashMap<>();

    public void addTracker(Id<Vehicle> vehicleId, VehicleType vehicleType) {
        trackers.putIfAbsent(vehicleId, new VehicleTracker(vehicleType));
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
            tracker.emptyTime = tracker.emptyTime + travelTime;
        } else {
            tracker.travelDistance = tracker.travelDistance + travelDistance;
            tracker.travelTime = tracker.travelTime + travelTime;
        }
    }

    public HashMap<Id<Vehicle>, VehicleTracker> getTrackers() {
        return trackers;
    }

    public void addDriver2Vehicle(Id<Person> personId, Id<Vehicle> vehicleId) {
        if (trackers.containsKey(vehicleId)) {
            trackers.get(vehicleId).driverId = personId;
            driver2VehicleId.put(personId,vehicleId);
        }
    }

    public void addCarrier2Vehicle(Id<Vehicle> vehicleId, Id<Carrier> carrierId) {
        if (trackers.containsKey(vehicleId)){
            trackers.get(vehicleId).carrierId = carrierId;
        }
    }
}
