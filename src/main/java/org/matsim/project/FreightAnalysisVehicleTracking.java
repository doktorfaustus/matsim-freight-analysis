package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import java.util.HashMap;

public class FreightAnalysisVehicleTracking {
    HashMap<Id<Vehicle>, VehicleTracker> trackers = new HashMap<>();

   public void addTracker(Id<Vehicle> vehicleId, VehicleType vehicleType, Id<Carrier> carrierId){
       trackers.putIfAbsent(vehicleId,new VehicleTracker(vehicleType, carrierId,vehicleType.getCostInformation().getFixedCosts()));
   }

   public void addLeg(Id<Vehicle> vehId, Double travelTime,Double travelDistance,Boolean isEmpty){
       if (!trackers.containsKey(vehId)){return;};//hier könnte man noch was warnen

       VehicleTracker tracker = trackers.get(vehId);
       tracker.cost =
               tracker.cost
               + tracker.travelDistance * tracker.vehicleType.getCostInformation().getCostsPerMeter()
               + tracker.travelTime * tracker.vehicleType.getCostInformation().getCostsPerSecond();
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
