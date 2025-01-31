package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculatorService {
    private static final double DISCOUNT_RATE = 0.05;

    public void calculateFare(final Ticket ticket) {
        calculateFare(ticket,false);
    }

    public void calculateFare(final Ticket ticket, final boolean discount) {
        validateOutTime(ticket);
        //TODO: Some tests are failing here. Need to check if this logic is correct
        double durationInMinutes = calculateTicketDurationInMinutes(ticket);

        if(isFreeParking(durationInMinutes)) {
            ticket.setPrice(0);
            return;
        }

        double durationInHours = convertMinutesToHours(durationInMinutes);
        double ratePerHour = getRatePerHour(ticket);
        double price = durationInHours * ratePerHour;

        if(discount){
        price *= (1-DISCOUNT_RATE);
         }
        BigDecimal roundedPrice = new BigDecimal(String.valueOf(price)).setScale(2, RoundingMode.HALF_UP);
        ticket.setPrice(roundedPrice.doubleValue());
    }

    private void validateOutTime(final Ticket ticket) {
        if ((ticket.getOutTime() == null)) {
            throw new IllegalArgumentException("Out time provided is null");
        }

        if (ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString() + "compared To In time : " + ticket.getInTime().toString());
        }
    }

    private void validateParkingType(final Ticket ticket) {
        if (ticket.getParkingSpot().getParkingType() == null) {
            throw new IllegalArgumentException("Parking Type is null");
        }
    }

    private double calculateTicketDurationInMinutes(final Ticket ticket) {
        double inTimeInMillis = ticket.getInTime().getTime();
        double outTimeInMillis = ticket.getOutTime().getTime();
        double durationInMillis = outTimeInMillis - inTimeInMillis;
        return durationInMillis / 60000.0;
    }

    private double convertMinutesToHours(final double minutes) {
        return minutes / 60.0;
    }

    private double getRatePerHour(final Ticket ticket) {
        validateParkingType(ticket);
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                return Fare.CAR_RATE_PER_HOUR;
            }
            case BIKE: {
                return Fare.BIKE_RATE_PER_HOUR;
            }
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }

    public boolean isFreeParking (final double durationInMinutes) {
        return durationInMinutes < 30.0;
    }
}