package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Date;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static FareCalculatorService fareCalculatorService;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @Mock
    private static Clock clock;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
        fareCalculatorService = new FareCalculatorService();
    }

    public void commonSetup(int vehicleType, String vehicleRegNumber) throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(vehicleType);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    @Test
    @DisplayName("should save a ticket in DB and Parking Table should be updated with availability for a car")
    void testParkingACar() throws Exception {
        //given
        String vehicleRegNumber = "ABCDEFGH";
        commonSetup(1, vehicleRegNumber);
        when(clock.millis()).thenReturn(1000L);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        //when
        parkingService.processIncomingVehicle();

        //then
        Ticket ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertNull(ticket.getOutTime());
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertFalse(parkingSpot.isAvailable());
        assertEquals(new Date(1000L), ticket.getInTime());

        //TODO: check that a ticket is actually saved in DB and Parking table is updated with availability
    }

    @Test
    @DisplayName("should save a ticket in DB and Parking Table should be updated with availability for a car")
    void testParkingABike() throws Exception {
        //given
        String vehicleRegNumber = "IJKLMNOP";
        commonSetup(2, vehicleRegNumber);
        when(clock.millis()).thenReturn(1000L);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        //when
        parkingService.processIncomingVehicle();

        //then
        Ticket ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertNull(ticket.getOutTime());
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals(ParkingType.BIKE, parkingSpot.getParkingType());
        assertFalse(parkingSpot.isAvailable());
        assertEquals(new Date(1000L), ticket.getInTime());
    }

    @Test
    @DisplayName("should generate a fare and an out Time in the Ticket table for a car")
    void testParkingLotExitACar() throws Exception {
        //given
        String vehicleRegNumber = "ABCDEFGH";
        commonSetup(1, vehicleRegNumber);
        when(clock.millis()).thenReturn(1000L, 360_010_000L);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        parkingService.processIncomingVehicle();
        Ticket ticket = parkingService.processExitingVehicle();

        //then

        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertEquals(new Date(1000L), ticket.getInTime());
        assertEquals(new Date(360_010_000L), ticket.getOutTime());
        assertTrue(ticket.getPrice() > 0.0);


        ParkingSpot parkingSpot = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber).getParkingSpot();
        assertNotNull(parkingSpot);
        Ticket ticket2 = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertEquals(ticket2.getParkingSpot().isAvailable(), parkingSpot.isAvailable());
        assertTrue(ticket.getParkingSpot().isAvailable());
        //TODO: check that the fare generated and out time are populated correctly in the database
    }

    @Test
    @DisplayName("should generate a fare and an out Time in the Ticket table for a bike")
    void testParkingLotExitABike() throws Exception {
        //given
        String vehicleRegNumber = "IJKLMNOP";
        commonSetup(2, vehicleRegNumber);
        when(clock.millis()).thenReturn(1000L, 360_010_000L);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        //when
        parkingService.processIncomingVehicle();
        Ticket ticket = parkingService.processExitingVehicle();

        //then
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertEquals(new Date(1000L), ticket.getInTime());
        assertEquals(new Date(360_010_000L), ticket.getOutTime());
        assertTrue(ticket.getPrice() > 0.0);
    }

    @Test
    @DisplayName("should generate a fare with the discount of 5% for recurring car user")
    void testParkingLotExitACarWithDiscount() throws Exception {
        String vehicleRegNumber = "ABCDEFGH";
        commonSetup(1, vehicleRegNumber);
        when(clock.millis()).thenReturn(
                720_020_000L,
                1_080_030_000L,
                1_440_040_000L,
                1_800_050_000L
        );
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        //then
        Ticket ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertEquals(new Date(1_440_040_000L), ticket.getInTime());
        assertEquals(new Date(1_800_050_000L), ticket.getOutTime());
        assertEquals( 142.5, ticket.getPrice(), 0.01,"the price is 150€ but with 5% discount is 142.5€");
    }

    @Test
    @DisplayName("should generate a fare with the discount of 5% for recurring bike user")
    void testParkingLotExitABikeWithDiscount() throws Exception {
        String vehicleRegNumber = "ABCDEFGH";
        commonSetup(2, vehicleRegNumber);
        when(clock.millis()).thenReturn(
                720_020_000L,
                1_080_030_000L,
                1_440_040_000L,
                1_800_050_000L
        );
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        //then
        Ticket ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertEquals(new Date(1_440_040_000L), ticket.getInTime());
        assertEquals(new Date(1_800_050_000L), ticket.getOutTime());
        assertEquals( 95.0, ticket.getPrice(), 0.01,"the price is 100€ but with 5% discount is 95€");
    }

    @Test
    @DisplayName("should generate a far equal to zero with duration less than 30 min for a car")
    void testParkingLotExitACarWithDurationLessThan30Min() throws Exception {
        //Given
        String vehicleRegNumber = "ABCDEFGH";
        commonSetup(1, vehicleRegNumber);
        when(clock.millis()).thenReturn(
                1_739_534_400_000L,
                1_739_535_900_000L
        );
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        //when
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        //then
        Ticket ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertEquals(new Date(1_739_534_400_000L), ticket.getInTime());
        assertEquals(new Date(1_739_535_900_000L), ticket.getOutTime());
        assertEquals(0.0, ticket.getPrice(), 0.01,"the price should be zero");
    }

    @Test
    @DisplayName("should generate a far equal to zero with duration less than 30 min for a bike")
    void testParkingLotExitABikeWithDurationLessThan30Min() throws Exception {
        //Given
        String vehicleRegNumber = "ABCDEFGH";
        commonSetup(2, vehicleRegNumber);
        when(clock.millis()).thenReturn(
                1_739_534_400_000L,
                1_739_535_900_000L
        );
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService, clock);

        //when
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        //then
        Ticket ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
        assertNotNull(ticket);
        assertEquals(vehicleRegNumber, ticket.getVehicleRegNumber());
        assertEquals(new Date(1_739_534_400_000L), ticket.getInTime());
        assertEquals(new Date(1_739_535_900_000L), ticket.getOutTime());
        assertEquals(0.0, ticket.getPrice(), 0.01,"the price should be zero");
    }
}
