import java.util.Scanner;

enum Data_Types
{
    // ASN.1 Data Type and their respective identifiers
    INTEGER(0x02),OCTET_STRING(0x04),NULL(0x05),OID(0x06),SEQUENCE(0x30),GETREQUEST(0xA0);
    private int identifier;
    private String data_type;

    Data_Types(int i)
    {
        identifier = i;
        data_type = this.name();
    }

    // To return identifier(in byte format) for specific ASN.1 Data Type
    public byte getIdentifer()
    {
        return (byte)identifier;
    }

    // To return ASN.1 Data Type for a given identifier
    public static String getData_Type(int i)
    {
        for(Data_Types x : Data_Types.values())
        {
            if(x.identifier == i)
                return x.data_type;
        }
        return "UNKNOWN";
    }
}

public class SNMPMessage
{
    private int Version,Request_ID,Error_Type,Error_Index;
    private String Community_String,OID,Value;

    // Initialize all fields of the SNMP Message
    public SNMPMessage()
    {
        setVersion(0);	// SNMP Version(1) : 0
        setCommunity_String("public");	// Default community string for v1 : public
        setRequest_ID(1);	// Request ID : 1
        setError_Type(0);	// Error Type : 0
        setError_Index(0);	// Error Index : 0
        setOID("");	// Object Identifier : NULL
        setValue(""); // Value : NULL
    }

    // Get Version, Community String and Object Identifier inputs from user
    public void getData()
    {
        System.out.println("Input SNMP Field Details");

        Scanner reader = new Scanner(System.in);

        System.out.print("Version : ");
        setVersion(reader.nextInt());

        reader.nextLine();

        System.out.print("Community String : ");
        setCommunity_String(reader.nextLine());

        System.out.print("OID : ");
        setOID(reader.nextLine());

        reader.close();
    }

    public int getVersion() {
        return Version;
    }

    public void setVersion(int version) {
        Version = version;
    }

    public String getCommunity_String() {
        return Community_String;
    }

    public void setCommunity_String(String community_String) {
        Community_String = community_String;
    }

    public int getRequest_ID() {
        return Request_ID;
    }

    public void setRequest_ID(int request_id) {
        Request_ID = request_id;
    }

    public int getError_Type() {
        return Error_Type;
    }

    public void setError_Type(int error_type) {
        Error_Type = error_type;
    }

    public int getError_Index() {
        return Error_Index;
    }

    public void setError_Index(int error_index) {
        Error_Index = error_index;
    }

    public String getOID() {
        return OID;
    }

    public void setOID(String oid) {
        OID = oid;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }
}