package com.flightapp.flightservice.repository;

import com.flightapp.flightservice.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Integer> {
    Optional<Flight> findByFlightNumber(String flightNumber);

    Optional<Flight> findByFromCityAndToCity(String fromCity, String toCity);
}
