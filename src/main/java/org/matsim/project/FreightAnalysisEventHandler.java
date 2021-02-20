package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
//import org.matsim.contrib.freight.events.ShipmentDeliveredEventHandler;
//import org.matsim.contrib.freight.events.ShipmentPickedUpEventHandler;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPServiceStartEvent;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceStartEventHandler;
import org.matsim.contrib.freight.events.eventhandler.ShipmentDeliveredEventHandler;
import org.matsim.contrib.freight.events.eventhandler.ShipmentPickedUpEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

/*
* EventHandler for analysis of matsim-freight runs. Tracks freight vehicles, carriers, shipments and services and is able to export results to TSV files.
* Only uses information that is certain by default. Without LSP Events this means that the connection between Carrier-related Objects (Carriers, Shipments, Services) often cannot be made, but this Handles tries to make an educated guess which you can optionally include in the export. Guessed info will be preceeded by "?" in export.
* */

public class FreightAnalysisEventHandler implements  ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler, LSPServiceStartEventHandler, LSPServiceEndEventHandler {
	private final static Logger log = Logger.getLogger(FreightAnalysisEventHandler.class);
	private Vehicles vehicles;
	private Network network;
	private Carriers carriers;
	HashMap<Id<Vehicle>, Double> vehiclesOnLink = new HashMap<>();
	FreightAnalysisVehicleTracking vehicleTracking = new FreightAnalysisVehicleTracking();
	FreightAnalysisShipmentTracking shipmentTracking = new FreightAnalysisShipmentTracking();
	FreightAnalysisServiceTracking serviceTracking = new FreightAnalysisServiceTracking();

	public FreightAnalysisEventHandler(Network network, Vehicles vehicles, Carriers carriers) {
		this.network = network;
		this.vehicles = vehicles;
		this.carriers = carriers;

		// the EventHandler tracks all vehicles containing "freight" by default which is as of now (02/21) the easiest way to do so, but not a pretty one.
		// You can add trackers by yourself at will.
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			String vehicleIdString = vehicle.getId().toString();
			if (vehicle.getId().toString().contains("freight")) {
				vehicleTracking.addTracker(vehicle);

				// if the vehicleId contains its own vehicleType, it is likely that the naming scheme
				// freight_$CARRIER_veh_$VEHICLETYPE_[...]
				// is used, so we try to extract the CarrierId based on this scheme and see if it is indeed a carrier.
				// If that is the case, it is used as a guess for the Carrier.
				if(vehicle.getId().toString().contains(vehicle.getType().getId().toString())){
					String carrierGuess = vehicleIdString.replaceAll("_veh.*","");
					carrierGuess = carrierGuess.replaceAll("freight_", "");
					for (Carrier carrier: carriers.getCarriers().values()){
						if (carrier.getId().toString().equals(carrierGuess)){
							vehicleTracking.addCarrierGuess(vehicle.getId(),carrier.getId());
						}
					}
				}

				log.info("started tracking vehicle #" + vehicle.getId().toString());
			}
		}


		for (Carrier carrier : carriers.getCarriers().values()) {

		// for all shipments and services of the carriers, tracking is started here.
		// once the vehicle or personIds can be obtained from the carrierFile, here would be a good moment to set that info to the trackers.

			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentTracking.addTracker(shipment);
			}

			for (CarrierService service : carrier.getServices().values()) {
				serviceTracking.addTracker(service, carrier.getId());
			}

			// If expectedArrivalTimes are given, those are passed unto the serviceTrackers.
			// Otherwise they are estimated from the TourElements
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				Double calculatedArrivalTime = 0.0;
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.Leg) {
						calculatedArrivalTime = ((Tour.Leg) tourElement).getExpectedDepartureTime() + ((Tour.Leg) tourElement).getExpectedTransportTime();
					}
					if (tourElement instanceof Tour.ServiceActivity) {
						Tour.ServiceActivity serviceAct = (Tour.ServiceActivity) tourElement;
							Id<CarrierService> serviceId = serviceAct.getService().getId();
						serviceTracking.setExpectedArrival(serviceId, serviceAct.getExpectedArrival());
						if (calculatedArrivalTime > 0.0) {
							serviceTracking.setCalculatedArrival(serviceId, calculatedArrivalTime);
							calculatedArrivalTime = 0.0;
						}
					}
				}
			}
		}
	}


	// evaluating the ActivityEvents to track vehicle usage and guess what happens with services and shipments
	@Override
	public void handleEvent(ActivityStartEvent activityStartEvent) {
		if (activityStartEvent.getActType().equals("end")) {
			vehicleTracking.endVehicleUsage(activityStartEvent.getPersonId());
		}
		if (activityStartEvent.getActType().equals("service")) {
			serviceTracking.trackServiceActivityStart(activityStartEvent);
		}

		if (activityStartEvent.getActType().equals("delivery")) {
			shipmentTracking.trackDeliveryActivity(activityStartEvent);
		}

		if (activityStartEvent.getActType().equals("pickup")){
			shipmentTracking.trackPickupActivity(activityStartEvent);
		}

	}

	// link events are used to calculate vehicle travel time and distance
	@Override
	public void handleEvent(LinkEnterEvent linkEnterEvent) {
		vehiclesOnLink.put(linkEnterEvent.getVehicleId(), linkEnterEvent.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
		if (vehiclesOnLink.containsKey(linkLeaveEvent.getVehicleId())) {
			Double onLinkTime = vehiclesOnLink.get(linkLeaveEvent.getVehicleId()) - linkLeaveEvent.getTime();
			Double linkLength = network.getLinks().get(linkLeaveEvent.getLinkId()).getLength();

			// (the method checks for itself whether the vehicle is to be tracked or not)
			vehicleTracking.addLeg(linkLeaveEvent.getVehicleId(), onLinkTime, linkLength, false);
		}
	}

	// Person<>Vehicle relations and vehicle usage times are tracked
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleTracking.addDriver2Vehicle(event.getPersonId(), event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		vehicleTracking.registerVehicleLeave(event);
	}

	// LSP Events for Shipments, those are UNTESTED
	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		shipmentTracking.trackDeliveryEvent(event);
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		shipmentTracking.trackPickedUpEvent(event);
	}

	@Override
	public void handleEvent(LSPServiceEndEvent event) {
		serviceTracking.handleEndEvent(event);
	}

	@Override
	public void handleEvent(LSPServiceStartEvent event) {
		serviceTracking.handleStartEvent(event);
	}










	// ##################################################
	// Export methods


	// Export vehicle Statistics to single TSV
	public void exportVehicleInfo(String path){
		exportVehicleInfo(path, false);
	}
	public void exportVehicleInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleStats.tsv"));
			out.write("VehicleId	VehicleType	CarrierId	DriverID	usageTime	roadTime	travelDistance	vehicleCost	legCount");
			out.newLine();
			HashMap<Id<Vehicle>, VehicleTracker> trackers = vehicleTracking.getTrackers();
			for (Id vehId : trackers.keySet()) {
				VehicleTracker tracker = trackers.get(vehId);
				String lastDriverIdString = id2String(tracker.lastDriverId);
				// if the carrier is not certain, export the guess if that is wanted.
				String carrierIdString = (tracker.carrierId==null && exportGuesses) ? "?" + id2String(tracker.carrierIdGuess) : id2String(tracker.carrierId);
				out.write(vehId.toString() + "	" + tracker.vehicleType.getId().toString() + "	" + carrierIdString + "	" + lastDriverIdString + "	" + tracker.usageTime.toString() + "	" + tracker.roadTime.toString() + "	" + tracker.travelDistance.toString() + "	" + tracker.cost.toString() + "	" + tracker.tripHistory.size());
				out.newLine();
			}
			out.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			return; // TODO
		}
	}

	// Export Vehicle Statistics per Trip to TSV
	public void exportVehicleTripInfo(String path){
		exportVehicleTripInfo(path, false );
	}
	public void exportVehicleTripInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleTripStats.tsv"));
			out.write("VehicleId	VehicleType	LegNumber	CarrierId	DriverID	tripRoadTime	tripDistance	tripVehicleCost");
			out.newLine();
			HashMap<Id<Vehicle>, VehicleTracker> trackers = vehicleTracking.getTrackers();
			for (Id vehId : trackers.keySet()) {
				VehicleTracker tracker = trackers.get(vehId);
				Integer i = 0;
				for (VehicleTracker.VehicleTrip trip : tracker.tripHistory) {
					// if info is not certain, export the guess if that is wanted.
					String driverIdString = trip.driverId == null ? "" : trip.driverId.toString();
					String carrierIdString = (tracker.carrierId == null && exportGuesses) ? "?" + id2String(tracker.carrierIdGuess) : tracker.carrierId.toString();
					out.write(vehId.toString() + "	" + tracker.vehicleType.getId().toString() + "	" + i.toString() + "	" + carrierIdString + "	" + driverIdString + "	" + trip.travelTime + "	" + trip.travelDistance + "	" + trip.cost);
					out.newLine();
					i++;
				}
			}
			out.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			return; // TODO
		}
	}

	// Export Vehicle Statistics grouped by VehicleType to individual TSV files per carrier
	public void exportCarrierInfo(String path){
		//there are no guesses to be exported as of now, still having this method to keep the export calls consistent
		exportCarrierInfo(path, false);
	}
	public void exportCarrierInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter singleFile = new BufferedWriter(new FileWriter(path + "/carrierStats.tsv"));
			singleFile.write("carrierId	vehicleType	vehicleCount	totalDistance	totalServiceTime	totalRoadTime	totalCost");
			singleFile.newLine();
			for (Carrier carrier : carriers.getCarriers().values()) {
			HashMap<String, CarrierVehicleTypeStats> vehicleTypeStatsMap = new HashMap<>();
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/carrier_" + carrier.getId().toString() + "_Stats.tsv"));
				for (VehicleTracker tracker : vehicleTracking.getTrackers().values()) {
					// if desired get carrierIdString, in which case the vehicleType gets the "?" prefix to separate guessed vehicle connections from non-guessed ones, even if they are of the same vehicle type
					String carrierIdString = tracker.carrierId==null && exportGuesses ? id2String(tracker.carrierIdGuess) : id2String(tracker.carrierId);
					String vehicleTypeString = tracker.carrierId==null && exportGuesses ? "?" + tracker.vehicleType.getId().toString() : tracker.vehicleType.getId().toString();

					if (carrierIdString.equals(id2String(carrier.getId()))) {
						if (!vehicleTypeStatsMap.containsKey(vehicleTypeString)) {
							vehicleTypeStatsMap.put(vehicleTypeString, new CarrierVehicleTypeStats());
						}
						CarrierVehicleTypeStats cVtStTr = vehicleTypeStatsMap.get(vehicleTypeString);
						cVtStTr.vehicleCount++;
						cVtStTr.totalCost += tracker.cost;
						cVtStTr.totalDistance += tracker.travelDistance;
						cVtStTr.totalRoadTime += tracker.roadTime;
						cVtStTr.totalServiceTime += tracker.usageTime;
					}
				}
			out.write("carrierId	vehicleType	vehicleCount	totalDistance	totalServiceTime	totalRoadTime	totalCost");
			out.newLine();
				for (String vt : vehicleTypeStatsMap.keySet()) {
					CarrierVehicleTypeStats vts = vehicleTypeStatsMap.get(vt);
					out.write(carrier.getId().toString() +"	" + vt + "	" + vts.vehicleCount.toString() + "	" + vts.totalDistance.toString() + "	" + vts.totalServiceTime + "	" + vts.totalRoadTime + "	" + vts.totalCost);
					singleFile.write(carrier.getId().toString() + "	" + vt + "	" + vts.vehicleCount.toString() + "	" + vts.totalDistance.toString() + "	" + vts.totalServiceTime + "	" + vts.totalRoadTime + "	" + vts.totalCost);
					out.newLine();
					singleFile.newLine();
				}
				out.close();
		}
			singleFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Export Statistics of performed services
	public void exportServiceInfo(String path){
		exportServiceInfo(path, false);
	}
	public void exportServiceInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/serviceStats.tsv"));
			out.write("carrierId	serviceId	driverId	vehicleId	ServiceETA	TourETA ArrivalTime");
			out.newLine();
			for (ServiceTracker serviceTracker : serviceTracking.getTrackers().values()) {
				String carrierIdString = id2String(serviceTracker.carrierId);
				String serviceIdString = id2String(serviceTracker.serviceId);
				// if info is not certain, export the guess if that is wanted.
				String driverIdString = (exportGuesses && serviceTracker.driverId == null) ? "?" + id2String(serviceTracker.driverIdGuess) : id2String(serviceTracker.driverId);
				String vehicleIdString = (vehicleTracking.getDriver2VehicleId(serviceTracker.driverId) == null && exportGuesses) ? "?" + id2String(vehicleTracking.getDriver2VehicleId(serviceTracker.driverIdGuess)) : id2String(vehicleTracking.getDriver2VehicleId(serviceTracker.driverId));
				String arrivalTime = (exportGuesses && serviceTracker.startTime == 0.0) ? "?" + serviceTracker.arrivalTimeGuess.toString() : serviceTracker.startTime.toString();
				out.write(carrierIdString + "	" + serviceIdString + "	" + driverIdString + "	" + vehicleIdString + "	" + serviceTracker.expectedArrival + "	" + serviceTracker.calculatedArrival + "	" + arrivalTime);
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Export Info about Shipments to individual and per-carrier TSV
	public void exportShipmentInfo(String path){
		exportShipmentInfo(path, false);
	}
	public void exportShipmentInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter singleFile = new BufferedWriter(new FileWriter(path + "/shipmentStats.tsv"));
			singleFile.write("carrierId	shipmentId	driverId	vehicleId	pickupTime	deliveryTime	deliveryDuration	beelineDistance");
			singleFile.newLine();
			for (Carrier carrier : carriers.getCarriers().values()) {
				HashMap<VehicleType, CarrierVehicleTypeStats> vehicleTypeStatsMap = new HashMap<>();
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/shipments_carrier_" + carrier.getId().toString() + ".tsv"));
				out.write("carrierId	shipmentId	driverId	vehicleId	pickupTime	deliveryTime	deliveryDuration	beelineDistance");
				out.newLine();
				for (CarrierShipment shipment : carrier.getShipments().values()) {
					ShipmentTracker shipmentTracker = shipmentTracking.getShipments().get(shipment.getId());
					if (shipmentTracker == null) {
						continue;
					}
					Id<Link> from = shipment.getFrom();
					Id<Link> toLink = shipment.getTo();
					// if info is not certain, export the guess if that is wanted.
					String carrierIdString = id2String(carrier.getId());
					String shipmentIdString = id2String(shipment.getId());
					String driverIdString = (shipmentTracker.driverId == null && exportGuesses) ? "?" + id2String(shipmentTracker.driverIdGuess) : id2String(shipmentTracker.driverId);
					String vehicleIdString = (vehicleTracking.getDriver2VehicleId(shipmentTracker.driverId) == null && exportGuesses) ? "?" + id2String(vehicleTracking.getDriver2VehicleId(shipmentTracker.driverIdGuess)) : id2String(vehicleTracking.getDriver2VehicleId(shipmentTracker.driverId));
					// calculate euclidean Distance between from and to for comparsion
					double dist = NetworkUtils.getEuclideanDistance(network.getLinks().get(from).getCoord(), network.getLinks().get(toLink).getCoord());
					out.write(carrierIdString + "	" + shipment.getId().toString() + "	" + driverIdString + "	" + vehicleIdString + "	"  + shipmentTracker.pickUpTime.toString() + "	" + shipmentTracker.deliveryTime.toString() + "	" + shipmentTracker.deliveryDuration.toString() + "	" +dist);
					out.newLine();
					singleFile.write(carrierIdString + "	" + shipmentIdString + "	" + driverIdString + "	" + vehicleIdString + "	"  + shipmentTracker.pickUpTime.toString() + "	" + shipmentTracker.deliveryTime.toString() + "	" + shipmentTracker.deliveryDuration.toString() + "	" + dist);
					singleFile.newLine();
				}
				out.close();
			}
			singleFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	private String id2String(Id id){ //Failsafe Id to String - Converter, because Id.toString() throws Exception if the Id is null.
		return id==null?" ":id.toString(); // return space because instead of empty string because TSV files get confused otherwise
	}


/*
// for as long as the connection tour->vehicle/agent cannot be made with services, one tour=one vehicle could be assumed. Result would be the regular vehicleStatsExport, but filtered by Carrier, which can be done without much effort anyways.
	public void exportCarrierTourStats(String path){
		HashMap<Id<Carrier>, Set<VehicleTracker>> carrierVehicles = new HashMap<>();
		for (VehicleTracker vt:vehicleTracking.getTrackers().values()) {
			if (vt.carrierId != null) {
				if (!carrierVehicles.containsKey(vt.carrierId)) {
					carrierVehicles.put(vt.carrierId, new HashSet<>());
				}
				carrierVehicles.get(vt.carrierId).add(vt);
			}
		}

		try {
			for(Id<Carrier> carrierId:carrierVehicles.keySet()){
				BufferedWriter out = new BufferedWriter(new FileWriter(path+"/tourStats_carrier_" + carrierId.toString() + ".tsv"));
				for(VehicleTracker vt:carrierVehicles.get(carrierId)){

				}
				out.write();
				out.close();
			}
			} catch (IOException e) {
				e.printStackTrace();
			}

	}
*/
}