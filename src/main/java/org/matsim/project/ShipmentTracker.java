package org.matsim.project;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.utils.misc.Time;

public class ShipmentTracker {
    public Id<Shipment> id;
    public Id<Carrier> carrierAgent;
    public Time pickupTime;
    public Time deliveryTime;
    public Boolean isOnTimePickUp;
    public Boolean isOnTimeDelivery;

    public ShipmentTracker(Id<Shipment> shipmentId, Id<Carrier> carrierId){
        this.id=shipmentId;
        this.carrierAgent=carrierId;
    }

}
