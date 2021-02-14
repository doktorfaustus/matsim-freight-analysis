package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.*;
//import org.matsim.contrib.freight.events.ShipmentDeliveredEventHandler;
//import org.matsim.contrib.freight.events.ShipmentPickedUpEventHandler;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
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

public class FreightAnalysisEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler {
	private final static Logger log = Logger.getLogger(FreightAnalysisEventHandler.class);
	private Vehicles vehicles;
	private Network network;
	private Carriers carriers;
	HashMap<Id<Vehicle>, Double> vehiclesOnLink = new HashMap<>();
	FreightAnalysisVehicleTracking freightAnalysisVehicleTracking = new FreightAnalysisVehicleTracking();
	FreightAnalysisShipmentTracking shipmentTracking = new FreightAnalysisShipmentTracking();
	FreightAnalysisServiceTracking serviceTracking = new FreightAnalysisServiceTracking();

	public FreightAnalysisEventHandler(Network network, Vehicles vehicles, Carriers carriers) {
		this.network = network;
		this.vehicles = vehicles;
		this.carriers = carriers;

		//for (Carrier carrier: carriers.getCarriers().values()){ // Für alle "echten" Frachtfahrzeuge wird ein Tracker angelegt.
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			if (vehicle.getId().toString().contains("freight")) {
				freightAnalysisVehicleTracking.addTracker(vehicle);//CarrierId hier noch nicht bekannt
				log.info("started tracking vehicle #" + vehicle.getId().toString());
			}
		}


		for (Carrier carrier : carriers.getCarriers().values()) {
			for (Vehicle vehicle : vehicles.getVehicles().values()) {
				if (vehicle.getId().toString().contains("veh_carrier_" + carrier.getId())) {
					freightAnalysisVehicleTracking.addCarrier2Vehicle(vehicle.getId(), carrier.getId()); // funktioniert nicht, weil Ids nicht stimmen.
				}
			}
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentTracking.addTracker(shipment);
			}

			for (CarrierService service : carrier.getServices().values()) {
				serviceTracking.addTracker(service);
			}
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				Double calculatedArrivalTime = 0.0;
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.Leg) {
						calculatedArrivalTime = ((Tour.Leg) tourElement).getExpectedDepartureTime() + ((Tour.Leg) tourElement).getExpectedTransportTime();
					}
					if (tourElement instanceof Tour.ServiceActivity) { //TODO find out whether services can exist that are not included in a tour. If not, the servicetrackers could be constructed here. The current way enables detection of shipments not bound to tours.
						Id<CarrierService> serviceId = ((Tour.ServiceActivity) tourElement).getService().getId();
						serviceTracking.trackers.get(serviceId).expectedArrival = ((Tour.ServiceActivity) tourElement).getExpectedArrival();
						serviceTracking.trackers.get(serviceId).linkId = ((Tour.ServiceActivity) tourElement).getLocation();
						if (calculatedArrivalTime > 0.0) {
							serviceTracking.trackers.get(serviceId).calculatedArrival = calculatedArrivalTime; //in case there is no expected Arrival for the service itself, we can at least track wether the estimate based on travel and departure times in the tour was correct.
							calculatedArrivalTime = 0.0;
						}
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent activityEndEvent) {
		if (activityEndEvent.getActType().equals("start")) {

		}
	}

	@Override
	public void handleEvent(ActivityStartEvent activityStartEvent) {
		if (activityStartEvent.getActType().equals("end")) {
			freightAnalysisVehicleTracking.endVehicleUsage(activityStartEvent.getPersonId());
			VehicleTracker vehicleUnit = freightAnalysisVehicleTracking.getTrackers().get(freightAnalysisVehicleTracking.getDriver2VehicleId(activityStartEvent.getPersonId()));
			vehicleUnit.serviceTime += activityStartEvent.getTime() - vehicleUnit.usageStartStime;
		}
		if (activityStartEvent.getActType().equals("service")) {
			activityStartEvent.getPersonId();
			activityStartEvent.getLinkId();
			activityStartEvent.getTime();
		}

		if (activityStartEvent.getActType().equals("delivery")) {
			activityStartEvent.getPersonId();
			shipmentTracking.trackDeliveryActivity(activityStartEvent);
		}

		if (activityStartEvent.getActType().equals("delivery")) {
			activityStartEvent.getPersonId();
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

			if (vehicles.getVehicles().get(linkLeaveEvent.getVehicleId()).getType().getCapacity().getOther() > 0) {
				freightAnalysisVehicleTracking.addLeg(linkLeaveEvent.getVehicleId(), onLinkTime, linkLength, false);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		freightAnalysisVehicleTracking.addDriver2Vehicle(event.getPersonId(), event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		freightAnalysisVehicleTracking.registerVehicleLeave(event);
	}


	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		shipmentTracking.trackDeliveryEvent(event);
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		shipmentTracking.trackPickedUpEvent(event);
	}

	public void exportVehicleInfo(String path) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleStats.tsv"));
			out.write("VehicleId	VehicleType	CarrierId	DriverID	serviceTime	roadTime	travelDistance	vehicleCost	legCount");
			out.newLine();
			HashMap<Id<Vehicle>, VehicleTracker> trackers = freightAnalysisVehicleTracking.getTrackers();
			for (Id vehId : trackers.keySet()) { //das funktioniert so nicht mehr, wenn alle Tracker hier gebündelt sind.
				VehicleTracker tracker = trackers.get(vehId);
				String lastDriverIdString = id2String(tracker.lastDriverId);
				String carrierIdString = id2String(tracker.carrierId);
				out.write(vehId.toString() + "	" + tracker.vehicleType.getId().toString() + "	" + carrierIdString + "	" + lastDriverIdString + "	" + tracker.serviceTime.toString() + "	" + tracker.travelTime.toString() + "	" + tracker.travelDistance.toString() + "	" + tracker.cost.toString() + "	" + tracker.tripHistory.size());
				out.newLine();
			}
			out.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			return; // TODO
		}
	}

	public void exportVehicleTripInfo(String path) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleTripStats.tsv"));
			out.write("VehicleId	VehicleType	LegNumber	CarrierId	DriverID	tripRoadTime	tripDistance	tripVehicleCost");
			out.newLine();
			HashMap<Id<Vehicle>, VehicleTracker> trackers = freightAnalysisVehicleTracking.getTrackers();
			for (Id vehId : trackers.keySet()) { //das funktioniert so nicht mehr, wenn alle Tracker hier gebündelt sind.
				VehicleTracker tracker = trackers.get(vehId);
				Integer i = 0;
				for (VehicleTracker.VehicleTrip trip : tracker.tripHistory) {
					String driverIdString = trip.driverId == null ? "" : trip.driverId.toString();
					String carrierIdString = tracker.carrierId == null ? "" : tracker.carrierId.toString();
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

	public void exportCarrierInfo(String path) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			HashMap<VehicleType, CarrierVehicleTypeStats> vehicleTypeStatsMap = new HashMap<>();
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/carrier_" + carrier.getId().toString() + "_Stats.tsv"));
				for (VehicleTracker tracker : freightAnalysisVehicleTracking.getTrackers().values()) {
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

	public void exportServiceInfo(String path) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/serviceStats.tsv"));
			out.write("carrierID	serviceId	currentDriverId	vehicleType	ServiceETA	TourETA ArrivalTime");
			out.newLine();
			for (ServiceTracker serviceTracker : serviceTracking.trackers.values()) {
				String carrierIdString = id2String(serviceTracker.carrierId);
				String serviceIdString = id2String(serviceTracker.serviceId);
				String driverIdString = id2String(serviceTracker.driverId);
				out.write(carrierIdString + "	" + serviceIdString + "	" + driverIdString + "	" + serviceTracker.expectedArrival + "	" + serviceTracker.calculatedArrival + "	" + serviceTracker.arrivalTime);
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exportShipmentInfo(String path) {
		try {
			BufferedWriter singleFile = new BufferedWriter(new FileWriter(path + "/shipmentStats.tsv"));
			for (Carrier carrier : carriers.getCarriers().values()) {
				HashMap<VehicleType, CarrierVehicleTypeStats> vehicleTypeStatsMap = new HashMap<>();
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/shipments_carrier_" + carrier.getId().toString() + ".tsv"));
				out.write("carrierId	shipmentId	driverId	vehicleId	onTimePickup	onTimeDelivery	pickupTime	deliveryTime	deliveryDuration");
				out.newLine();
				singleFile.write("carrierId	shipmentId	driverId	vehicleId	onTimePickup	onTimeDelivery	pickupTime	deliveryTime	deliveryDuration");
				singleFile.newLine();
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
					String driverIdString = shipmentTracker.driverId==null?id2String(shipmentTracker.driverIdGuess):id2String(shipmentTracker.driverId);
					String vehicleIdString = (freightAnalysisVehicleTracking.getDriver2VehicleId(shipmentTracker.driverId) == null) ? "" : freightAnalysisVehicleTracking.getDriver2VehicleId(shipmentTracker.driverId).toString();


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
		return id==null?"":id.toString();
	}


/*
// for as long as the connection tour->vehicle/agent cannot be made with services, one tour=one vehicle could be assumed. Result would be the regular vehicleStatsExport, but filtered by Carrier, which can be done without much effort anyways.
	public void exportCarrierTourStats(String path){
		HashMap<Id<Carrier>, Set<VehicleTracker>> carrierVehicles = new HashMap<>();
		for (VehicleTracker vt:freightAnalysisVehicleTracking.getTrackers().values()) {
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