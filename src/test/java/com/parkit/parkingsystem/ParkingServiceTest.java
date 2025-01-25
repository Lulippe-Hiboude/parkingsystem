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

import java.util.Date;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {
    @Mock
    private  InputReaderUtil inputReaderUtil;
    @Mock
    private  ParkingSpotDAO parkingSpotDAO;
    @Mock
    private  TicketDAO ticketDAO;
    @Mock
    private FareCalculatorService fareCalculatorService;

    @InjectMocks
    private  ParkingService parkingService;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @Test
    @DisplayName("should display regular customer message when number of tickets is greater than 0")
    public void processIncomingVehicleOfRegularCustomerTest() throws Exception {
        //given
        final String vehicleRegistrationNumber ="ABCDEF";
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegistrationNumber);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(ticketDAO.getNbTickets(vehicleRegistrationNumber)).thenReturn(2);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        //when
        parkingService.processIncomingVehicle();

        //then
        verify(ticketDAO,times(1)).getNbTickets(vehicleRegistrationNumber);
    }

    @Test
    @DisplayName("should correctly process incoming vehicle and save ticket with correct details")
    public void processIncomingVehicleTest() throws Exception {
        //given
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
        assertNotNull(savedTicket.getInTime());
        assertNull(savedTicket.getOutTime());
    }

    @Test
    @DisplayName("should not update parking spot when updateTicket fails")
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        //given
        final String vehicleRegNumber ="ABCDEF";
        final ParkingSpot parkingSpot = new ParkingSpot(1,ParkingType.CAR,false);
        final Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(new Date());
        ticket.setOutTime(null);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        when(ticketDAO.getTicket(vehicleRegNumber)).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        when(ticketDAO.getNbTickets(vehicleRegNumber)).thenReturn(2);
        //when
        parkingService.processExitingVehicle();

        //then
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO,times(1)).getTicket(vehicleRegNumber);
        verify(fareCalculatorService,times(1)).calculateFare(ticket,true);
        verify(ticketDAO,times(1)).updateTicket(ticket);
        verify(parkingSpotDAO,times(0)).updateParking(any(ParkingSpot.class));
    }

    @Test
    @DisplayName("should return a valid parking spot when one is available ")
    public void GetNextParkingNumberIfAvailableTest () {
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
    public void GetNextParkingNumberIfUnavailableParkingNumberNotFoundTest (){
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
    public void GetNextParkingNumberIfAvailableParkingNumberWrongArgumentTest (){
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
    public void processExitingVehicleOfRegularClientTest() throws Exception {
        //given
        final String vehicleRegNumber ="ABCDEF";
        final ParkingSpot parkingSpot = new ParkingSpot(1,ParkingType.CAR,false);
        final Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        final Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(inTime);
        ticket.setOutTime(new Date());
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(vehicleRegNumber)).thenReturn(ticket);
        when(ticketDAO.updateTicket(ticket)).thenReturn(true);
        when(ticketDAO.getNbTickets(vehicleRegNumber)).thenReturn(4);

        //when
        parkingService.processExitingVehicle();

        //then
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, times(1)).getTicket(vehicleRegNumber);
        verify(ticketDAO,times(1)).getNbTickets(vehicleRegNumber);
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(fareCalculatorService,times(1)).calculateFare(ticket,true);
        verify(ticketDAO, times(1)).updateTicket(ticket);
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
    }

    @Test
    @DisplayName("should process exiting correctly when customer is not regular")
    public void processExitingCustomerOfNotRegularClientTest() throws Exception {
        //given
        final String vehicleRegNumber ="ABCDEF";
        final ParkingSpot parkingSpot = new ParkingSpot(1,ParkingType.CAR,false);
        final Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        final Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(inTime);
        ticket.setOutTime(new Date());
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(vehicleRegNumber)).thenReturn(ticket);
        when(ticketDAO.updateTicket(ticket)).thenReturn(true);
        when(ticketDAO.getNbTickets(vehicleRegNumber)).thenReturn(1);

        //when
        parkingService.processExitingVehicle();

        //then
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, times(1)).getTicket(vehicleRegNumber);
        verify(ticketDAO,times(1)).getNbTickets(vehicleRegNumber);
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(fareCalculatorService,times(1)).calculateFare(ticket,false);
        verify(ticketDAO, times(1)).updateTicket(ticket);
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
    }
}
