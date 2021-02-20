package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;

import java.util.HashMap;
import java.util.HashSet;

public class FreightAnalysisShipmentTracking {

	private HashMap<Id<CarrierShipment>, ShipmentTracker> shipments = new HashMap();
    public void addTracker(CarrierShipment shipment){
        shipments.put(shipment.getId(),new ShipmentTracker(shipment) );
    }
	public HashMap<Id<CarrierShipment>, ShipmentTracker> getShipments() {
		return shipments;
	}

	// tracking Shipments based on Guesses the same way as Services are tracked, and also with the same issues.
	public void trackDeliveryActivity(ActivityStartEvent activityStartEvent) {
    	for (ShipmentTracker shipment: shipments.values()){
    		if (shipment.to==activityStartEvent.getLinkId() ){
				if(shipment.driverId == null){
					if(shipment.shipment.getDeliveryTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=shipment.shipment.getDeliveryTimeWindow().getEnd()){
						if (shipment.possibleDrivers.contains(activityStartEvent.getPersonId().toString())) {
							shipment.driverIdGuess = activityStartEvent.getPersonId();
							shipment.deliveryTimeGuess=activityStartEvent.getTime();
						}
					}
				} else if (shipment.driverId.toString().equals(activityStartEvent.getPersonId().toString())){
					shipment.deliveryTime=activityStartEvent.getTime();
				}
			}
		}
	}

	// for improving the guess, we track the pickup activities aswell to narrow down the selection of drivers on those that could have picked up the shipment when we later on try to match the delivery activity.
	public void trackPickupActivity(ActivityStartEvent activityStartEvent) {
    	for (ShipmentTracker shipmentTracker: shipments.values()){
    		if (shipmentTracker.from==activityStartEvent.getLinkId()){
    			if (shipmentTracker.driverId==null){
    				if(shipmentTracker.shipment.getPickupTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=shipmentTracker.shipment.getPickupTimeWindow().getEnd()){
    					shipmentTracker.possibleDrivers.add(activityStartEvent.getPersonId().toString());
					}
				}
			}
		}
	}
// untested LSP Event handling for precise Shipment Tracking
	public void trackPickedUpEvent(ShipmentPickedUpEvent event) {
		if (shipments.containsKey(event.getShipment().getId())) {
			CarrierShipment shipment = event.getShipment();
			shipments.get(shipment.getId()).pickUpTime = event.getTime();
			shipments.get(shipment.getId()).driverId = event.getDriverId();
		}
	}


	public void trackDeliveryEvent(ShipmentDeliveredEvent event) {
		if (shipments.containsKey(event.getShipment().getId())){
			ShipmentTracker shipmentTracker = shipments.get(event.getShipment().getId());
			shipmentTracker.deliveryTime=event.getTime();
			shipmentTracker.deliveryDuration +=  (event.getTime() - shipmentTracker.pickUpTime);
		}
	}
}
class ShipmentTracker {
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
