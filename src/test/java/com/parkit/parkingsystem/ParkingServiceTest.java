package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {
    @Mock
    private  InputReaderUtil inputReaderUtil;
    @Mock
    private  ParkingSpotDAO parkingSpotDAO;
    @Mock
    private  TicketDAO ticketDAO;
    @Mock
    private FareCalculatorService fareCalculatorService;
    @Mock
    private Clock clock;

    @InjectMocks
    private  ParkingService parkingService;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @Test
    @DisplayName("should set isRegularCustomer to false when numbers of tickets equal 0")
    void processIncomingVehicleOfNonRegularCustomerTest() throws Exception {
        //given
        final String vehicleRegistrationNumber ="ABCDEF";
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(ticketDAO.getNbTickets(vehicleRegistrationNumber)).thenReturn(0);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        //when
        parkingService.processIncomingVehicle();

        //then
        verify(ticketDAO,times(1)).getNbTickets(vehicleRegistrationNumber);
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(ticketDAO, times(1)).saveTicket(ticketCaptor.capture());
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();

        final Ticket savedTicket = ticketCaptor.getValue();
        assertEquals(ParkingType.CAR, savedTicket.getParkingSpot().getParkingType());
        assertEquals("ABCDEF", savedTicket.getVehicleRegNumber());
        assertFalse(savedTicket.getIsRegularCustomer());
    }

    @Test
    @DisplayName("should set IsRegularCustomer to true when number of tickets is greater than 0")
    void processIncomingVehicleOfRegularCustomerTest() throws Exception {
        //given
        final String vehicleRegistrationNumber ="ABCDEF";
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(clock.millis()).thenReturn(1000L);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(ticketDAO.getNbTickets(vehicleRegistrationNumber)).thenReturn(2);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        //when
        parkingService.processIncomingVehicle();

        //then
        verify(ticketDAO,times(1)).getNbTickets(vehicleRegistrationNumber);
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(ticketDAO, times(1)).saveTicket(ticketCaptor.capture());
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();

        final Ticket savedTicket = ticketCaptor.getValue();
        assertEquals(ParkingType.CAR, savedTicket.getParkingSpot().getParkingType());
        assertEquals("ABCDEF", savedTicket.getVehicleRegNumber());
        assertTrue(savedTicket.getIsRegularCustomer());
    }

    @Test
    @DisplayName("should correctly process incoming vehicle and save ticket with correct details")
    void processIncomingVehicleTest() throws Exception {
        //given
        when(clock.millis()).thenReturn(1000L).thenReturn(360_010_000L);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(inputReaderUtil.readSelection()).thenReturn(2);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE)).thenReturn(1);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        //when
        parkingService.processIncomingVehicle();

        //then
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.BIKE);
        verify(ticketDAO, times(1)).saveTicket(ticketCaptor.capture());
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();

        final Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket);
        assertEquals(1, savedTicket.getParkingSpot().getId());
        assertEquals(ParkingType.BIKE, savedTicket.getParkingSpot().getParkingType());
        assertEquals("ABCDEF", savedTicket.getVehicleRegNumber());
        assertEquals(0.0, savedTicket.getPrice());
        assertEquals(new Date(1000L), savedTicket.getInTime());
        assertNull(savedTicket.getOutTime());
    }

    @Test
    @DisplayName("should not update parking spot when updateTicket fails")
    void processExitingVehicleTestUnableUpdate() throws Exception {
        //given
        final String vehicleRegNumber ="ABCDEF";
        final ParkingSpot parkingSpot = new ParkingSpot(1,ParkingType.CAR,false);
        final Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(new Date());
        ticket.setOutTime(null);
        ticket.setIsRegularCustomer(true);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        when(ticketDAO.getTicketWithRecentInTime(vehicleRegNumber)).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        when(clock.millis()).thenReturn(360_010_000L);
        //when
        parkingService.processExitingVehicle();

        //then
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO,times(1)).getTicketWithRecentInTime(vehicleRegNumber);
        verify(fareCalculatorService,times(1)).calculateFare(ticket,false);
        verify(ticketDAO,times(1)).updateTicket(ticket);
        verify(parkingSpotDAO,times(0)).updateParking(any(ParkingSpot.class));
    }

    @Test
    @DisplayName("should return a valid parking spot when one is available ")
    void GetNextParkingNumberIfAvailableTest () {
        //given
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        //when
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();
        //then
        assertNotNull(parkingSpot);
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertEquals(1, parkingSpot.getId());
        assertTrue(parkingSpot.isAvailable());
    }

    @Test
    @DisplayName("should return null when no parking spot is available")
    void GetNextParkingNumberIfUnavailableParkingNumberNotFoundTest (){
        //given
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        //when
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //then
        assertNull(parkingSpot);
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        verify(inputReaderUtil,times(1)).readSelection();
    }

    @Test
    @DisplayName("should return null when invalid vehicle type")
    void GetNextParkingNumberIfAvailableParkingNumberWrongArgumentTest (){
        //given
        when(inputReaderUtil.readSelection()).thenReturn(3);

        //when
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //then
        assertNull(parkingSpot);
        verify(inputReaderUtil,times(1)).readSelection();
        verify(parkingSpotDAO, times(0)).getNextAvailableSlot(any(ParkingType.class));
    }

    @Test
    @DisplayName("should correctly process exiting vehicle")
    void processExitingVehicleOfRegularClientTest() throws Exception {
        //given
        final String vehicleRegNumber ="ABCDEF";
        final ParkingSpot parkingSpot = new ParkingSpot(1,ParkingType.CAR,false);

        final Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setIsRegularCustomer(true);
        ticket.setInTime(new Date(1000L));

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        when(ticketDAO.getTicketWithRecentInTime(vehicleRegNumber)).thenReturn(ticket);
        when(ticketDAO.getNbTickets(vehicleRegNumber)).thenReturn(2);
        when(clock.millis()).thenReturn(360_010_000L);
        when(ticketDAO.updateTicket(ticket)).thenReturn(true);

        //when
        parkingService.processExitingVehicle();

        //then
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, times(1)).getTicketWithRecentInTime(vehicleRegNumber);
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(fareCalculatorService,times(1)).calculateFare(ticket,true);
        verify(ticketDAO, times(1)).updateTicket(ticketCaptor.capture());
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
        final Ticket updatedTicket = ticketCaptor.getValue();
        assertNotNull(updatedTicket);
        assertEquals(1, updatedTicket.getParkingSpot().getId());
        assertTrue(updatedTicket.getIsRegularCustomer());
        assertEquals(new Date(1000L), updatedTicket.getInTime());
        assertEquals(new Date(360_010_000L), updatedTicket.getOutTime());
    }

    @Test
    @DisplayName("should process exiting correctly when customer is not regular")
    void processExitingCustomerOfNotRegularClientTest() throws Exception {
        //given
        final String vehicleRegNumber ="ABCDEF";
        final ParkingSpot parkingSpot = new ParkingSpot(1,ParkingType.CAR,false);
        final Date inTime = new Date(1000L);
        final Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(inTime);
        ticket.setIsRegularCustomer(false);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicketWithRecentInTime(vehicleRegNumber)).thenReturn(ticket);
        when(clock.millis()).thenReturn(360_010_000L);
        when(ticketDAO.updateTicket(ticket)).thenReturn(true);

        //when
        parkingService.processExitingVehicle();

        //then
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, times(1)).getTicketWithRecentInTime(vehicleRegNumber);
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(fareCalculatorService,times(1)).calculateFare(ticket,false);
        verify(ticketDAO, times(1)).updateTicket(ticketCaptor.capture());
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
        final Ticket updatedTicket = ticketCaptor.getValue();
        assertNotNull(updatedTicket);
        assertEquals(1, updatedTicket.getParkingSpot().getId());
        assertFalse(updatedTicket.getIsRegularCustomer());
    }

    @Test
    @DisplayName("should do nothing if parkingSpot is null")
    void ShouldDoNothingIfParkingSpotIsNullTest() throws Exception {
        //given
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE)).thenReturn(0);
        when(inputReaderUtil.readSelection()).thenReturn(2);
        //when
        parkingService.processIncomingVehicle();
        //then
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.BIKE);
        verify(ticketDAO, times(0)).saveTicket(any(Ticket.class));
        verify(inputReaderUtil, times(0)).readVehicleRegistrationNumber();
    }

    @Test
    @DisplayName("should throw Illegal Argument Exception if outTime is already set")
    void ShouldThrowIllegalArgumentExceptionIfOutTimeIsAlreadySetTest() throws Exception {
        //given
        String vehicleRegNumber = "ABC123";
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(new Date(1000L));
        ticket.setOutTime(new Date(1500L));

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        when(ticketDAO.getTicketWithRecentInTime(vehicleRegNumber)).thenReturn(ticket);

        //when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parkingService.processExitingVehicle());

        assertEquals("the ticket has already an outTime", exception.getMessage());

    }
}
