package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;

import java.util.HashSet;

public class ShipmentTracker {
    public Id<Person> driverIdGuess;
    public double deliveryTimeGuess;
    public HashSet<String> possibleDrivers = new HashSet<String>();
    Id<Link> from;
    Id<Link> to;
    public Double pickUpTime = 0.;
    public Double deliveryDuration = 0.;
    public Double deliveryTime = 0.;
    public Id<Person> driverId;
    public CarrierShipment shipment;
    public Id<CarrierShipment> id;
    public Id<Carrier> carrierId;

    public ShipmentTracker(CarrierShipment shipment) {
        this.id = shipment.getId();
        this.from = shipment.getFrom();
        this.to=shipment.getTo();
        this.shipment=shipment;
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
