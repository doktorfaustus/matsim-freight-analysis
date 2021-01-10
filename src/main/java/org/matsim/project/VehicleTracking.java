package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;

public class VehicleTracking{
    HashMap<Id<Vehicle>, VehicleTracker> trackers = new HashMap<>();

   public void addTracker(Vehicle vehicle){
       trackers.putIfAbsent(vehicle.getId(), new VehicleTracker(vehicle.getType().getId().toString()));
   }
   public void addTracker(Id<Vehicle> vehicleId, String vehicleTypeIdString, Id<Carrier> carrierId){
       trackers.putIfAbsent(vehicleId,new VehicleTracker(vehicleTypeIdString, carrierId));
   }

   public void addLeg(Id<Vehicle> vehId, Double travelTime,Double travelDistance,Boolean isEmpty){
       if (!trackers.containsKey(vehId)){return;};//hier könnte man noch was warnen

       VehicleTracker tracker = trackers.get(vehId);
       if (isEmpty){
           tracker.emptyDistance = tracker.emptyDistance + travelDistance;
           tracker.emptyTime = tracker.emptyTime + travelTime;
       } else {
           tracker.travelDistance = tracker.travelDistance + travelDistance;
           tracker.travelTime = tracker.travelTime + travelTime;
       }
   }

   public HashMap getTrackers(){
       return trackers;
   }
}
