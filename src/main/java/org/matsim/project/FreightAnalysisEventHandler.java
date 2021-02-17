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

		//for (Carrier carrier: carriers.getCarriers().values()){ // Für alle "echten" Frachtfahrzeuge wird ein Tracker angelegt.
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			String vehicleIdString = vehicle.getId().toString();
			if (vehicle.getId().toString().contains("freight")) {
				vehicleTracking.addTracker(vehicle);//CarrierId hier noch nicht bekannt

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

			// insert carrier>vehicle mapping as soon as vehicle id can be obtained from carrier plans
			//for (Vehicle vehicle : vehicles.getVehicles().values()) {
			//}

			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentTracking.addTracker(shipment);
			}

			for (CarrierService service : carrier.getServices().values()) {
				serviceTracking.addTracker(service, carrier.getId());
			}
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				Double calculatedArrivalTime = 0.0;
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.Leg) {
						calculatedArrivalTime = ((Tour.Leg) tourElement).getExpectedDepartureTime() + ((Tour.Leg) tourElement).getExpectedTransportTime(); // schätzen der Ankunftszeit aus TourLegs
					}
					if (tourElement instanceof Tour.ServiceActivity) { //TODO find out whether services can exist that are not included in a tour. If not, the servicetrackers could be constructed here. The current way enables detection of shipments not bound to tours.
						Tour.ServiceActivity serviceAct = (Tour.ServiceActivity) tourElement;
							Id<CarrierService> serviceId = serviceAct.getService().getId();
						serviceTracking.setExpectedArrival(serviceId, serviceAct.getExpectedArrival());
						if (calculatedArrivalTime > 0.0) {
							//in case there is no expected Arrival for the service itself, we can at least track wether the estimate based on travel and departure times in the tour was correct.
							serviceTracking.setCalculatedArrival(serviceId, calculatedArrivalTime);
							calculatedArrivalTime = 0.0;
						}
					}
				}
			}
		}
	}


	@Override
	public void handleEvent(ActivityStartEvent activityStartEvent) {
		if (activityStartEvent.getActType().equals("end")) {
			vehicleTracking.endVehicleUsage(activityStartEvent.getPersonId());
			VehicleTracker vehicleUnit = vehicleTracking.getTrackers().get(vehicleTracking.getDriver2VehicleId(activityStartEvent.getPersonId()));
			vehicleUnit.serviceTime += activityStartEvent.getTime() - vehicleUnit.usageStartStime;
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

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleTracking.addDriver2Vehicle(event.getPersonId(), event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		vehicleTracking.registerVehicleLeave(event);
	}


	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		shipmentTracking.trackDeliveryEvent(event);
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		shipmentTracking.trackPickedUpEvent(event);
	}

	public void exportVehicleInfo(String path){
		exportVehicleInfo(path, false);
	}
	public void exportVehicleInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleStats.tsv"));
			out.write("VehicleId	VehicleType	CarrierId	DriverID	serviceTime	roadTime	travelDistance	vehicleCost	legCount");
			out.newLine();
			HashMap<Id<Vehicle>, VehicleTracker> trackers = vehicleTracking.getTrackers();
			for (Id vehId : trackers.keySet()) {
				VehicleTracker tracker = trackers.get(vehId);
				String lastDriverIdString = id2String(tracker.lastDriverId);
				String carrierIdString = (tracker.carrierId==null && exportGuesses) ? "?" + id2String(tracker.carrierIdGuess) : id2String(tracker.carrierId);
				out.write(vehId.toString() + "	" + tracker.vehicleType.getId().toString() + "	" + carrierIdString + "	" + lastDriverIdString + "	" + tracker.serviceTime.toString() + "	" + tracker.travelTime.toString() + "	" + tracker.travelDistance.toString() + "	" + tracker.cost.toString() + "	" + tracker.tripHistory.size());
				out.newLine();
			}
			out.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			return; // TODO
		}
	}

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

	public void exportCarrierInfo(String path, Boolean exportGuesses){
		//there are no guesses to be exported as of now, still having this method to keep the export calls consistent
		exportCarrierInfo(path);
	}
	public void exportCarrierInfo(String path) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			HashMap<VehicleType, CarrierVehicleTypeStats> vehicleTypeStatsMap = new HashMap<>();
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/carrier_" + carrier.getId().toString() + "_Stats.tsv"));
				for (VehicleTracker tracker : vehicleTracking.getTrackers().values()) {
					if (id2String(tracker.carrierId).equals(id2String(carrier.getId()))) {
						if (!vehicleTypeStatsMap.containsKey(tracker.vehicleType)) {
							vehicleTypeStatsMap.put(tracker.vehicleType, new CarrierVehicleTypeStats());
						}
						CarrierVehicleTypeStats cVtStTr = vehicleTypeStatsMap.get(tracker.vehicleType);
						cVtStTr.vehicleCount++;
						cVtStTr.totalCost += tracker.cost;
						cVtStTr.totalDistance += tracker.travelDistance;
						cVtStTr.totalRoadTime += tracker.travelTime;
						cVtStTr.totalServiceTime += tracker.serviceTime;
					}
				}
				out.write("vehicleType	vehicleCount	totalDistance	totalServiceTime	totalRoadTime	totalCost");
				out.newLine();
				for (VehicleType vt : vehicleTypeStatsMap.keySet()) {
					CarrierVehicleTypeStats vts = vehicleTypeStatsMap.get(vt);
					out.write(vt.getId().toString() + "	" + vts.vehicleCount.toString() + "	" + vts.totalDistance.toString() + "	" + vts.totalServiceTime + "	" + vts.totalRoadTime + "	" + vts.totalCost);
					out.newLine();
				}
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void exportServiceInfo(String path){
		exportServiceInfo(path, false);
	}
	public void exportServiceInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/serviceStats.tsv"));
			out.write("carrierID	serviceId	driverId	vehicleId	ServiceETA	TourETA ArrivalTime");
			out.newLine();
			for (ServiceTracker serviceTracker : serviceTracking.getTrackers().values()) {
				String carrierIdString = id2String(serviceTracker.carrierId);
				String serviceIdString = id2String(serviceTracker.serviceId);
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

	public void exportShipmentInfo(String path){
		exportShipmentInfo(path, false);
	}
	public void exportShipmentInfo(String path, Boolean exportGuesses) {
		try {
			BufferedWriter singleFile = new BufferedWriter(new FileWriter(path + "/shipmentStats.tsv"));
			singleFile.write("carrierId	shipmentId	driverId	vehicleId	onTimePickup	onTimeDelivery	pickupTime	deliveryTime	deliveryDuration");
			singleFile.newLine();
			for (Carrier carrier : carriers.getCarriers().values()) {
				HashMap<VehicleType, CarrierVehicleTypeStats> vehicleTypeStatsMap = new HashMap<>();
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/shipments_carrier_" + carrier.getId().toString() + ".tsv"));
				out.write("carrierId	shipmentId	driverId	vehicleId	onTimePickup	onTimeDelivery	pickupTime	deliveryTime	deliveryDuration");
				out.newLine();
				for (CarrierShipment shipment : carrier.getShipments().values()) {
					ShipmentTracker shipmentTracker = shipmentTracking.getShipments().get(shipment.getId());
					if (shipmentTracker == null) {
						continue;
					}
					Boolean onTimePickup = ((shipment.getPickupTimeWindow().getStart() <= shipmentTracker.pickUpTime) && shipmentTracker.pickUpTime <= shipment.getPickupTimeWindow().getEnd());
					Boolean onTimeDelivery = ((shipment.getDeliveryTimeWindow().getStart()) <= shipmentTracker.pickUpTime) && (shipmentTracker.deliveryTime <= shipment.getDeliveryTimeWindow().getEnd());
					Id<Link> from = shipment.getFrom();
					Id<Link> toLink = shipment.getTo();
					String carrierIdString = id2String(carrier.getId());
					String shipmentIdString = id2String(shipment.getId());
					String driverIdString = (shipmentTracker.driverId == null && exportGuesses) ? "?" + id2String(shipmentTracker.driverIdGuess) : id2String(shipmentTracker.driverId);
					String vehicleIdString = (vehicleTracking.getDriver2VehicleId(shipmentTracker.driverId) == null && exportGuesses) ? "?" + id2String(vehicleTracking.getDriver2VehicleId(shipmentTracker.driverIdGuess)) : id2String(vehicleTracking.getDriver2VehicleId(shipmentTracker.driverId));

					double dist = NetworkUtils.getEuclideanDistance(network.getLinks().get(from).getCoord(), network.getLinks().get(toLink).getCoord());
					out.write(carrierIdString + "	" + shipment.getId().toString() + "	" + driverIdString + "	" + vehicleIdString + "	" + onTimePickup.toString() + "	" + onTimeDelivery.toString() + "	" + shipmentTracker.pickUpTime.toString() + "	" + shipmentTracker.deliveryTime.toString() + "	" + shipmentTracker.deliveryDuration.toString() + dist);
					out.newLine();
					singleFile.write(carrierIdString + "	" + shipmentIdString + "	" + driverIdString + "	" + vehicleIdString + "	" + onTimePickup.toString() + "	" + onTimeDelivery.toString() + "	" + shipmentTracker.pickUpTime.toString() + "	" + shipmentTracker.deliveryTime.toString() + "	" + shipmentTracker.deliveryDuration.toString() + dist);
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
		return id==null?" ":id.toString();
	}

	@Override
	public void handleEvent(LSPServiceEndEvent event) {
		serviceTracking.handleEndEvent(event);
	}

	@Override
	public void handleEvent(LSPServiceStartEvent event) {
		serviceTracking.handleStartEvent(event);
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