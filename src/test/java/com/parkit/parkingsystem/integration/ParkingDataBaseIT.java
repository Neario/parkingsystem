package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.*;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.*;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static final String VEHICLE_REG_NUMBER = "ABCDEF";
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        //
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REG_NUMBER);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        //GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        //WHEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        parkingService.processIncomingVehicle();
        //THEN
        Ticket ticket = ticketDAO.getTicket(VEHICLE_REG_NUMBER);
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(VEHICLE_REG_NUMBER, ticket.getVehicleRegNumber());
        Assertions.assertNotNull(ticket.getInTime());
        Assertions.assertNull(ticket.getOutTime());
        Assertions.assertEquals(0, ticket.getPrice());
    }

    @Test
    public void testParkingLotExit() {
        when(inputReaderUtil.readSelection()).thenReturn(1);

        ParkingService parkingService =
                new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(VEHICLE_REG_NUMBER);
        assertNotNull(ticket);

        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setOutTime(new Date());

        parkingService.processExitingVehicle();
        Ticket updatedTicket = ticketDAO.getTicket(VEHICLE_REG_NUMBER);
        assertNotNull(updatedTicket.getOutTime());
        assertTrue(updatedTicket.getPrice() >= 0);
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        // GIVEN incoming Vehicle
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        final ParkingSpot parkingSpot1 = new ParkingSpot(1, ParkingType.CAR, true);
        final Ticket ticket1 = new Ticket();
        ticket1.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket1.setOutTime(new Date(System.currentTimeMillis() - (50 * 60 * 1000)));
        ticket1.setParkingSpot(parkingSpot1);
        ticket1.setVehicleRegNumber(VEHICLE_REG_NUMBER);
        ticketDAO.saveTicket(ticket1);


        final ParkingSpot parkingSpot2 = new ParkingSpot(1, ParkingType.CAR, false);
        final Ticket ticket2 = new Ticket();
        ticket2.setInTime(new Date(System.currentTimeMillis() - (45 * 60 * 1000)));
        ticket2.setParkingSpot(parkingSpot2);
        ticket2.setVehicleRegNumber(VEHICLE_REG_NUMBER);
        ticketDAO.saveTicket(ticket2);

        final double expectedPrice = (Fare.CAR_RATE_PER_HOUR * 0.75) * 0.95;
        // WHEN vehicle exiting
        parkingService.processExitingVehicle();
        //THEN
        Ticket secondTicket = ticketDAO.getTicket(VEHICLE_REG_NUMBER);

        Assertions.assertNotNull(secondTicket);
        Assertions.assertEquals(roundPrice(expectedPrice), roundPrice(secondTicket.getPrice()));
    }

    private BigDecimal roundPrice(double price) {
        BigDecimal bigDecimal = new BigDecimal(price);
        return bigDecimal.setScale(2, RoundingMode.HALF_UP);
    }

}
