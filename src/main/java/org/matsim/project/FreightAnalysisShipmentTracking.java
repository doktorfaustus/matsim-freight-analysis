package org.matsim.project;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Id;

import java.util.HashMap;

public class FreightAnalysisShipmentTracking {
    HashMap<Id<Shipment>, ShipmentTracker> shipments = new HashMap();

}
