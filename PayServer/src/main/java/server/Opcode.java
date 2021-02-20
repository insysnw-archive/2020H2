package server;

public enum Opcode {
    ERROR(0),
    RESPONSE(1),
    SIGNIN(2),
    SIGNUP(3),
    WALLETRQST(4),
    WALLETLIST(5),
    SEND(6),
    SENDSTATUS(7),
    RECEIVESTATUS(8),
    SENDREQUEST(9),
    REQUESTRESPONSE(10),
    REQUESTLIST(11),
    CHECKREQUESTS(12),
    CHECKWALLET(13),
    ANSWERWALLET(14),
    DISCONNECT(15);

    public int getOpcode() {
        return opcode;
    }
    public int getOpcodeEnum(int opcode) {return this.opcode;}

    private final int opcode;

    Opcode(int opcode) {
        this.opcode = opcode;
    }
}