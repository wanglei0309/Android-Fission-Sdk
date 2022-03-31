package com.szfission.wear.demo;

import java.util.Objects;

public class BluetoothDeviceEntity {
    private String name;
    private String address;
    private Integer rssi;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BluetoothDeviceEntity that = (BluetoothDeviceEntity) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {

        return Objects.hash(address);
    }
}
