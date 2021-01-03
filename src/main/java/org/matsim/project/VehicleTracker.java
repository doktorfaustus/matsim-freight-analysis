package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

public class VehicleTracker {
    Id<Vehicle> vehicleId;
    String vehicleType;
    Double travelTime;
    Double travelDistance;
    Double emptyTime;
    Double emptyDistance;

   public VehicleTracker(Id<Vehicle> id, String vehicleType){
       this.vehicleId = id;
       this.vehicleType = vehicleType;
   }

   public void addLeg(Double travelTime,Double travelDistance,Boolean isEmpty){
       if (isEmpty){
           this.emptyDistance = this.emptyDistance + travelDistance;
           this.emptyTime = this.emptyTime + travelTime;
       } else {
           this.travelDistance = this.travelDistance + travelDistance;
           this.travelTime = this.travelTime + travelTime;
       }
   }

   @Override
   public String toString(){
       Double emptyTimeShare = (emptyTime / travelTime);
       Double emptyDistanceShare = (emptyDistance / travelDistance);
       return vehicleId.toString() + "  " + vehicleType.toString() + "  " + travelTime.toString() + "   " + travelDistance.toString() + "   " + emptyTimeShare.toString() + "   " + emptyDistanceShare.toString();
   }
}
