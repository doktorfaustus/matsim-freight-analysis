package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

import java.io.File;

public class RunFreightAnalysis {
    public static void main(String[] args) {
        // benoetigte Dateien:
        RunFreightAnalysis rfa = new RunFreightAnalysis();
        rfa.runAnalysis();
    }

    public RunFreightAnalysis() {
    }

    private void runAnalysis(){//Dateipfade uebergeben lassen
       File networkFile = new File("output100/output_network.xml.gz");
       File carrierFile = new File("output100/output_carriers.xml");
       //File carrierFile = new File("output100/carrierReadtest.xml");
       File vehicleTypeFile = new File("output100/output_VehicleTypes.xml.gz");
       File vehiclesFile = new File("output100/output_allVehicles.xml.gz");
       File eventsFile = new File("output100/output_events.xml.gz");

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
       freightEventHandler.export();
    }
}
