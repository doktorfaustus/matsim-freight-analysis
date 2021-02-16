package org.matsim.project;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.*;

import java.io.File;

public class RunFreightAnalysis {
    public static void main(String[] args) {
        // benoetigte Dateien:
        RunFreightAnalysis rfa = new RunFreightAnalysis();
        String basePath = "output100";
//        String basePath = "/Users/jakob/debianserv/data/Uni/Master/2020_WS/MATSim_Advanced/matsim-freight/Input_KMT/21_ICEVBEV_NwCE_BVWP_10000it_DC_noTax";
        rfa.runAnalysis(basePath);
    }

    public RunFreightAnalysis() {
    }

    private void runAnalysis(String basePath){//Dateipfade uebergeben lassen
       File networkFile = new File(basePath + "/output_network.xml.gz");
       File carrierFile = new File(basePath + "/output_carriers.xml");
       //File carrierFile = new File(basePath + "/carrierReadtest.xml");
       File vehicleTypeFile = new File(basePath + "/output_VehicleTypes.xml.gz");
       File vehiclesFile = new File(basePath + "/output_allVehicles.xml.gz");
       File eventsFile = new File(basePath + "/output_events.xml.gz");

       Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

       Carriers carriers = new Carriers();
       new CarrierPlanXmlReader(carriers).readFile(carrierFile.getAbsolutePath());

       Vehicles vehicles = new VehicleUtils().createVehiclesContainer();
       new  MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

       EventsManager eventsManager = EventsUtils.createEventsManager();
       FreightAnalysisEventHandler freightEventHandler = new FreightAnalysisEventHandler(network, vehicles,  carriers);
       eventsManager.addHandler(freightEventHandler);

       eventsManager.initProcessing();
       MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
       eventsReader.readFile(eventsFile.getAbsolutePath());
       eventsManager.finishProcessing();
       freightEventHandler.exportVehicleInfo("freightOutput");
       freightEventHandler.exportVehicleTripInfo("freightOutput");
       freightEventHandler.exportCarrierInfo("freightOutput");
       freightEventHandler.exportServiceInfo("freightOutput");
       freightEventHandler.exportShipmentInfo("freightOutput");
    }
}
