package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
//import org.matsim.contrib.freight.events.ShipmentDeliveredEventHandler;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
//import org.matsim.contrib.freight.events.ShipmentPickedUpEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class FreightAnalysisEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler{

    private final Vehicles vehicles;
    private final Network network;
    private final Carriers carriers;
    HashMap<Id<Vehicle>, Double> vehiclesOnLink = new HashMap();
    FreightAnalysisVehicleTracking freightAnalysisVehicleTracking = new FreightAnalysisVehicleTracking();
    FreightAnalysisShipmentTracking shipmentTracking = new FreightAnalysisShipmentTracking();

    public FreightAnalysisEventHandler(Network network, Vehicles vehicles, Carriers carriers) {
        this.network=network;
        this.vehicles=vehicles;
        this.carriers=carriers;

        //for (Carrier carrier: carriers.getCarriers().values()){ // Für alle "echten" Frachtfahrzeuge wird ein Tracker angelegt.
        for(Vehicle vehicle:vehicles.getVehicles().values()){
                if (vehicle.getId().toString().contains("freight")){
                   freightAnalysisVehicleTracking.addTracker(vehicle.getId(), vehicle.getType() );//CarrierId hier noch nicht bekannt
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
                freightAnalysisVehicleTracking.addLeg(linkLeaveEvent.getVehicleId(), onLinkTime, linkLength, false);
            }
        }
    }



//    @Override
//    public void handleEvent(ShipmentDeliveredEvent event) {
//        if (!shipmentTracking.shipments.containsKey(event.getShipment().getId())){
//            shipmentTracking.addTracker(event.getShipment(), event.getDriverId() );
//        }
//        shipmentTracking.trackEvent(event);
//    }
//
//    @Override
//    public void handleEvent(ShipmentPickedUpEvent event) {
//
//    }
//
    public void export(){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("vehicleStatsExport.tsv"));
            out.write("VehicleType  CarrierId  travelTime  travelDistance  cost");
            out.newLine();
            HashMap<Id, VehicleTracker> trackers = freightAnalysisVehicleTracking.getTrackers();
            for(Id vehId : trackers.keySet()){ //das funktioniert so nicht mehr, wenn alle Tracker hier gebündelt sind.
                VehicleTracker tracker = trackers.get(vehId);
                out.write(vehId.toString() + "  " +tracker.typeIdString + "  " + tracker.carrierId + "    " + tracker.travelTime.toString() + "   " + tracker.travelDistance.toString() + "   " + tracker.cost.toString());
                out.newLine();
            }
            out.close();
            System.out.println("File created successfully");
        }
        catch (IOException e) {
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        freightAnalysisVehicleTracking.addDriver2Vehicle(event.getPersonId(),event.getVehicleId());
    }
}