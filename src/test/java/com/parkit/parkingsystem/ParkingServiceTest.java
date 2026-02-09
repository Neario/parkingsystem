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

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @Test
    public void processExitingVehicleTest(){
        //GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
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

    private static Stream<Arguments> processIncomingVehicleArguments(){
        return Stream.of(
                Arguments.of(ParkingType.CAR, 1),
                Arguments.of(ParkingType.BIKE, 2)
        );

    }

    @ParameterizedTest
    @MethodSource("processIncomingVehicleArguments")
    public void testProcessIncomingVehicle(ParkingType parkingType, int menuChoice) {
        //GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType,true);

        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(inputReaderUtil.readSelection()).thenReturn(menuChoice);
        when(parkingSpotDAO.getNextAvailableSlot(parkingType)).thenReturn(parkingSpot.getId());
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(0);
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
        Assertions.assertEquals("ABCDEF", ticketArgument.getVehicleRegNumber());
        Assertions.assertEquals(parkingSpot, ticketArgument.getParkingSpot());
        Assertions.assertNotNull(ticketArgument.getInTime());
        Assertions.assertNull(ticketArgument.getOutTime());
        Assertions.assertEquals(0, ticketArgument.getPrice());

        verify(ticketDAO, never()).updateTicket(any());

    }

    private static Stream<Arguments> processExitingVehicleTestUnableUpdateArguments() {
        return Stream.of(
                Arguments.of(ParkingType.CAR),
                Arguments.of(ParkingType.BIKE)
        );
    }

    @ParameterizedTest
    @MethodSource("processExitingVehicleTestUnableUpdateArguments")
    public void processExitingVehicleTestUnableUpdate(ParkingType parkingType) {
        //GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(parkingSpot);
        //WHEN
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
        when(ticketDAO.updateTicket(ticket)).thenReturn(false);

        parkingService.processExitingVehicle();
        //THEN
        verify(ticketDAO, times(1)).updateTicket(ticket);
        verify(parkingSpotDAO, never()).updateParking(parkingSpot);
    }

    private static Stream<Arguments> processGetNextParkingNumberIfAvailableArguments() {
        return Stream.of(
                Arguments.of(ParkingType.CAR, 1),
                Arguments.of(ParkingType.BIKE, 2)
        );

    }

    @ParameterizedTest
    @MethodSource("processGetNextParkingNumberIfAvailableArguments")
    public void testGetNextParkingNumberIfAvailable(ParkingType parkingType, int menuChoice) {
        //GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType, true);

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

    private static Stream<Arguments> processGetNextParkingNumberIfAvailableParkingNumberNotFoundArguments() {
        return Stream.of(
                Arguments.of(ParkingType.CAR, 1),
                Arguments.of(ParkingType.BIKE, 2)
        );
    }

    @ParameterizedTest
    @MethodSource("processGetNextParkingNumberIfAvailableParkingNumberNotFoundArguments")
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(ParkingType parkingType, int menuChoice) {
        //GIVEN
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


}
