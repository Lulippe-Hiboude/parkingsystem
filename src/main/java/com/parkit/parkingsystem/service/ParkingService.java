package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.Date;

public class ParkingService {
    private static final Logger logger = LogManager.getLogger("ParkingService");

    private final InputReaderUtil inputReaderUtil;
    private final ParkingSpotDAO parkingSpotDAO;
    private final TicketDAO ticketDAO;
    private final FareCalculatorService fareCalculatorService;
    private final Clock clock;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO, FareCalculatorService fareCalculatorService, Clock clock) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
        this.fareCalculatorService = fareCalculatorService;
        this.clock = clock;
    }

    public void processIncomingVehicle() {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();

            if (parkingSpot != null) {
                handleVehicleEntry(parkingSpot);
            }
        } catch (Exception e) {
            logger.error("Unable to process incoming vehicle", e);
        }
    }

    private void handleVehicleEntry(ParkingSpot parkingSpot) throws Exception {
        String vehicleRegNumber = getVehicleRegNumber();
        boolean isRegularCustomer = isRegularCustomerEntering(vehicleRegNumber);
        allocateParkingSpot(parkingSpot);
        Ticket ticket = createTicket(parkingSpot, vehicleRegNumber, isRegularCustomer);
        printTicketInfo(parkingSpot, vehicleRegNumber, ticket.getInTime());

        if (isRegularCustomer) {
            printRegularCustomerMessage();
        }
    }

    private void allocateParkingSpot(ParkingSpot parkingSpot) throws Exception {
        parkingSpot.setAvailable(false);
        parkingSpotDAO.updateParking(parkingSpot);
    }

    private boolean isRegularCustomerEntering(String vehicleRegNumber) {
        return ticketDAO.getNbTickets(vehicleRegNumber) > 0;
    }

    private Ticket createTicket(ParkingSpot parkingSpot, String vehicleRegNumber, boolean isRegularCustomer) {
        Date inTime = new Date(clock.millis());
        Ticket ticket = new Ticket();
        //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
        //ticket.setId(ticketID);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setPrice(0);
        ticket.setInTime(inTime);
        ticket.setOutTime(null);
        ticket.setIsRegularCustomer(isRegularCustomer);
        ticketDAO.saveTicket(ticket);
        return ticket;
    }

    private void printTicketInfo(ParkingSpot parkingSpot, String vehicleRegNumber, Date inTime) {
        System.out.println("Generated Ticket and saved in DB");
        System.out.println("Please park your vehicle in spot number:" + parkingSpot.getId());
        System.out.println("Recorded in-time for vehicle number:" + vehicleRegNumber + " is:" + inTime);
    }

    private void printRegularCustomerMessage() {
        String regularCustomerMessage = "Heureux de vous revoir ! " +
                "En tant qu’utilisateur régulier de notre parking, " +
                "vous allez obtenir une remise de 5%";
        System.out.println(regularCustomerMessage);
    }

    private String getVehicleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    public ParkingSpot getNextParkingNumberIfAvailable() {
        int parkingNumber = 0;
        ParkingSpot parkingSpot = null;
        try {
            ParkingType parkingType = getVehicleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if (parkingNumber > 0) {
                parkingSpot = new ParkingSpot(parkingNumber, parkingType, true);
            } else {
                throw new Exception("Error fetching parking number from DB. Parking slots might be full");
            }
        } catch (IllegalArgumentException ie) {
            logger.error("Error parsing user input for type of vehicle", ie);
        } catch (Exception e) {
            logger.error("Error fetching next available parking slot", e);
        }
        return parkingSpot;
    }

    private ParkingType getVehicleType() {
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch (input) {
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    private boolean isRegularCustomerExiting(String vehicleRegNumber) {
        return ticketDAO.getNbTickets(vehicleRegNumber) > 1;
    }

    public Ticket processExitingVehicle() throws Exception {
        Ticket ticket = null;
        try {
            String vehicleRegNumber = getVehicleRegNumber();
            ticket = ticketDAO.getTicketWithRecentInTime(vehicleRegNumber);
            updateTicketOutTime(ticket);
            boolean isRegularCustomer = isRegularCustomerExiting(vehicleRegNumber);

            fareCalculatorService.calculateFare(ticket, isRegularCustomer);

            if (ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = handleParkingSpotAvailability(ticket.getParkingSpot());
                ticket.setParkingSpot(parkingSpot);
                printExitInfo(ticket.getVehicleRegNumber(), ticket.getPrice(), ticket.getOutTime());
            } else {
                System.out.println("Unable to update ticket information. Error occurred");
            }
        } catch (Exception e) {
            logger.error("Unable to process exiting vehicle", e);
            throw e;
        }
        return ticket;
    }

    private void updateTicketOutTime(Ticket ticket) {
        if(ticket.getOutTime() != null) {
            throw new IllegalArgumentException("the ticket has already an outTime");
        }
        //ajouter une condition qui throw IllegalAr gumentException quand getOutTime est null "the ticket has already an outTime"
        Date outTime = new Date(clock.millis());
        ticket.setOutTime(outTime);
    }

    private ParkingSpot handleParkingSpotAvailability(ParkingSpot parkingSpot) {
        parkingSpot.setAvailable(true);
        parkingSpotDAO.updateParking(parkingSpot);
        System.out.println("Parking has been updated: " + parkingSpot.getId() + parkingSpot.isAvailable());
        return parkingSpot;
    }

    private void printExitInfo(String vehicleRegNumber, double ticketPrice, Date outTime) {
        System.out.println("Please pay the parking fare: " + ticketPrice);
        System.out.println("Recorded out-time for vehicle number: " + vehicleRegNumber + " is: " + outTime);
    }
}
