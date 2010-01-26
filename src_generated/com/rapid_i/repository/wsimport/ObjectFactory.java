
package com.rapid_i.repository.wsimport;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.rapid_i.repository.wsimport package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CancelTriggerResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "cancelTriggerResponse");
    private final static QName _GetRunningProcesses_QNAME = new QName("http://service.web.rapidrepository.com/", "getRunningProcesses");
    private final static QName _GetRunningProcessesResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "getRunningProcessesResponse");
    private final static QName _ExecuteProcessCronResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessCronResponse");
    private final static QName _ExecuteProcessSimple_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessSimple");
    private final static QName _CancelTrigger_QNAME = new QName("http://service.web.rapidrepository.com/", "cancelTrigger");
    private final static QName _ExecuteProcessCron_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessCron");
    private final static QName _ExecuteProcessSimpleResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessSimpleResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rapid_i.repository.wsimport
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ExecutionResponse }
     * 
     */
    public ExecutionResponse createExecutionResponse() {
        return new ExecutionResponse();
    }

    /**
     * Create an instance of {@link ProcessStackTraceElement }
     * 
     */
    public ProcessStackTraceElement createProcessStackTraceElement() {
        return new ProcessStackTraceElement();
    }

    /**
     * Create an instance of {@link CancelTrigger }
     * 
     */
    public CancelTrigger createCancelTrigger() {
        return new CancelTrigger();
    }

    /**
     * Create an instance of {@link ExecuteProcessSimpleResponse }
     * 
     */
    public ExecuteProcessSimpleResponse createExecuteProcessSimpleResponse() {
        return new ExecuteProcessSimpleResponse();
    }

    /**
     * Create an instance of {@link ProcessResponse }
     * 
     */
    public ProcessResponse createProcessResponse() {
        return new ProcessResponse();
    }

    /**
     * Create an instance of {@link GetRunningProcesses }
     * 
     */
    public GetRunningProcesses createGetRunningProcesses() {
        return new GetRunningProcesses();
    }

    /**
     * Create an instance of {@link ExecuteProcessSimple }
     * 
     */
    public ExecuteProcessSimple createExecuteProcessSimple() {
        return new ExecuteProcessSimple();
    }

    /**
     * Create an instance of {@link CancelTriggerResponse }
     * 
     */
    public CancelTriggerResponse createCancelTriggerResponse() {
        return new CancelTriggerResponse();
    }

    /**
     * Create an instance of {@link GetRunningProcessesResponse }
     * 
     */
    public GetRunningProcessesResponse createGetRunningProcessesResponse() {
        return new GetRunningProcessesResponse();
    }

    /**
     * Create an instance of {@link ProcessStackTrace }
     * 
     */
    public ProcessStackTrace createProcessStackTrace() {
        return new ProcessStackTrace();
    }

    /**
     * Create an instance of {@link Response }
     * 
     */
    public Response createResponse() {
        return new Response();
    }

    /**
     * Create an instance of {@link ExecuteProcessCron }
     * 
     */
    public ExecuteProcessCron createExecuteProcessCron() {
        return new ExecuteProcessCron();
    }

    /**
     * Create an instance of {@link ExecuteProcessCronResponse }
     * 
     */
    public ExecuteProcessCronResponse createExecuteProcessCronResponse() {
        return new ExecuteProcessCronResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelTriggerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "cancelTriggerResponse")
    public JAXBElement<CancelTriggerResponse> createCancelTriggerResponse(CancelTriggerResponse value) {
        return new JAXBElement<CancelTriggerResponse>(_CancelTriggerResponse_QNAME, CancelTriggerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetRunningProcesses }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getRunningProcesses")
    public JAXBElement<GetRunningProcesses> createGetRunningProcesses(GetRunningProcesses value) {
        return new JAXBElement<GetRunningProcesses>(_GetRunningProcesses_QNAME, GetRunningProcesses.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetRunningProcessesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getRunningProcessesResponse")
    public JAXBElement<GetRunningProcessesResponse> createGetRunningProcessesResponse(GetRunningProcessesResponse value) {
        return new JAXBElement<GetRunningProcessesResponse>(_GetRunningProcessesResponse_QNAME, GetRunningProcessesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExecuteProcessCronResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "executeProcessCronResponse")
    public JAXBElement<ExecuteProcessCronResponse> createExecuteProcessCronResponse(ExecuteProcessCronResponse value) {
        return new JAXBElement<ExecuteProcessCronResponse>(_ExecuteProcessCronResponse_QNAME, ExecuteProcessCronResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExecuteProcessSimple }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "executeProcessSimple")
    public JAXBElement<ExecuteProcessSimple> createExecuteProcessSimple(ExecuteProcessSimple value) {
        return new JAXBElement<ExecuteProcessSimple>(_ExecuteProcessSimple_QNAME, ExecuteProcessSimple.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelTrigger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "cancelTrigger")
    public JAXBElement<CancelTrigger> createCancelTrigger(CancelTrigger value) {
        return new JAXBElement<CancelTrigger>(_CancelTrigger_QNAME, CancelTrigger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExecuteProcessCron }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "executeProcessCron")
    public JAXBElement<ExecuteProcessCron> createExecuteProcessCron(ExecuteProcessCron value) {
        return new JAXBElement<ExecuteProcessCron>(_ExecuteProcessCron_QNAME, ExecuteProcessCron.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExecuteProcessSimpleResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "executeProcessSimpleResponse")
    public JAXBElement<ExecuteProcessSimpleResponse> createExecuteProcessSimpleResponse(ExecuteProcessSimpleResponse value) {
        return new JAXBElement<ExecuteProcessSimpleResponse>(_ExecuteProcessSimpleResponse_QNAME, ExecuteProcessSimpleResponse.class, null, value);
    }

}
