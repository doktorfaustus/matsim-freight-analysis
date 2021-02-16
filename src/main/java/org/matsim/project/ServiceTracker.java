package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

import java.util.HashSet;

public class ServiceTracker {
	public CarrierService service;
	public Id<CarrierService> serviceId;
	public Double calculatedArrival =0.0;
	public Id<Carrier> carrierId ;
	public Id<Person> driverId ;
	public Double arrivalTime =0.0;
	public HashSet<Id<Person>> possibleDrivers = new HashSet<>();
	public Id<Person> driverIdGuess;
	public Double arrivalTimeGuess = 0.0;
	public double expectedArrival = 0.0;

	public ServiceTracker(CarrierService service) {
		this.serviceId=service.getId();
		this.service=service;
	}
}
