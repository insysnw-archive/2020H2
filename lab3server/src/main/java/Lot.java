import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

public class Lot {
    private final Client owner;
    private Client highestBidder;
    private final int initialPrice;
    private int currentPrice;
    private final String lotName;
    private final String lotId;

    public Lot(Client owner, int initialPrice, String lotName) {
        this.owner = owner;
        this.initialPrice = initialPrice;
        this.currentPrice = initialPrice;
        this.lotName = lotName;
        this.lotId = UUID.randomUUID().toString();
    }

    public Lot(Lot lot) {
        this.owner = lot.getOwner();
        this.initialPrice = lot.getInitialPrice();
        this.currentPrice = lot.getCurrentPrice();
        this.lotName = lot.getLotName();
        this.lotId = lot.getLotId();
    }

    public int getInitialPrice() {
        return initialPrice;
    }

    public Client getOwner() {
        return owner;
    }

    public int getCurrentPrice() {
        return currentPrice;
    }

    public String getLotName() {
        return lotName;
    }

    public Client getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(Client highestBidder) {
        this.highestBidder = highestBidder;
    }

    public String getLotId() {
        return lotId;
    }

    public boolean setCurrentPrice(int currentPrice) {
        if(currentPrice <= this.currentPrice) {
            this.currentPrice = currentPrice;
            return true;
        } else return false;
    }

    public JSONObject toJsonObject() {
        JSONObject result = new JSONObject(Server.packets.getJSONObject("lotListReply").getJSONArray("lots").getJSONObject(0));
        result.put("lotName", lotName);
        result.put("seller", owner.getUsername());
        result.put("lotId", lotId);
        result.put("lotInitialPrice", initialPrice);
        result.put("lotCurrentPrice", currentPrice);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lot lot = (Lot) o;
        return lotId.equals(lot.lotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lotId);
    }
}
