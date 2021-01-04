package org.matsim.project;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentDeliveredEventHandler;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class FreightAnalysisEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler {

    private final Vehicles vehicles;
    private final Network network;
    HashMap<Id<Vehicle>, Double> vehiclesOnLink = new HashMap();
    HashMap<Id<Vehicle>, VehicleTracker> vehicleTrackers = new HashMap();
    HashMap<Id<Shipment>, ShipmentTracker> shipmentTrackers = new HashMap();

    public FreightAnalysisEventHandler(Network network, Vehicles vehicles) {
        this.network=network;
        this.vehicles=vehicles;
        for (Vehicle vehicle: this.vehicles.getVehicles().values()){
            if  (vehicle.getType().getCapacity().getOther() > 0){
                vehicleTrackers.put(vehicle.getId(),new VehicleTracker(vehicle.getType()));
            }
        }
    }

    public void handleEvent(IterationEndsEvent e){
        System.out.println("GuNa!");
    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {

    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        vehiclesOnLink.put(linkEnterEvent.getVehicleId(),linkEnterEvent.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        if (vehiclesOnLink.containsKey(linkLeaveEvent.getVehicleId())) {
            Double onLinkTime = vehiclesOnLink.get(linkLeaveEvent.getVehicleId()) - linkLeaveEvent.getTime();
            Double linkLength = network.getLinks().get(linkLeaveEvent.getLinkId()).getLength();

            if (vehicles.getVehicles().get(linkLeaveEvent.getVehicleId()).getType().getCapacity().getOther() > 0) {
                vehicleTrackers.get(linkLeaveEvent.getVehicleId()).addLeg(onLinkTime, linkLength, false);
            }
        }
    }

    @Override
    public void handleEvent(ShipmentDeliveredEvent event) {

    }

    @Override
    public void handleEvent(ShipmentPickedUpEvent event) {

    }

    public void export(){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("vehicleStatsExport.tsv"));
            out.write("VehicleType  travelTime  travelDistance  emptyTimeShare   emptyDistanceShare");
            out.newLine();
            for(VehicleTracker vt : vehicleTrackers.values()){
                out.write(vt.toString());
                out.newLine();
            }
            out.close();
            System.out.println("File created successfully");
        }
        catch (IOException e) {
        }
    }
}
