package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.core.utils.misc.Time;

import java.sql.Driver;
import java.util.HashSet;
import java.util.LinkedList;

public class ShipmentTracker {
    private Id<Person> driverId = null;
    private CarrierShipment shipment;
    private Id<CarrierShipment> id;
    private Id<Carrier> carrierId;
    private Time pickupTime;
    private Time deliveryTime;
    private Boolean isOnTimePickUp;
    private Boolean isOnTimeDelivery;
    LinkedList<Event> events = new LinkedList<>();
    HashSet<Id<Driver>> driverHistory = new HashSet<>();

    public ShipmentTracker(CarrierShipment shipment) {
        this.id = shipment.getId();
    }

    public ShipmentTracker(CarrierShipment shipment, Id<Carrier> carrierId) {
        this(shipment);
        this.carrierId = carrierId;
    }

    public ShipmentTracker(CarrierShipment shipment, Id<Carrier> carrierId, Id<Person> driverId) {
        this(shipment, carrierId);
        this.driverId = driverId;

    }
}
