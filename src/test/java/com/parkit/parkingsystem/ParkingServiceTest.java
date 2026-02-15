package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.*;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.stream.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static final String VEHICLE_REG_NUMBER = "ABCDEF";

    private ParkingService parkingService;

    @Mock
    private InputReaderUtil inputReaderUtil;
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    @Mock
    private TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void processExitingVehicleTest(ParkingType type) throws Exception {
        //GIVEN
        Ticket ticket = newTicket(type);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REG_NUMBER);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNbTicket(VEHICLE_REG_NUMBER)).thenReturn(1);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        //WHEN
        parkingService.processExitingVehicle();
        //THEN
        ArgumentCaptor<Ticket> ticketArgumentCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketDAO, Mockito.times(1)).updateTicket(ticketArgumentCaptor.capture());

        Ticket ticketArgument = ticketArgumentCaptor.getValue();

        Assertions.assertNotNull(ticketArgument);
        Assertions.assertNotNull(ticketArgument.getParkingSpot());
        Assertions.assertNotNull(ticketArgument.getVehicleRegNumber());
        Assertions.assertNotNull(ticketArgument.getInTime());
        Assertions.assertNotNull(ticketArgument.getOutTime());
        Assertions.assertTrue(ticketArgument.getPrice() > 0);

        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

    }

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testProcessIncomingVehicle(ParkingType parkingType) throws Exception {
        //GIVEN
        int menuChoice = (parkingType == ParkingType.CAR) ? 1 : 2;
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType,true);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REG_NUMBER);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(inputReaderUtil.readSelection()).thenReturn(menuChoice);
        when(parkingSpotDAO.getNextAvailableSlot(parkingType)).thenReturn(parkingSpot.getId());
        when(ticketDAO.getNbTicket(VEHICLE_REG_NUMBER)).thenReturn(0);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
        // WHEN
        parkingService.processIncomingVehicle();
        // THEN
        ArgumentCaptor<ParkingSpot> parkingSpotArgumentCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(parkingSpotDAO).updateParking(parkingSpotArgumentCaptor.capture());
        Assertions.assertFalse(parkingSpotArgumentCaptor.getValue().isAvailable());

        ArgumentCaptor<Ticket> ticketArgumentCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketDAO).saveTicket(ticketArgumentCaptor.capture());

        Ticket ticketArgument = ticketArgumentCaptor.getValue();
        Assertions.assertNotNull(ticketArgument);
        Assertions.assertEquals(VEHICLE_REG_NUMBER, ticketArgument.getVehicleRegNumber());
        Assertions.assertEquals(parkingSpot, ticketArgument.getParkingSpot());
        Assertions.assertNotNull(ticketArgument.getInTime());
        Assertions.assertNull(ticketArgument.getOutTime());
        Assertions.assertEquals(0, ticketArgument.getPrice());

        verify(ticketDAO, never()).updateTicket(any());

    }

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void processExitingVehicleTestUnableUpdate(ParkingType parkingType) throws Exception {
        //GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, false);
        Ticket ticket = newTicket(parkingType);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REG_NUMBER);
        when(ticketDAO.getTicket(VEHICLE_REG_NUMBER)).thenReturn(ticket);
        when(ticketDAO.getNbTicket(VEHICLE_REG_NUMBER)).thenReturn(1);
        when(ticketDAO.updateTicket(ticket)).thenReturn(false);
        //WHEN
        parkingService.processExitingVehicle();
        //THEN
        verify(ticketDAO, times(1)).updateTicket(ticket);
        verify(parkingSpotDAO, never()).updateParking(parkingSpot);
    }

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testGetNextParkingNumberIfAvailable(ParkingType parkingType) {
        //GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, true);
        int menuChoice = (parkingType == ParkingType.CAR) ? 1 : 2;

        when(inputReaderUtil.readSelection()).thenReturn(menuChoice);
        when(parkingSpotDAO.getNextAvailableSlot(parkingType)).thenReturn(parkingSpot.getId());
        //WHEN
        ParkingSpot parkingSpotResult = parkingService.getNextParkingNumberIfAvailable();
        //THEN
        Assertions.assertNotNull(parkingSpotResult);
        Assertions.assertEquals(parkingSpot.getId(), parkingSpotResult.getId());
        Assertions.assertTrue(parkingSpotResult.isAvailable());
        verify(parkingSpotDAO).getNextAvailableSlot(parkingType);
    }

    @ParameterizedTest
    @EnumSource(ParkingType.class)
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(ParkingType parkingType) {
        //GIVEN
        int  menuChoice = (parkingType == ParkingType.CAR) ? 1 : 2;
        when(inputReaderUtil.readSelection()).thenReturn(menuChoice);
        when(parkingSpotDAO.getNextAvailableSlot(parkingType)).thenReturn(0);
        //WHEN
        ParkingSpot parkingSpotResult = parkingService.getNextParkingNumberIfAvailable();
        //THEN
        Assertions.assertNull(parkingSpotResult);
        verify(parkingSpotDAO).getNextAvailableSlot(parkingType);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3);
        //WHEN
        ParkingSpot parkingSpotResult = parkingService.getNextParkingNumberIfAvailable();
        //THEN
        Assertions.assertNull(parkingSpotResult);
        verify(parkingSpotDAO, never()).getNextAvailableSlot(any());
    }

    private Ticket newTicket(ParkingType type) {
        ParkingSpot spot = new ParkingSpot(1, type, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber(VEHICLE_REG_NUMBER);
        return ticket;
    }
}
