package org.matsim.project;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.core.utils.misc.Time;

import java.util.LinkedList;

public class CarrierShipmentTracker {
    private Id<Person> driverId = null;
    private CarrierShipment shipment;
    private Id<CarrierShipment> id;
    private Id<Carrier> carrierId;
    private Time pickupTime;
    private Time deliveryTime;
    private Boolean isOnTimePickUp;
    private Boolean isOnTimeDelivery;
    LinkedList<Event> events = new LinkedList<>();

    public CarrierShipmentTracker(CarrierShipment shipment){
        this.id=shipment.getId();
    }
    public CarrierShipmentTracker(CarrierShipment shipment, Id<Carrier> carrierId){
        this(shipment);
        this.carrierId=carrierId;
    }
    public CarrierShipmentTracker(CarrierShipment shipment, Id<Carrier> carrierId, Id<Person> driverId){
       this(shipment, carrierId);
       this.driverId = driverId;

    }
    public void addEvent(Event event){
        events.add(event);
    }
    public LinkedList getEvents(){
        return events;
    }
}
