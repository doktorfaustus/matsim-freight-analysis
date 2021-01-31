package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;

import java.util.HashMap;

public class FreightAnalysisShipmentTracking {
    HashMap<Id<CarrierShipment>, ShipmentTracker> shipments = new HashMap();
    public void addTracker(CarrierShipment shipment, Id<Person> driverId){
        shipments.put(shipment.getId(),new ShipmentTracker(shipment) );
    }

    public void trackEvent(ShipmentDeliveredEvent event) {
        shipments.get(event.getShipment().getId()).addEvent(event);
    }

    public void trackPickedUpEvent(ShipmentPickedUpEvent event) {
        if (!shipments.containsKey(event.getShipment().getId())){
            addTracker(event.getShipment(),event.getDriverId());
        }

    }

    public void trackDeliveryEvent(ShipmentDeliveredEvent event) {
    }

    public void addTracker(CarrierShipment shipment) {
    }
}
