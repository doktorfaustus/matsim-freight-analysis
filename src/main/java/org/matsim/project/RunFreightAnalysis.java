package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.File;

public class RunFreightAnalysis {
    public static void main(String[] args) {
        // benoetigte Dateien:
        RunFreightAnalysis rfa = new RunFreightAnalysis();
        rfa.runAnalysis();
    }

   private void runAnalysis(){//Dateipfade uebergeben lassen
       File networkFile = new File("output100/output_network.xml.gz");
       File carrierFile = new File("output100/output_carriers.xml.gz");
       File vehicleTypeFile = new File("output100/output_VehicleTypes.xml.gz");
       File vehiclesFile = new File("output100/output_allVehicles.xml.gz");
       File eventsFile = new File("output100/output_events.xml.gz");

       Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

       //CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
       //new CarrierVehicleTypeReader(vehicleTypes).readFile(carrierFile.getAbsolutePath());
       // Vehicles vehicles = new Ve;
       // MatsimVehicleReader.VehicleReader vehicleReader = new MatsimVehicleReader.VehicleReader(vehicles);
       vehicleReader.readFile(vehiclesFile.getAbsolutePath());

       EventsManager eventsManager = EventsUtils.createEventsManager();
       EventHandler freightEventHandler = new FreightAnalysisEventHandler(network, vehicles);
       eventsManager.addHandler(freightEventHandler);

       eventsManager.initProcessing();
       MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
       eventsReader.readFile(eventsFile.getAbsolutePath());
       eventsManager.finishProcessing();
    }


}
