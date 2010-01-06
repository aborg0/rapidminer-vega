
package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for executionResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="executionResponse">
 *   &lt;complexContent>
 *     &lt;extension base="{http://service.web.rapidrepository.com/}response">
 *       &lt;sequence>
 *         &lt;element name="firstExecution" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "executionResponse", propOrder = {
    "firstExecution"
})
public class ExecutionResponse
    extends Response
{

    protected String firstExecution;

    /**
     * Gets the value of the firstExecution property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFirstExecution() {
        return firstExecution;
    }

    /**
     * Sets the value of the firstExecution property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFirstExecution(String value) {
        this.firstExecution = value;
    }

}
