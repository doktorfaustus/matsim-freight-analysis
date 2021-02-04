package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;

import java.util.HashMap;

public class FreightAnalysisShipmentTracking {

	private HashMap<Id<CarrierShipment>, ShipmentTracker> shipments = new HashMap();
    public void addTracker(CarrierShipment shipment){
        shipments.put(shipment.getId(),new ShipmentTracker(shipment) );
    }

    public void trackPickedUpEvent(ShipmentPickedUpEvent event) {
        CarrierShipment shipment = event.getShipment();
        if (!shipments.containsKey(shipment.getId())){
            addTracker(shipment);
        }
        shipments.get(shipment.getId()).pickUpTime=event.getTime();
        shipments.get(shipment.getId()).driverId=event.getDriverId();
    }


	public void trackDeliveryEvent(ShipmentDeliveredEvent event) {
    	if (shipments.containsKey(event.getShipment().getId())){
			ShipmentTracker shipmentTracker = shipments.get(event.getShipment().getId());
    	    shipmentTracker.deliveryTime=event.getTime();
			shipmentTracker.deliveryDuration += shipmentTracker.deliveryDuration + (event.getTime() - shipmentTracker.pickUpTime);
        }
    }
	public HashMap<Id<CarrierShipment>, ShipmentTracker> getShipments() {
		return shipments;
	}

	public void trackDeliveryActivity(ActivityStartEvent activityStartEvent) {
    	for (ShipmentTracker shipment: shipments.values()){
    		if (shipment.to==activityStartEvent.getLinkId() ){
				if(shipment.driverId.toString().equals("-1")){
					if(shipment.shipment.getDeliveryTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=shipment.shipment.getDeliveryTimeWindow().getEnd()){
						if (shipment.possibleDrivers.contains(activityStartEvent.getPersonId().toString())) {
							shipment.driverIdGuess = activityStartEvent.getPersonId();
						}
					}
					shipment.deliveryTimeGuess=activityStartEvent.getTime();
				} else if (shipment.driverId.toString().equals(activityStartEvent.getPersonId().toString())){
					shipment.deliveryTime=activityStartEvent.getTime();
				}
			}
		}
	}

	public void trackPickupActivity(ActivityStartEvent activityStartEvent) {
    	for (ShipmentTracker shipmentTracker: shipments.values()){
    		if (shipmentTracker.from==activityStartEvent.getLinkId()){
    			if (shipmentTracker.driverId.toString().equals("-1")){
    				if(shipmentTracker.shipment.getPickupTimeWindow().getStart()<=activityStartEvent.getTime() && activityStartEvent.getTime()<=shipmentTracker.shipment.getPickupTimeWindow().getEnd()){
    					shipmentTracker.possibleDrivers.add(activityStartEvent.getPersonId().toString());
					}
				}
			}
		}
	}
}
