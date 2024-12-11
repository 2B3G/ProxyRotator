package com.example.proxyrotator;

import java.util.List;
import java.util.stream.Collectors;

public class ProxyFilter {
    public static List<ProxyElement> filterByAddress(List<ProxyElement> proxies, String address){
        return proxies.stream()
                .filter(proxy -> proxy.getAddress().startsWith(address))
                .collect(Collectors.toList());
    }

    public static List<ProxyElement> filterByCountryName(List<ProxyElement> proxies, String countryName){
        return proxies.stream()
                .filter(proxy -> proxy.getCountryName().startsWith(countryName))
                .collect(Collectors.toList());
    }

    public static List<ProxyElement> filterByBoth(List<ProxyElement> proxies, String address, String countryName){
        return proxies.stream()
                .filter(proxy -> proxy.getAddress().startsWith(address) &&
                        proxy.getCountryName().startsWith(countryName))
                .collect(Collectors.toList());
    }
}
