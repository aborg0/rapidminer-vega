
package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for storeProcess complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="storeProcess">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="entryLocation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="processXML" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "storeProcess", propOrder = {
    "entryLocation",
    "processXML"
})
public class StoreProcess {

    protected String entryLocation;
    protected String processXML;

    /**
     * Gets the value of the entryLocation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEntryLocation() {
        return entryLocation;
    }

    /**
     * Sets the value of the entryLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEntryLocation(String value) {
        this.entryLocation = value;
    }

    /**
     * Gets the value of the processXML property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProcessXML() {
        return processXML;
    }

    /**
     * Sets the value of the processXML property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProcessXML(String value) {
        this.processXML = value;
    }

}
