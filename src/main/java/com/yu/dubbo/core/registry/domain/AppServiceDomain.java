package com.yu.dubbo.core.registry.domain;

import java.util.List;

public class AppServiceDomain {

    private List<Provider> providerList;

    private List<Customer> customerList;

    public List<Provider> getProviderList() {
        return providerList;
    }

    public void setProviderList(List<Provider> providerList) {
        this.providerList = providerList;
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList = customerList;
    }

    public static class Provider {

        private String serviceName;

        private List<String> addressList;

        public Provider(String serviceName, List<String> addressList) {
            this.serviceName = serviceName;
            this.addressList = addressList;
        }

        public Provider() {
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public List<String> getAddressList() {
            return addressList;
        }

        public void setAddressList(List<String> addressList) {
            this.addressList = addressList;
        }

    }

    public static class Customer {

        private String serviceName;

        private Boolean isOnline;

        private List<CustomerItem> customerItems;

        public Customer() {
        }

        public Customer(String serviceName, Boolean isOnline, List<CustomerItem> customerItems) {
            this.serviceName = serviceName;
            this.customerItems = customerItems;
            this.isOnline = isOnline;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public List<CustomerItem> getCustomerItems() {
            return customerItems;
        }

        public void setCustomerItems(List<CustomerItem> customerItems) {
            this.customerItems = customerItems;
        }

        public Boolean getIsOnline() {
            return isOnline;
        }

        public void setIsOnline(Boolean isOnline) {
            this.isOnline = isOnline;
        }

    }

    public static class CustomerItem {

        private String address;

        private String connectType;

        private String appName;

        public CustomerItem(String address, String connectType, String appName) {
            this.address = address;
            this.connectType = connectType;
            this.appName = appName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getConnectType() {
            return connectType;
        }

        public void setConnectType(String connectType) {
            this.connectType = connectType;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

    }

}
