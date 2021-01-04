package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class VehicleTracker {
    VehicleType vehicleType;
    Double travelTime=0.0;
    Double travelDistance=0.0;
    Double emptyTime=0.0;
    Double emptyDistance=0.0;

   public VehicleTracker(VehicleType vehicleType){
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
       return vehicleType.getId().toString() + "  " + travelTime.toString() + "   " + travelDistance.toString() + "   " + emptyTimeShare.toString() + "   " + emptyDistanceShare.toString();
   }
}
