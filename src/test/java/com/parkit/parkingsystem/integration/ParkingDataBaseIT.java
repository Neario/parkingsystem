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

import java.util.*;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    public static final String vehiculeRegistrationNumber = "ABCDEF";
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehiculeRegistrationNumber);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar(){
        //GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        //WHEN
        parkingService.processIncomingVehicle();
        //THEN
        Ticket ticket = ticketDAO.getTicket(vehiculeRegistrationNumber);
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(vehiculeRegistrationNumber, ticket.getVehicleRegNumber());
        Assertions.assertNotNull(ticket.getInTime());
        Assertions.assertNull(ticket.getOutTime());
        Assertions.assertEquals(0, ticket.getPrice());
    }

    @Test
    public void testParkingLotExit(){
        //GIVEN
        testParkingACar(); // <--JAMAIS
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        //WHEN
        parkingService.processExitingVehicle();
        //THEN
        Ticket ticket = ticketDAO.getTicket(vehiculeRegistrationNumber);
        Assertions.assertNotNull(ticket.getOutTime());
        Assertions.assertTrue(ticket.getPrice() >= 0);
        Assertions.assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        parkingService.processExitingVehicle();
        Ticket firstTicket = ticketDAO.getTicket(vehiculeRegistrationNumber);

        Assertions.assertNotNull(firstTicket);

        parkingService.processIncomingVehicle();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        parkingService.processExitingVehicle();
        Ticket secondTicket = ticketDAO.getTicket(vehiculeRegistrationNumber);

        Assertions.assertNotNull(secondTicket);
        int nbTicket = ticketDAO.getNbTicket(vehiculeRegistrationNumber);

    }

}
