
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

    private final static QName _GetProcessContentsResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "getProcessContentsResponse");
    private final static QName _ExecuteProcessCronResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessCronResponse");
    private final static QName _GetFolderContents_QNAME = new QName("http://service.web.rapidrepository.com/", "getFolderContents");
    private final static QName _ExecuteProcessSimple_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessSimple");
    private final static QName _StoreProcessResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "storeProcessResponse");
    private final static QName _GetEntryResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "getEntryResponse");
    private final static QName _GetFolderContentsResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "getFolderContentsResponse");
    private final static QName _CancelTrigger_QNAME = new QName("http://service.web.rapidrepository.com/", "cancelTrigger");
    private final static QName _DeleteEntryResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "deleteEntryResponse");
    private final static QName _CancelTriggerResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "cancelTriggerResponse");
    private final static QName _StoreProcess_QNAME = new QName("http://service.web.rapidrepository.com/", "storeProcess");
    private final static QName _CreateBlobResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "createBlobResponse");
    private final static QName _CreateBlob_QNAME = new QName("http://service.web.rapidrepository.com/", "createBlob");
    private final static QName _MakeFolderResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "makeFolderResponse");
    private final static QName _MakeFolder_QNAME = new QName("http://service.web.rapidrepository.com/", "makeFolder");
    private final static QName _GetProcessContents_QNAME = new QName("http://service.web.rapidrepository.com/", "getProcessContents");
    private final static QName _GetEntry_QNAME = new QName("http://service.web.rapidrepository.com/", "getEntry");
    private final static QName _DeleteEntry_QNAME = new QName("http://service.web.rapidrepository.com/", "deleteEntry");
    private final static QName _ExecuteProcessCron_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessCron");
    private final static QName _ExecuteProcessSimpleResponse_QNAME = new QName("http://service.web.rapidrepository.com/", "executeProcessSimpleResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rapid_i.repository.wsimport
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetProcessContents }
     * 
     */
    public GetProcessContents createGetProcessContents() {
        return new GetProcessContents();
    }

    /**
     * Create an instance of {@link GetEntry }
     * 
     */
    public GetEntry createGetEntry() {
        return new GetEntry();
    }

    /**
     * Create an instance of {@link CancelTriggerResponse }
     * 
     */
    public CancelTriggerResponse createCancelTriggerResponse() {
        return new CancelTriggerResponse();
    }

    /**
     * Create an instance of {@link MakeFolder }
     * 
     */
    public MakeFolder createMakeFolder() {
        return new MakeFolder();
    }

    /**
     * Create an instance of {@link ExecutionResponse }
     * 
     */
    public ExecutionResponse createExecutionResponse() {
        return new ExecutionResponse();
    }

    /**
     * Create an instance of {@link CreateBlobResponse }
     * 
     */
    public CreateBlobResponse createCreateBlobResponse() {
        return new CreateBlobResponse();
    }

    /**
     * Create an instance of {@link ProcessContentsResponse }
     * 
     */
    public ProcessContentsResponse createProcessContentsResponse() {
        return new ProcessContentsResponse();
    }

    /**
     * Create an instance of {@link StoreProcess }
     * 
     */
    public StoreProcess createStoreProcess() {
        return new StoreProcess();
    }

    /**
     * Create an instance of {@link ExecuteProcessCron }
     * 
     */
    public ExecuteProcessCron createExecuteProcessCron() {
        return new ExecuteProcessCron();
    }

    /**
     * Create an instance of {@link StoreProcessResponse }
     * 
     */
    public StoreProcessResponse createStoreProcessResponse() {
        return new StoreProcessResponse();
    }

    /**
     * Create an instance of {@link GetProcessContentsResponse }
     * 
     */
    public GetProcessContentsResponse createGetProcessContentsResponse() {
        return new GetProcessContentsResponse();
    }

    /**
     * Create an instance of {@link ExecuteProcessSimpleResponse }
     * 
     */
    public ExecuteProcessSimpleResponse createExecuteProcessSimpleResponse() {
        return new ExecuteProcessSimpleResponse();
    }

    /**
     * Create an instance of {@link ExecuteProcessSimple }
     * 
     */
    public ExecuteProcessSimple createExecuteProcessSimple() {
        return new ExecuteProcessSimple();
    }

    /**
     * Create an instance of {@link ExecuteProcessCronResponse }
     * 
     */
    public ExecuteProcessCronResponse createExecuteProcessCronResponse() {
        return new ExecuteProcessCronResponse();
    }

    /**
     * Create an instance of {@link CreateBlob }
     * 
     */
    public CreateBlob createCreateBlob() {
        return new CreateBlob();
    }

    /**
     * Create an instance of {@link EntryResponse }
     * 
     */
    public EntryResponse createEntryResponse() {
        return new EntryResponse();
    }

    /**
     * Create an instance of {@link GetFolderContents }
     * 
     */
    public GetFolderContents createGetFolderContents() {
        return new GetFolderContents();
    }

    /**
     * Create an instance of {@link DeleteEntryResponse }
     * 
     */
    public DeleteEntryResponse createDeleteEntryResponse() {
        return new DeleteEntryResponse();
    }

    /**
     * Create an instance of {@link DeleteEntry }
     * 
     */
    public DeleteEntry createDeleteEntry() {
        return new DeleteEntry();
    }

    /**
     * Create an instance of {@link CancelTrigger }
     * 
     */
    public CancelTrigger createCancelTrigger() {
        return new CancelTrigger();
    }

    /**
     * Create an instance of {@link MakeFolderResponse }
     * 
     */
    public MakeFolderResponse createMakeFolderResponse() {
        return new MakeFolderResponse();
    }

    /**
     * Create an instance of {@link GetFolderContentsResponse }
     * 
     */
    public GetFolderContentsResponse createGetFolderContentsResponse() {
        return new GetFolderContentsResponse();
    }

    /**
     * Create an instance of {@link Response }
     * 
     */
    public Response createResponse() {
        return new Response();
    }

    /**
     * Create an instance of {@link GetEntryResponse }
     * 
     */
    public GetEntryResponse createGetEntryResponse() {
        return new GetEntryResponse();
    }

    /**
     * Create an instance of {@link FolderContentsResponse }
     * 
     */
    public FolderContentsResponse createFolderContentsResponse() {
        return new FolderContentsResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetProcessContentsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getProcessContentsResponse")
    public JAXBElement<GetProcessContentsResponse> createGetProcessContentsResponse(GetProcessContentsResponse value) {
        return new JAXBElement<GetProcessContentsResponse>(_GetProcessContentsResponse_QNAME, GetProcessContentsResponse.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFolderContents }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getFolderContents")
    public JAXBElement<GetFolderContents> createGetFolderContents(GetFolderContents value) {
        return new JAXBElement<GetFolderContents>(_GetFolderContents_QNAME, GetFolderContents.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link StoreProcessResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "storeProcessResponse")
    public JAXBElement<StoreProcessResponse> createStoreProcessResponse(StoreProcessResponse value) {
        return new JAXBElement<StoreProcessResponse>(_StoreProcessResponse_QNAME, StoreProcessResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetEntryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getEntryResponse")
    public JAXBElement<GetEntryResponse> createGetEntryResponse(GetEntryResponse value) {
        return new JAXBElement<GetEntryResponse>(_GetEntryResponse_QNAME, GetEntryResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFolderContentsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getFolderContentsResponse")
    public JAXBElement<GetFolderContentsResponse> createGetFolderContentsResponse(GetFolderContentsResponse value) {
        return new JAXBElement<GetFolderContentsResponse>(_GetFolderContentsResponse_QNAME, GetFolderContentsResponse.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteEntryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "deleteEntryResponse")
    public JAXBElement<DeleteEntryResponse> createDeleteEntryResponse(DeleteEntryResponse value) {
        return new JAXBElement<DeleteEntryResponse>(_DeleteEntryResponse_QNAME, DeleteEntryResponse.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link StoreProcess }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "storeProcess")
    public JAXBElement<StoreProcess> createStoreProcess(StoreProcess value) {
        return new JAXBElement<StoreProcess>(_StoreProcess_QNAME, StoreProcess.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateBlobResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "createBlobResponse")
    public JAXBElement<CreateBlobResponse> createCreateBlobResponse(CreateBlobResponse value) {
        return new JAXBElement<CreateBlobResponse>(_CreateBlobResponse_QNAME, CreateBlobResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateBlob }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "createBlob")
    public JAXBElement<CreateBlob> createCreateBlob(CreateBlob value) {
        return new JAXBElement<CreateBlob>(_CreateBlob_QNAME, CreateBlob.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MakeFolderResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "makeFolderResponse")
    public JAXBElement<MakeFolderResponse> createMakeFolderResponse(MakeFolderResponse value) {
        return new JAXBElement<MakeFolderResponse>(_MakeFolderResponse_QNAME, MakeFolderResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MakeFolder }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "makeFolder")
    public JAXBElement<MakeFolder> createMakeFolder(MakeFolder value) {
        return new JAXBElement<MakeFolder>(_MakeFolder_QNAME, MakeFolder.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetProcessContents }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getProcessContents")
    public JAXBElement<GetProcessContents> createGetProcessContents(GetProcessContents value) {
        return new JAXBElement<GetProcessContents>(_GetProcessContents_QNAME, GetProcessContents.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetEntry }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "getEntry")
    public JAXBElement<GetEntry> createGetEntry(GetEntry value) {
        return new JAXBElement<GetEntry>(_GetEntry_QNAME, GetEntry.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteEntry }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidrepository.com/", name = "deleteEntry")
    public JAXBElement<DeleteEntry> createDeleteEntry(DeleteEntry value) {
        return new JAXBElement<DeleteEntry>(_DeleteEntry_QNAME, DeleteEntry.class, null, value);
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
