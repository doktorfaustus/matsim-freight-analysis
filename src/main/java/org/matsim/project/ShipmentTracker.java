package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.utils.misc.Time;

public class ShipmentTracker {
    private Id id;
    private Id<Carrier> carrierAgent;
    private Time pickupTime;
    private Time deliveryTime;
    private Boolean isOnTimePickUp;
    private Boolean isOnTimeDelivery;

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public Id<Carrier> getCarrierAgent() {
        return carrierAgent;
    }

    public void setCarrierAgent(Id<Carrier> carrierAgent) {
        this.carrierAgent = carrierAgent;
    }

    public Time getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(Time pickupTime) {
        this.pickupTime = pickupTime;
    }

    public Time getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(Time deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public Boolean getOnTimePickUp() {
        return isOnTimePickUp;
    }

    public void setOnTimePickUp(Boolean onTimePickUp) {
        isOnTimePickUp = onTimePickUp;
    }

    public Boolean getOnTimeDelivery() {
        return isOnTimeDelivery;
    }

    public void setOnTimeDelivery(Boolean onTimeDelivery) {
        isOnTimeDelivery = onTimeDelivery;
    }

}
