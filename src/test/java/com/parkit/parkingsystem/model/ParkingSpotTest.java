package com.parkit.parkingsystem.model;

import com.parkit.parkingsystem.constants.ParkingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkingSpotTest {
    private ParkingSpot parkingSpot;

    @Test
    @DisplayName("should return id")
    void getIdTest() {
        parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        assertEquals(1, parkingSpot.getId());
    }

    @Test
    @DisplayName("should return parkingType")
    void getParkingTypeTest() {
        parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
    }

    @Test
    @DisplayName("should return true when parking spot is available")
    void isAvailableTest() {
        parkingSpot = new ParkingSpot(2, ParkingType.CAR,true);
        assertTrue(parkingSpot.isAvailable());
    }

    @Test
    @DisplayName("should return false when parking spot is not available")
    void isNotAvailableTest() {
        parkingSpot = new ParkingSpot(2, ParkingType.CAR,false);
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    @DisplayName("should set available")
    void setAvailableTest() {
        parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        parkingSpot.setAvailable(false);
        assertFalse(parkingSpot.isAvailable());

        parkingSpot.setAvailable(true);
        assertTrue(parkingSpot.isAvailable());
    }
    @Test
    void testEquals() {
        parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        ParkingSpot sameSpot = new ParkingSpot(1, ParkingType.CAR, true);
        ParkingSpot differentSpot = new ParkingSpot(2, ParkingType.CAR, true);
        Object nonParkingSpot = new Object();

        assertEquals(parkingSpot, sameSpot);
        assertNotEquals(parkingSpot, differentSpot);
        assertNotEquals(parkingSpot, nonParkingSpot);
    }

    @Test
    void testHashCode() {
        parkingSpot = new ParkingSpot(1, ParkingType.CAR,true);
        ParkingSpot sameSpot = new ParkingSpot(1, ParkingType.CAR, true);
        assertEquals(parkingSpot.hashCode(), sameSpot.hashCode());
    }

}