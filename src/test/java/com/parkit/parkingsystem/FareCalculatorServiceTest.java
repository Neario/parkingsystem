package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.stream.Stream;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @BeforeAll
    public static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    public void setUpPerTest() {
        ticket = new Ticket();
    }

    @ParameterizedTest
    @MethodSource("fareArguments")
    public void calculateFares(ParkingType parkingType, long duration, double expectedPrice ){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  duration * 60L * 1_000L) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(expectedPrice, ticket.getPrice());
    }

    @Test
    public void calculateFareUnkownType(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, null,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithFutureInTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (0.75 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithMoreThanADayParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );//24 hours parking time should give 24 * parking fare per hour
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @ParameterizedTest
    @CsvSource({ "CAR, 20", "BIKE, 25" })
    public void calculateFareVehicleWithLessThan30minutesParkingTime(ParkingType type, long minutes) {
        ParkingSpot parkingSpot = new ParkingSpot(1, type,false);
        Date  inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  minutes * 60 * 1000) );
        Date outTime = new Date();

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0, ticket.getPrice());
    }

    @ParameterizedTest
    @MethodSource("fareArgumentsWithDiscount")
    public void calculateFareVehicleWithDiscount(ParkingType type, long minutes, double expected) {
        ParkingSpot parkingSpot = new ParkingSpot(1, type,false);
        Date  inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  minutes * 60 * 1000) );

        ticket.setInTime(inTime);
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(parkingSpot);

        fareCalculatorService.calculateFare(ticket, true);
        assertEquals(expected, ticket.getPrice(),0.001);

    }

    private static  Stream<Arguments> fareArguments(){
        return Stream.of(
                Arguments.of(ParkingType.CAR, 60, Fare.CAR_RATE_PER_HOUR),
                Arguments.of(ParkingType.BIKE, 60, Fare.BIKE_RATE_PER_HOUR)
        );

    }

    private static  Stream<Arguments> fareArgumentsWithDiscount(){
        return Stream.of(
                Arguments.of(ParkingType.CAR, 60, Fare.CAR_RATE_PER_HOUR * 0.95),
                Arguments.of(ParkingType.BIKE, 60, Fare.BIKE_RATE_PER_HOUR * 0.95)
        );

    }

}
