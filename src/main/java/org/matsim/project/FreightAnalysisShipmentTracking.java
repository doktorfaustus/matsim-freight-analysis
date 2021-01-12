package org.matsim.project;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;

import java.util.HashMap;

public class FreightAnalysisShipmentTracking {
    HashMap<Id<CarrierShipment>, CarrierShipmentTracker> shipments = new HashMap();
    public void addTracker(CarrierShipment shipment){
        shipments.put(shipment.getId(),new CarrierShipmentTracker(shipment) );
    }
    public void addTracker(CarrierShipment shipment, Id<Carrier> carrierId){
       shipments.put(shipment.getId(),new CarrierShipmentTracker(shipment,carrierId)); 
    }
    public void addTracker(CarrierShipment shipment, Id<Carrier> carrierId, Id<Person> driverId) {
        shipments.put(shipment.getId(), new CarrierShipmentTracker(shipment,carrierId,driverId));
    }

    public void trackEvent(ShipmentDeliveredEvent event) {
        shipments.get(event.getShipment().getId()).addEvent(event);
    }
}
