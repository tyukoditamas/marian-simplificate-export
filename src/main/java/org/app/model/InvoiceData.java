package org.app.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceData {
    String awb;
    String date;
    String exporterName;
    String exporterAddress;
    String exporterCity;
    String exporterCountry;
    String exporterPostalCode;
    String importerName;
    String importerAddress;
    String importerCity;
    String importerCountry;
    String importerPostalCode;
    String goodsDescription;
    String value;
    String currency;
    String numberOfPackages;
    String weightGross;
    String countryItineraryCodes;

}
