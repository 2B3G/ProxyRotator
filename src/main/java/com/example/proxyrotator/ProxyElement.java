package com.example.proxyrotator;

public class ProxyElement {
    String address;
    String countryName;

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getAddress() {
        return address;
    }

    String countryCode;

    ProxyElement(String address, String countryLong, String countryShort) {
        this.address = address;
        this.countryName = countryLong;
        this.countryCode = countryShort;
    }
}
